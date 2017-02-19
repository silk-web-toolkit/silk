(ns silk.cli.io
  (:require [silk.core.input.env :as se]
            [silk.core.input.file :as sf]
            [silk.core.transform.pipeline :as pipes]
            [clojure.java.io :refer [file]]
            [clojure.data.json :as json]
            [io.aviso.ansi :as aa]
            [io.aviso.exception :as ae]
            [me.rossputin.diskops :as do])
  (import java.io.File))

;; =============================================================================
;; Helper functions
;; =============================================================================

(defn- filter-file [r]
  (reify java.io.FilenameFilter
    (accept [_ d name] (not (nil? (re-find r name))))))

(defn- is-detail? [d r]
  (let [files (.list (file d) (filter-file r))]
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

(defn side-effecting-spin-io []
  (let [s (se/site-path) r (se/resource-path) m (se/meta-path)]
    (when (do/exists-dir? s) (do/delete-directory s))
    (when (do/exists-dir? se/sw-path) (do/delete-directory se/sw-path))
    (.mkdir (File. s))
    (.mkdir (File. se/sw-path))
    (when (do/exists-dir? r) (do/copy-recursive r s))
    (when (do/exists-dir? m) (do/copy-file-children m s))))

(defn silk-project? []
  (and (do/exists-dir? (se/views-path)) (do/exists-dir? (se/project-templates-path))))

;; last spun time and silk projects both live in silk home
(defn silk-configured? [] (do/exists-dir? se/silk-home))

(defn check-silk-configuration []
  (if (not (silk-configured?))
    (do
      (println
        (aa/bold-red "WARNING: Creating missing shared directory"))
        (.mkdirs (File. se/silk-home)))))

(defn check-silk-project-structure []
  (if (not (silk-project?))
    (do
      (throw (IllegalArgumentException. "Not a Silk project, a directory may be missing - template or view ?")))))

(defn handler [f & handlers]
  (reduce (fn [handled h] (partial h handled)) f (reverse handlers)))

(defn handle-silk-project-exception [f & args]
  (try
    (apply f args)
    (catch IllegalArgumentException iex
      (println (str (aa/bold-red "ERROR: ") (aa/italic "Sorry, either Silk is not configured properly or there is a problem with this Silk project.")))
      (println (str (aa/bold-red "CAUSE: ") (aa/italic (.getMessage iex)))))
    (catch Exception ex
      (println (str (aa/bold-red "ERROR: ") (aa/italic "Sorry, there was a problem, either a component or datasource is missing or this is not a Silk project ?")))
      (println (str (aa/bold-red "CAUSE: ") (aa/italic (.getMessage ex)))))))

(defn trace-silk-project-exception [f & args]
  (try (apply f args) (catch Exception iex (ae/write-exception iex))))

(defn create-view-driven-pages [vdp]
  (doseq [t vdp]
    (let [parent (.getParent (File. (:path t)))]
      (when parent (.mkdirs (File. (se/site-path) parent)))
      (spit (str (se/site-path) (:path t)) (:content t)))))

(defn get-data-driven-pipeline [live?]
  (filter #(not (nil?  %))
    (flatten
      (for [path (sf/get-data-directories)]
        (if (is-detail? path #".edn")
          (let [f (file path)
                tpl (file (str (se/project-details-path) (.getName f) ".html"))]
            (when (.exists (file tpl))
              (-> (pipes/data-detail-pipeline-> (.listFiles f) tpl)
                  (pipes/gen-nav-data-pipeline->)
                  (pipes/inject-data-pipeline->)
                  (pipes/html-pipeline-> live?))))
          (do-index-pages path))))))

(defn create-data-driven-pages [ddp]
  (doseq [d ddp]
    (let [parent (.getParent (new File (:path d)))
          raw (str (se/site-path) (:path d))
          save-path (str (subs raw 0 (.lastIndexOf raw ".")) ".html")]
      (when parent (.mkdirs (File. (se/site-path) parent)))
      (spit save-path (:content d)))))

;; TODO config file?
(defn create-tipue-search-content-file [tp]
  (if (not (nil? tp))
    (let [path (str (se/site-path) "resource" (do/fs) "js" (do/fs))
          file (str path "tipuesearch_content.js")]
      (.mkdirs (File. path))
      (spit file (str
        "var tipuesearch=" (json/write-str tp)
        ";var tipuedrop=tipuesearch;"
        "var tipuesearch_stop_words = [\"and\", \"be\", \"by\", \"do\", \"for\", \"he\", \"how\", \"if\", \"is\", \"it\", \"my\", \"not\", \"of\", \"or\", \"the\", \"to\", \"up\", \"what\", \"when\"];"
        "var tipuesearch_replace = {\"words\": []};"
        "var tipuesearch_stem = {\"words\": []};")))))

(defn store-project-dir
  "Writes the current project path and time to the central store."
  []
  (let [f se/spun-projects-file]
    (if (not (.exists f)) (.createNewFile f))
    (let [path (.getPath f)
          millis (.getTime (new java.util.Date))
          old (with-open [rdr (clojure.java.io/reader path)] (doall (line-seq rdr)))
          removed (remove #(.contains % (str se/current-project ",")) old)
          formatted (apply str (map #(str % "\n") removed))
          updated (conj [(str se/current-project "," millis "\n")]  formatted)]
      (spit path (apply str updated)))))
