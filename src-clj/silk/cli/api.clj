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

(defn- spin
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

(defn- single-spin
  [project live? trace?]
  (let [spin-handled (io/handler spin io/handle-silk-project-exception)
        spin-traced  (io/handler spin io/trace-silk-project-exception)]
    (if trace?
      (spin-traced  project live?)
      (spin-handled project live?))))

(defn- reload-spin
  [project live? trace?]
  (single-spin project live? trace?)
  (println "Watching for changes. Press enter to exit")
  (let [paths [se/components-path ;; GLOBAL
               se/data-path ;; GLOBAL
               (str project (do/fs) "components" (do/fs))
               (se/project-data-path project)
               (se/meta-path project)
               (se/resource-path project)
               (se/project-templates-path project)
               (se/views-path project)]
        hnd (fn [ctx {file :file kind :kind}]
              (println (name kind) (sp/relativise-> project file))
              (single-spin project live? trace?)
              (println "Watching for changes. Press enter to exit")
              ctx)]
    (hawk/watch! [{:paths (filter #(.exists (clojure.java.io/file %)) paths)
                   :filter hawk/file?
                   :handler hnd}])
    (loop [input (read-line)]
      (when-not (= "\n" input)
        (System/exit 0)
        (recur (read-line))))))

(defn- sites
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

(def sites-handled (io/handler sites io/handle-silk-project-exception))

(defn spin-or-reload
  [reload? directory live? trace?]
  (if reload?
    (reload-spin (project-path directory) live? trace?)
    (single-spin (project-path directory) live? trace?)))
