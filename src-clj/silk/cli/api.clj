(ns silk.cli.api
  (:require [me.rossputin.diskops :as do]
            [silk.core.input.env :as se]
            [silk.core.input.file :as sf]
            [silk.core.transform.path :as sp]
            [silk.core.transform.pipeline :as pipes]
            ; [watchtower.core :as watch]
            [hawk.core :as hawk]
            [silk.cli.io :as io])
  (:use [clojure.string :only [split]])
  (import java.io.File)
  (:gen-class))

;; =============================================================================
;; Helper functions
;; =============================================================================

(defn- spin
  [live?]
  (io/display-spin-start)
  (io/check-silk-configuration)
  (io/check-silk-project-structure)
  (io/side-effecting-spin-io)
  (let [view (-> (pipes/view-pipline->)
                 (pipes/gen-nav-data-pipeline->)
                 (pipes/inject-data-pipeline->)
                 (pipes/html-pipeline-> live?))
        data (io/get-data-driven-pipeline live?)
        text (pipes/text-pipeline-> (concat view data))]
    (io/create-view-driven-pages view)
    (io/create-data-driven-pages data)
    (io/create-tipue-search-content-file text))
  (io/store-project-dir)
  (io/display-spin-end))

(def spin-handled (io/handler spin io/handle-silk-project-exception))

(def spin-traced (io/handler spin io/trace-silk-project-exception))

(defn- spin-handler
  [live? trace?]
  (if trace?
    (spin-traced live?)
    (spin-handled live?)))

(defn- reload
  [live? trace?]
  (spin-handler live? trace?)
  (println "Watching for changes. Press enter to exit")
  (let [cp se/current-project
        sp (.getAbsolutePath (clojure.java.io/file (se/site-path)))]
    (hawk/watch! [{:paths [se/current-project]
                   :filter (fn [_ {:keys [file]}]
                             (binding [se/current-project cp] ; current-project is lost otherwise
                               (not (or (= (.getAbsolutePath file) cp)
                                        (.startsWith (.getAbsolutePath file) sp)
                                        (.isHidden file)))))
                   :handler (fn [ctx e]
                              (binding [se/current-project cp] ; current-project is lost otherwise
                                (println (name (:kind e)) (sp/relativise-> cp (:file e)))
                                (spin-handler live? trace?)
                                (println "Watching for changes. Press enter to exit")
                                ctx))}]))
  (loop [input (read-line)]
    (when-not (= "\n" input)
      (System/exit 0)
      (recur (read-line)))))

(defn- sites
  []
  (io/check-silk-configuration)
  (println "Your Silk sites are : ")
  (with-open [rdr (clojure.java.io/reader se/spun-projects-file)]
    (doseq [line (line-seq rdr)]
      (let [splitStr (split line #",")
            path (first splitStr)
            date (new java.util.Date (read-string (second splitStr)))
            date-str (.format (new java.text.SimpleDateFormat) date)]
        (println  "Last spun:" date-str path)))))

(defn- calc-project-path
  [directory]
  (if (empty? directory)
    (do/pwd)
    (if (.endsWith directory (do/fs))
      (subs directory 0 (.lastIndexOf directory (do/fs)))
      directory)))

;; =============================================================================
;; Public API
;; =============================================================================

(def sites-handled (io/handler sites io/handle-silk-project-exception))

(defn spin-or-reload
  [reload? directory live? trace?]
  (binding [se/current-project (.getAbsolutePath (clojure.java.io/file (calc-project-path directory)))]
    (if reload?
      (reload live? trace?)
      (spin-handler live? trace?))))
