(ns silk.core.input.file
  "File input functions including template and component."
  (:require [clojure.java.io :refer [file]]
            [clojure.edn :as edn]
            [hickory.core :as h]
            [me.rossputin.diskops :as do]
            [silk.core.input.env :as se])
  (import java.io.File))

;; =============================================================================
;; Helper functions
;; =============================================================================

(defn- system-root-resource [rel-path system-root]
  (let [sep (if (re-find #"indow" se/os) "\\\\" (do/fs))
        path (str system-root sep (.replaceAll rel-path "/" sep))]
    (file path)))

(defn- file-2-map
  [f]
  {:sw/last-modified (.lastModified f)
   :sw/name (.getName f)
   :sw/path (.getPath f)})

(defn- edn-file?
  [f]
  (.endsWith (.toLowerCase (.getName f)) ".edn"))

(defn- edn-files
  [files dirs?]
  (filter #(if dirs? (or (.isDirectory %) (edn-file? %)) (edn-file? %)) files))

(defn- get-data-meta
  "Get directory metadata under the 'data' directory given a directory d.
   Useful in cases where we do not intend to do anything with file contents."
  [res]
  (map #(file-2-map %) (edn-files (file-seq res) false)))

(defn- file-tree [#^File f]
 (if (.isDirectory f)
   (merge (file-2-map f) {:sw/contents (vec (map file-tree (edn-files (.listFiles f) true)))})
   (merge (file-2-map f) (edn/read-string (slurp f)))))

;; =============================================================================
;; File based input, see namespace comment
;; =============================================================================

(defn template
  "Return a Silk template from the silk template directory given a filename f.
   A Silk template is raw markup."
  [f]
  (file (str (se/project-templates-path) f)))

(defn quantum-resource
  "Return a Silk resource which may be in one of several places given a path.
   Initially component or datasource.  Both return a File.
   Typically used to source artifacts from either a local silk project directory,
   an env var root or silk home."
  [rel-path local-root system-root]
  (let [item-path (str local-root (do/fs) rel-path)
        local-res-path (str se/current-project (do/fs) item-path)
        local-file (file local-res-path)
        reserve (system-root-resource rel-path system-root)
        home (file (str se/silk-home (do/fs) item-path))
        sw (file (str se/silk-home (do/fs) rel-path))]
    (cond
      (.exists local-file) local-file
      (.exists reserve) reserve
      (.exists home) home
      (.exists sw) sw
      :else (throw (Exception. (str "Can't find " item-path))))))

(defn component [path]
  (quantum-resource (str path ".html") "components" se/components-path))

(defn get-views []
  (-> (remove #(.isDirectory %) (file-seq (file (se/views-path))))
      (do/filter-exts ["html"])))

(defn get-data-directories
  "Get each of the directories which contain files to process as either:
     detail files
     binary assets to be listed in an index page.
   Assume for now we will only generate detail pages from local data.  Unsure how
   to resolve shared data... it must not overwrite local etc."
  []
  (->> (get-data-meta (file (se/project-data-path)))
       (map #(file (:sw/path %)))
       (filter #(.isFile %))
       (map #(.getParent %))
       distinct))

(defn slurp-data
  [path]
  (let [qr (quantum-resource path "data" se/data-path)]
    (if (.isDirectory qr)
      (:sw/contents (file-tree qr))
      (file-tree qr))))

(defn hick-file
 "Converts a HMTL file into hickory"
 [f]
 (try
   (h/as-hickory (h/parse (slurp f)))
   (catch Exception e
     (throw (Exception. (str (.getMessage e) " in file " (.getName f)) e)))))
