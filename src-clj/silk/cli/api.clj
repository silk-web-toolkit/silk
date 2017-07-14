(ns silk.cli.api
  (:require [me.rossputin.diskops :as do]
            [silk.core.input.env :as se]
            [silk.core.input.file :as sf]
            [silk.core.transform.path :as sp]
            [silk.core.transform.pipeline :as pipes]
            [hawk.core :as hawk]
            [silk.cli.io :as io])
  (:use [clojure.string :only [split]])
  (:gen-class))

;; =============================================================================
;; Helper functions
;; =============================================================================

(defn- silk-spin
  [project live?]
  (io/display-spin-start)
  (io/check-silk-configuration)
  (io/check-silk-project-structure project)
  (io/side-effecting-spin-io project)
  (let [view (-> (pipes/view-pipline-> project)
                 (pipes/gen-nav-data-pipeline->)
                 (pipes/inject-data-pipeline-> project)
                 (pipes/html-pipeline-> project live?))
        data (io/get-data-driven-pipeline project live?)
        text (pipes/text-pipeline-> (concat view data))]
    (io/create-view-driven-pages project view)
    (io/create-data-driven-pages project data)
    (io/create-tipue-search-content-file project text))
  (io/store-project-dir project)
  (io/display-spin-end))

(defn- silk-sites
  []
  (io/check-silk-configuration)
  (println "Silk Sites\nLast Spun      Path")
  (with-open [rdr (clojure.java.io/reader se/spun-projects-file)]
    (doseq [line (line-seq rdr)]
      (let [[path date-raw] (split line #",")
            date (clojure.instant/read-instant-date date-raw)
            date-str (.format (new java.text.SimpleDateFormat) date)]
        (println  date-str path)))))

(defn- project-path
  [d]
  (.getAbsolutePath
    (clojure.java.io/file
      (cond
        (empty? d)            (do/pwd)
        (.endsWith d (do/fs)) (subs d 0 (.lastIndexOf d (do/fs)))
        :else                 d))))

;; =============================================================================
;; Public API
;; =============================================================================

(defn print-sites
  ([] (print-sites false))
  ([trace?] (apply (io/handler silk-sites trace?) [])))

(defn spin
  ([dir] (spin dir false false))
  ([dir live?] (spin dir live? false))
  ([dir live? trace?]
    (apply (io/handler silk-spin trace?) [(project-path dir) live?])))

(defn auto-spin
  ([dir] (auto-spin dir false false))
  ([dir live?] (auto-spin dir live? false))
  ([dir live? trace?]
    (spin dir live? trace?)
    (println "Watching for changes. Press enter to exit")
    (let [project (project-path dir)
          paths [se/components-path ;; GLOBAL
                 se/data-path ;; GLOBAL
                 (str project (do/fs) "components" (do/fs))
                 (se/project-data-path project)
                 (se/meta-path project)
                 (se/resource-path project)
                 (se/project-templates-path project)
                 (se/views-path project)]
          hnd (fn [ctx {file :file kind :kind}]
                (println (name kind) (sp/relativise-> project file))
                (spin dir live? trace?)
                (println "Watching for changes. Press enter to exit")
                ctx)]
      (hawk/watch! [{:paths (filter #(.exists (clojure.java.io/file %)) paths)
                     :filter hawk/file?
                     :handler hnd}])
      (loop [input (read-line)]
        (when-not (= "\n" input)
          (System/exit 0)
          (recur (read-line)))))))
