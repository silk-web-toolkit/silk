(ns silk.input.file
  "File input functions including template and component."
  (:require [clojure.java.io :refer [file]]
            [silk.input.env :as se]))

(def rrest (comp rest rest))

(defn- file-2-map
  [f]
  {:last-modified (.lastModified f) :name (.getName f) :path (.getPath f)})

(defn runtime-template
  "Return a runtime Silk template from the runtime silk template directory given a
   filename f.
   A runtime Silk template is the result of a Silk spin process."
  [f]
  (file (str se/runtime-templates-path f)))

(defn template
  "Return a Silk template template from the silk template directory given a filename f.
   A Silk template is raw markup."
  [f]
  (file (str se/templates-path f)))

(defn component
  "Return a Silk component from the silk components directory given a filename f.
  A Silk component is raw markup."
  [f]
  (let [os (System/getProperty "os.name")
        sep (if (re-find #"indow" os) "\\\\" se/fs)
        lv (str se/components-path sep (.replaceAll f "/" sep))]
  (file lv)))

(defn get-views [] (remove #(.isDirectory %) (file-seq (file se/views-path))))

(defn get-data-meta
  "Get directory metadata under the 'data' directory given a directory d.
   Useful in cases where we do not intend to do anything with file contents."
  [d]
  (map #(file-2-map %) (rrest (file-seq (file (str se/data-path))))))
