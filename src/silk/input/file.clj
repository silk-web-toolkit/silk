(ns silk.input.file
  "File input functions including template and component."
  (:require [clojure.java.io :refer [file]]
            [silk.input.env :as se]))

;; =============================================================================
;; Helper functions
;; =============================================================================

(defn system-root-resource
  [rel-path system-root]
  (let [sep (if (re-find #"indow" se/os) "\\\\" se/fs)
        path (str system-root sep (.replaceAll rel-path "/" sep))]
    (file path)))


(defn- file-2-map
  [f]
  {:last-modified (.lastModified f)
   :name (.getName f)
   :path (.getPath f)
   :type (if (.isDirectory f) "directory" "file")
   :is-directory (.isDirectory f)
   :is-file (.isFile f)
   :is-hidden (.isHidden f)})


;; =============================================================================
;; File based input, see namespace comment
;; =============================================================================

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

(defn quantum-resource
  "Return a Silk resource which may be in one of two places given a path.
   Initially component or datasource.  Both return a File.
   Typically used to source artifacts from either a local silk project directory,
   or an env var root."
  [rel-path local-root system-root]
  (let [local-res-path (str se/pwd se/fs local-root se/fs rel-path)
        local-file (file local-res-path)]
    (if (.exists local-file) local-file (system-root-resource rel-path system-root))))

(defn get-views [] (remove #(.isDirectory %) (file-seq (file se/views-path))))

(defn get-data-meta
  "Get directory metadata under the 'data' directory given a directory d.
   Useful in cases where we do not intend to do anything with file contents."
  [res]
  (let [raw-file
        (if (nil? res) '() (file-seq res))
        artifact (if (> (count raw-file) 1) (rest raw-file) raw-file)]
    (map #(file-2-map %) artifact)))

(defn get-data-directories
  "Get each of the directories which contain files to process as either:
     detail files
     binary assets to be listed in an index page.
   Assume for now we will only generate detail pages from local data.  Unsure how
   to resolve shared data... it must not overwrite local etc."
  []
  (->> (get-data-meta (file se/pwd "data"))
     (map #(file (:path %)))
     (filter #(.isFile %))
     (map #(.getParent %))
     distinct))

(defn store-project-dir
  "Writes the current project directory to the central store."
  []
  (let [file se/spun-projects-file pwd se/pwd]
    (if (not (.exists file)) (.createNewFile file))
    (if (not (.contains (slurp (.getPath file)) pwd))
      (spit (.getPath file) (str pwd "\n") :append true))))
