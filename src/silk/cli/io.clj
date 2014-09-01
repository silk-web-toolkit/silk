(ns silk.cli.io
  (:require [silk.core.input.env :as se]
            [silk.core.input.file :as sf]
            [silk.core.transform.pipeline :as pipes]
            [silk.core.transform.path :as sp]
            [clojure.java.io :refer [file]]
            [clojure.data.json :as json]
            [io.aviso.ansi :as aa]
            [io.aviso.exception :as ae]
            [me.rossputin.diskops :as do])
  (import java.io.File))

;; =============================================================================
;; Helper functions
;; =============================================================================

(defmacro get-version []
  (System/getProperty "silk.version"))

(defn- filter-file
  [r]
  (reify java.io.FilenameFilter
    (accept [_ d name] (not (nil? (re-find r name))))))

(defn- is-detail?
  [d r]
  (let [files (.list (file d) (filter-file r))]
    (if (seq files) true false)))

; TODO: TBD
(defn- do-index-pages
  [d]
  nil)


;; =============================================================================
;; Ugly side effecting IO
;; =============================================================================

(defn cli-app-banner-display
  []
  (println      "    _ _ _")
  (println      " __(_) | |__")
  (println      "(_-< | | / /")
  (println (str "/__/_|_|_\\_\\ " "v" (get-version)))
  (println ""))

(defn display-spin-start
  []
  (println "Spinning your site..."))

(defn display-spin-end
  []
  (println (str (aa/bold-green "SUCCESS: ") (aa/italic "Site spinning is complete, we hope you like it."))))

(defn display-files-changed
  [files]
  (println "Files changed in " (do/pwd))
  (doseq [file files] (println (sp/relativise-> (do/pwd) (.getPath file)))))

(defn side-effecting-spin-io
  []
  (when (do/exists-dir? "site") (do/delete-directory "site"))
  (when (do/exists-dir? se/sw-path) (do/delete-directory se/sw-path))
  (.mkdir (File. "site"))
  (.mkdir (File. se/sw-path))
  (when (do/exists-dir? "resource") (do/copy-recursive "resource" "site"))
  (when (do/exists-dir? "meta") (do/copy-file-children "meta" "site")))

(defn is-silk-project?
  []
  (and
   (do/exists-dir? "view") (do/exists-dir? "template")))

;; last spun time and silk projects both live in silk home
(defn is-silk-configured?
  []
  (do/exists-dir? se/silk-home))

(defn check-silk-configuration
  []
  (if (not (is-silk-configured?))
    (do
      (throw (IllegalArgumentException. "Silk is not configured, please ensure your SILK_PATH is setup and contains a components and data directory.")))))

(defn check-silk-project-structure
  []
  (if (not (is-silk-project?))
    (do
      (throw (IllegalArgumentException. "Not a Silk project, a directory may be missing - template or view ?")))))

(defn handler
  [f & handlers]
  (reduce (fn [handled h] (partial h handled)) f (reverse handlers)))

(defn handle-silk-project-exception
  [f & args]
  (try
    (apply f args)
    (catch IllegalArgumentException iex
      (println (str (aa/bold-red "ERROR: ") (aa/italic "Sorry, either Silk is not configured properly or there is a problem with this Silk project.")))
      (println (str (aa/bold-red "CAUSE: ") (aa/italic (.getMessage iex)))))
    (catch Exception ex
      (println (str (aa/bold-red "ERROR: ") (aa/italic "Sorry, there was a problem, either a component or datasource is missing or this is not a Silk project ?")))
      (println (str (aa/bold-red "CAUSE: ") (aa/italic (.getMessage ex)))))))

(defn trace-silk-project-exception
  [f & args]
  (try (apply f args) (catch Exception iex (ae/write-exception iex))))

(defn create-view-driven-pages
  [vdp]
  (doseq [t vdp]
    (let [parent (.getParent (new File (:path t)))]
      (when-not (nil? parent) (.mkdirs (File. "site" parent)))
      (spit (str se/site-path (:path t)) (:content t)))))

(defn get-data-driven-pipeline
  [mode]
  (flatten
    (for [path (sf/get-data-directories)]
      (if (is-detail? path #".edn")
        (let [f (file path)
              tpl (file (str se/templates-details-path (.getName f) ".html"))]
          (if (.exists (file tpl))
            (pipes/data-detail-pipeline-> (.listFiles f) tpl mode)
            nil))
        (do-index-pages path)))))

(defn create-data-driven-pages
  [ddp]
  (doseq [d ddp]
    (let [parent (.getParent (new File (:path d)))
          raw (str se/site-path (:path d))
          save-path (str (subs raw 0 (.lastIndexOf raw ".")) ".html")]
      (when-not (nil? parent) (.mkdirs (File. "site" parent)))
      (spit save-path (:content d)))))

(defn create-tipue-search-content-file
  [tp]
  (let [path (str se/site-path "resource" (do/fs) "js" (do/fs))
        file (str path "tipuesearch_content.js")]
    (.mkdirs (File. path))
    (spit file (str
      "var tipuesearch=" (json/write-str tp) ";var tipuedrop=tipuesearch;"))))

(defn store-project-dir
  "Writes the current project path and time to the central store."
  []
  (let [f se/spun-projects-file]
    (if (not (.exists f)) (.createNewFile f))
    (let [path (.getPath f)
          millis (.getTime (new java.util.Date))
          old (with-open [rdr (clojure.java.io/reader path)] (doall (line-seq rdr)))
          removed (remove #(.contains % (str (do/pwd) ",")) old)
          formatted (apply str (map #(str % "\n") removed))
          updated (conj [(str (do/pwd) "," millis "\n")]  formatted)]
      (spit path (apply str updated)))))
