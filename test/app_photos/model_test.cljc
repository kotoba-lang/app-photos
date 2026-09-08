(ns app-photos.model-test
  (:require [app-photos.model :as model]
            [app-photos.page :as page]
            [app-photos.source :as source]
            [kotoba.lang.text :as str]
            [clojure.test :refer [deftest is testing]]
            [design-quality.audit :as dq]
            [mokuroku.catalog :as catalog]
            [mokuroku.item :as item]))

(def entries
  [{:path "/p/a.jpg" :name "a.jpg" :captured 1700 :modified 9999 :size 4194304
    :width 6000 :height 4000 :orientation 1 :camera "X100V" :iso 400}
   {:path "/p/b.jpg" :name "b.jpg" :captured 1900 :modified 9999 :size 3145728
    :width 6000 :height 4000 :orientation 6 :camera "X100V" :iso 200}
   ;; copied off a card: no EXIF, so only a file time
   {:path "/p/c.jpg" :name "c.jpg" :modified 5000 :size 2097152
    :width 1024 :height 768 :orientation 1}])

(defn- cat-of [es]
  (catalog/refresh (catalog/catalog (source/fixture-source "Library" es)
                                    model/default-query)))

(deftest orientation-swaps-the-reported-dimensions
  ;; Ignoring this reports a portrait photo as landscape, and every
  ;; aspect-ratio-driven layout puts it in the wrong slot.
  (testing "orientations 5-8 rotate"
    (is (= [4000 6000] (model/displayed-dimensions
                        {:width 6000 :height 4000 :orientation 6})))
    (is (= [6000 4000] (model/displayed-dimensions
                        {:width 6000 :height 4000 :orientation 1}))))

  (testing "the rendered dimension string follows"
    (let [by-id (into {} (map (juxt :item/id identity)) (model/listing->items entries))]
      (is (= "6000 × 4000" (item/attr (by-id "/p/a.jpg") :dimensions)))
      (is (= "4000 × 6000" (item/attr (by-id "/p/b.jpg") :dimensions)))))

  (testing "an entry with no dimensions gets none rather than zeros"
    (is (nil? (model/displayed-dimensions {:width nil :height nil})))
    (is (nil? (item/attr (model/entry->item {:path "/p/x" :name "x"}) :dimensions)))))

(deftest capture-time-falls-back-but-says-so
  ;; Silently mixing EXIF and filesystem times produces a timeline where
  ;; copied files jump to today — the most common way a photo library sorts
  ;; wrongly.
  (let [by-id (into {} (map (juxt :item/id identity)) (model/listing->items entries))]
    (is (= 1700 (item/attr (by-id "/p/a.jpg") :captured)))
    (is (= :exif (item/attr (by-id "/p/a.jpg") :time-source)))
    (is (= 5000 (item/attr (by-id "/p/c.jpg") :captured)))
    (is (= :filesystem (item/attr (by-id "/p/c.jpg") :time-source))
        "and the app can mark it rather than pass it off as a capture date"))

  (testing "undated photos are surfaced, not hidden"
    (is (= ["/p/c.jpg"] (mapv :item/id (model/undated (model/listing->items entries)))))))

(deftest newest-first-is-the-default
  (is (= ["/p/c.jpg" "/p/b.jpg" "/p/a.jpg"]
         (mapv :item/id (:result/items (catalog/result (cat-of entries)))))
      "c has a file time of 5000, which really is the most recent thing known"))

(deftest the-half-granted-state-is-its-own-state
  ;; fs/browse succeeded, image/metadata did not: the files are listed but
  ;; nothing is known about them, and sorting by date is meaningless.
  (is (source/metadata-denied? source/metadata-only-denied))
  (is (not (source/denied? source/metadata-only-denied)))
  (is (not (source/metadata-denied? (source/granted entries))))
  (is (= "image/metadata" (:photos/capability source/denied)))
  (is (= "fs/browse" (:photos/browse-capability source/metadata-only-denied))))

(deftest photos-cannot-be-deleted-from-here
  (let [c (catalog/select (cat-of entries) "/p/a.jpg")]
    (is (= #{:open :quicklook :copy-path :export}
           (set (map :command/id (:view/commands (catalog/view c))))))
    (is (= :source-does-not-accept (:proposal/refused (catalog/propose c :trash)))
        "no provider implements deletion, so it is not offered")))

(deftest a-badge-marks-the-rows-that-are-guessing
  (let [html (page/render-html (cat-of entries))]
    (is (str/includes? html "File date"))))

(deftest window-meets-the-design-quality-floor
  (let [pages {"album" (page/render (cat-of entries))
               "selection" (page/render (catalog/select-all (cat-of entries)))
               "awaiting-grant" (page/render
                                 (catalog/catalog (source/fixture-source "Library" [])
                                                  model/default-query))}
        {:keys [overall pages] :as report} (dq/audit pages {:extra-axes dq/extra-axes})]
    (println "design-quality: aggregate" overall)
    (doseq [[nm r] (sort-by key pages)] (println " " nm (:overall r)))
    (is (>= overall 98.0) (pr-str (:findings report)))
    (doseq [[nm r] pages] (is (>= (:overall r) 98.0) nm))))
