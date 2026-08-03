(ns app-photos.source
  "The `image/metadata` seam. Nothing here opens a file."
  (:require [app-photos.model :as model]
            [mokuroku.source :as source]))

(defrecord AlbumSource [album read-fn]
  source/ISource
  (-descriptor [_] (model/descriptor album))
  (-fetch [_] (model/listing->items (read-fn album))))

(defn album-source
  "READ-FN takes an album/folder identifier and returns entry maps carrying
  whatever EXIF the provider could read. It is where the grant is spent."
  [album read-fn]
  (->AlbumSource album read-fn))

(defn fixture-source [album entries]
  (album-source album (constantly entries)))

(def denied
  {:photos/state :denied
   :photos/capability model/metadata-capability
   :photos/entries []})

(defn granted [entries]
  {:photos/state :granted
   :photos/capability model/metadata-capability
   :photos/entries (vec entries)})

(defn denied? [r] (= :denied (:photos/state r)))

(def metadata-only-denied
  "The half-granted case: fs/browse succeeded, image/metadata did not.

  The files are listed but nothing is known about them, which is a genuinely
  different state from both a refused listing and a working one -- and the
  only one where sorting by date is meaningless."
  {:photos/state :metadata-denied
   :photos/capability model/metadata-capability
   :photos/browse-capability model/browse-capability})

(defn metadata-denied? [r] (= :metadata-denied (:photos/state r)))
