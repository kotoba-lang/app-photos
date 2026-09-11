(ns app-photos.model
  "Photos' domain: images with the metadata that came out of the camera.

  Two capabilities. `fs/browse` finds the files; `image/metadata` opens them
  far enough to read EXIF. They are separate because a picker that only needs
  to list a folder should not also be able to read where every photo was
  taken."
  (:require [mokuroku.item :as item]
            [mokuroku.source :as source]))

(def metadata-capability "image/metadata")
(def browse-capability "fs/browse")

(def columns
  [(source/attribute :name "Name" :string)
   (source/attribute :captured "Date" :number)
   (source/attribute :dimensions "Dimensions" :string)
   (source/attribute :camera "Camera" :string)
   (source/attribute :iso "ISO" :number)
   (source/attribute :size "Size" :bytes)
   (source/attribute :path "Path" :string false)])

(def commands
  #{:open :quicklook :copy-path :export})

(defn descriptor
  ([] (descriptor "Library"))
  ([album]
   (source/descriptor
    {:id :app-photos/album
     :item-kind :image
     :label album
     :capability metadata-capability
     :commands commands
     :attributes columns})))

(def rotated?
  "EXIF orientations 5-8 swap width and height.

  A viewer that ignores this reports a portrait photo as landscape, and every
  aspect-ratio-driven layout puts it in the wrong slot."
  #{5 6 7 8})

(defn displayed-dimensions
  "Width and height as the image will actually appear, after orientation."
  [{:keys [width height orientation]}]
  (when (and (number? width) (number? height))
    (if (rotated? orientation)
      [height width]
      [width height])))

(defn entry->item
  "Normalise one provider row.

  `:captured` prefers the EXIF capture time and falls back to the file's
  modification time, and `:time-source` records which was used. Silently
  mixing the two produces a timeline where copied files jump to today —
  the single most common way a photo library sorts wrongly."
  [{:keys [path name captured modified size camera lens iso] :as entry}]
  (let [[w h] (displayed-dimensions entry)
        exif? (number? captured)]
    (item/item path
               :image
               (or name path)
               (cond-> {:name (or name path)
                        :path path
                        :size size
                        :captured (if exif? captured modified)
                        :time-source (if exif? :exif :filesystem)
                        :camera camera
                        :lens lens
                        :iso iso}
                 (and w h) (assoc :dimensions (str w " × " h)
                                  :width w
                                  :height h
                                  :megapixels (/ (Math/round (/ (* w h) 100000.0)) 10.0))))))

(defn listing->items [entries]
  (mapv entry->item entries))

(def newest-first
  "The default: most recent at the top, which is what a camera roll is."
  [[:captured :desc]])

(def default-query
  {:query/sort newest-first :query/text "" :query/filters []})

(defn undated
  "Items whose date came from the filesystem rather than the camera.

  Surfaced rather than hidden: a library that silently presents file times as
  capture times is telling the user something false about their own history."
  [items]
  (vec (filter #(= :filesystem (item/attr % :time-source)) items)))

(def located?
  "Whether an item carries a location. `image/metadata` providers may or may
  not return GPS; the app never infers one."
  (fn [it] (some? (item/attr it :location))))
