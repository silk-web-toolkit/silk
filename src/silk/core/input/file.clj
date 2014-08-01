(ns silk.core.input.file
  "File input functions including template and component."
  (:require [clojure.java.io :refer [file]]
            [me.rossputin.diskops :as do]
            [silk.core.input.env :as se])
  (import java.io.File))

;; =============================================================================
;; Helper functions
;; =============================================================================

(defn system-root-resource [rel-path system-root]
  (let [sep (if (re-find #"indow" se/os) "\\\\" (do/fs))
        path (str system-root sep (.replaceAll rel-path "/" sep))]
    (file path)))

(defn- file-2-map [f]
  {:silk/last-modified (.lastModified f)
   :silk/name (.getName f)
   :silk/path (.getPath f)
   :silk/type (if (.isDirectory f) "directory" "file")
   :silk/is-directory (.isDirectory f)
   :silk/is-file (.isFile f)
   :silk/is-hidden (.isHidden f)})

(defstruct node-st :name :path :content :node-type)

(defn file-tree [#^File f]
  (if (.isDirectory f)
    (struct node-st (.getName f) (.getPath f) (vec (map file-tree (.listFiles f))) :directory)
    (struct node-st (.getName f) (.getPath f) [(.getName f)] :file)))

(defn get-views-raw [] (remove #(.isDirectory %) (file-seq (file se/views-path))))


;; =============================================================================
;; File based input, see namespace comment
;; =============================================================================

(defn template
  "Return a Silk template template from the silk template directory given a filename f.
   A Silk template is raw markup."
  [f]
  (file (str se/templates-path f)))

(defn quantum-resource
  "Return a Silk resource which may be in one of several places given a path.
   Initially component or datasource.  Both return a File.
   Typically used to source artifacts from either a local silk project directory,
   an env var root or silk home."
  [rel-path local-root system-root]
  (let [item-path (str local-root (do/fs) rel-path)
        local-res-path (str (do/pwd) (do/fs) item-path)
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

(defn data [path] (quantum-resource path "data" se/data-path))

(defn get-views []
  (-> (get-views-raw)
      (do/filter-exts ["html"])))

(defn get-data-meta
  "Get directory metadata under the 'data' directory given a directory d.
   Useful in cases where we do not intend to do anything with file contents."
  [res]
  (let [raw-file
        (if (nil? res) '() (file-seq res))
        artifact (if (> (count raw-file) 1) (rest raw-file) raw-file)]
    (map #(file-2-map %) artifact)))

(defn get-data-meta-tree
  "Same as get-data-meta but work with hierarchy."
  [res]
  (if (nil? res) {} (file-tree res)))

(defn get-data-directories
  "Get each of the directories which contain files to process as either:
     detail files
     binary assets to be listed in an index page.
   Assume for now we will only generate detail pages from local data.  Unsure how
   to resolve shared data... it must not overwrite local etc."
  []
  (->> (get-data-meta (file (do/pwd) "data"))
     (map #(file (:silk/path %)))
     (filter #(.isFile %))
     (map #(.getParent %))
     distinct))
