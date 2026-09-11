(ns app-photos.page
  (:require [mokuroku.catalog :as catalog]
            [mokuroku-ui.core :as mui]))

(def view-opts
  {:columns [:name :captured :dimensions]
   :formatters {:size mui/human-bytes
                :iso #(when % (str "ISO " %))}
   :noun "photos"
   :search-placeholder "Search photos"
   :empty-title "No photos"
   :empty-body "Nothing in this album matches the current filter."
   ;; The badge is the honesty mechanism: a row dated from the filesystem
   ;; rather than the camera says so, instead of sitting in the timeline
   ;; pretending to be a capture date.
   :badge (fn [it]
            (when (= :filesystem (:time-source (:item/attrs it)))
              "File date"))
   :title "Photos"
   :description "An album, newest first."})

(defn render [cat] (mui/->page (catalog/view cat) view-opts))
(defn render-html [cat] (mui/->html (catalog/view cat) view-opts))
