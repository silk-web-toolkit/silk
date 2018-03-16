(ns silk.cli.io
  (:require [silk.core.input.env :as se]
            [silk.core.input.file :as sf]
            [silk.core.transform.pipeline :as pipes]
            [clojure.java.io :as io]
            [clojure.data.json :as json]
            [io.aviso.ansi :as aa]
            [io.aviso.exception :as ae]
            [me.rossputin.diskops :as do]))

;; =============================================================================
;; Helper functions
;; =============================================================================

(defn- filter-file [r]
  (reify java.io.FilenameFilter
    (accept [_ d name] (not (nil? (re-find r name))))))

(defn- is-detail? [d r]
  (let [files (.list (io/file d) (filter-file r))]
    (if (seq files) true false)))

; TODO: TBD
(defn- do-index-pages [d] nil)


;; =============================================================================
;; Ugly side effecting IO
;; =============================================================================

(defn display-spin-start [] (println "Spinning your site..."))

(defn display-spin-end []
  (println
    (str (aa/bold-green "SUCCESS: ")
         (aa/italic "Site spinning is complete, we hope you like it."))))

(defn side-effecting-spin-io
  [project]
  (let [s (se/site-path project)
        r (se/resource-path project)
        m (se/meta-path project)]
    (when (do/exists-dir? s) (do/delete-directory s))
    (when (do/exists-dir? se/sw-path) (do/delete-directory se/sw-path))
    (.mkdir (io/file s))
    (.mkdir (io/file se/sw-path))
    (when (do/exists-dir? r) (do/copy-recursive r s))
    (when (do/exists-dir? m) (do/copy-file-children m s))))

(defn silk-project?
  [project]
  (and (do/exists-dir? (se/views-path project))
       (do/exists-dir? (se/project-templates-path project))))

;; last spun time and silk projects both live in silk home
(defn silk-configured? [] (do/exists-dir? se/silk-home))

(defn check-silk-configuration []
  (if (not (silk-configured?))
    (do
      (println
        (aa/bold-red "WARNING: Creating missing shared directory"))
        (.mkdirs (io/file se/silk-home)))))

(defn check-silk-project-structure
  [project]
  (if (not (silk-project? project))
    (do
      (throw (IllegalArgumentException. "Not a Silk project, a directory may be missing - template or view ?")))))

(defn handle-exception [f & args]
  (try
    (apply f args)
    (catch IllegalArgumentException iex
      (println (str (aa/bold-red "ERROR: ") (aa/italic "Sorry, either Silk is not configured properly or there is a problem with this Silk project.")))
      (println (str (aa/bold-red "CAUSE: ") (aa/italic (.getMessage iex)))))
    (catch Exception ex
      (println (str (aa/bold-red "ERROR: ") (aa/italic "Sorry, there was a problem, either a component or datasource is missing or this is not a Silk project ?")))
      (println (str (aa/bold-red "CAUSE: ") (aa/italic (.getMessage ex)))))))

(defn trace-exception [f & args]
  (try (apply f args) (catch Exception iex (ae/write-exception iex))))

(defn handler [f trace?]
  (partial (if trace? trace-exception handle-exception) f))

(defn create-view-driven-pages
  [project vdp]
  (doseq [t vdp]
    (let [parent (.getParent (io/file (:path t)))]
      (when parent (.mkdirs (io/file (se/site-path project) parent)))
      (spit (str (se/site-path project) (:path t)) (:content t)))))

(defn get-data-driven-pipeline
  [project live?]
  (filter #(not (nil?  %))
    (flatten
      (for [path (sf/get-data-directories project)]
        (if (is-detail? path #".edn")
          (let [f (io/file path)
                tpl (io/file (str (se/project-details-path project) (.getName f) ".html"))]
            (when (.exists (io/file tpl))
              (-> (pipes/data-detail-pipeline-> project (.listFiles f) tpl)
                  (pipes/gen-nav-data-pipeline->)
                  (pipes/inject-data-pipeline-> project)
                  (pipes/html-pipeline-> project live?))))
          (do-index-pages path))))))

(defn create-data-driven-pages
  [project ddp]
  (doseq [d ddp]
    (let [parent (.getParent (io/file (:path d)))
          raw (str (se/site-path project) (:path d))
          save-path (str (subs raw 0 (.lastIndexOf raw ".")) ".html")]
      (when parent (.mkdirs (io/file (se/site-path project) parent)))
      (spit save-path (:content d)))))

;; TODO config file?
(defn create-tipue-search-content-file
  [project tp]
  (if (not (nil? tp))
    (let [path (str (se/site-path project) "resource" (do/fs) "js" (do/fs))
          file (str path "tipuesearch_content.js")]
      (.mkdirs (io/file path))
      (spit file (str "var tipuesearch=" (json/write-str tp) ";"
                      (slurp (io/resource "tipuesearch_set.js")))))))

(defn store-project-dir
  "Writes the current project path and time to the central store."
  [project]
  (let [f se/spun-projects-file]
    (if (not (.exists f)) (.createNewFile f))
    (let [path (.getPath f)
          project-path (.getCanonicalPath (io/file project))
          sdf (doto (java.text.SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                    (.setTimeZone (java.util.TimeZone/getTimeZone "UTC")))
          now (.format sdf (java.util.Date.))
          old (with-open [rdr (clojure.java.io/reader path)] (doall (line-seq rdr)))
          removed (remove #(.contains % (str project-path ",")) old)
          formatted (apply str (map #(str % "\n") removed))
          updated (conj [(str project-path "," now "\n")]  formatted)]
      (spit path (apply str updated)))))
