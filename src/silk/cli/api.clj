(ns silk.cli.api
  (:require [me.rossputin.diskops :as do]
            [silk.core.input.env :as se]
            [silk.core.input.file :as sf]
            [silk.core.transform.pipeline :as pipes]
            [watchtower.core :as watch]
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

(defn- reload-report
  [payload live? trace?]
  (io/display-files-changed payload)
  (if trace? (spin-traced live?) (spin-handled live?))
  (println "Press enter to exit"))

(defn- reload-filter
  "A file filter that removes Silk Site and hidden directories."
  [f]
  (not (or (.startsWith (.getCanonicalPath f) (se/site-path))
           (.startsWith (.getCanonicalPath f) (str se/current-project (do/fs) ".")))))

(defn- reload
  [live? trace?]
  (future (watch/watcher [se/current-project]
    (watch/rate 500) ;; poll every 500ms
    (watch/file-filter watch/ignore-dotfiles) ;; ignore any dotfiles
    (watch/file-filter reload-filter) ;; ignore files in Silk "site" directory
    (watch/notify-on-start? true)   ;; Determines whether notifications are made
    (watch/on-modify #(reload-report % live? trace?))
    (watch/on-add #(reload-report % live? trace?))
    (watch/on-delete #(reload-report % live? trace?))))
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
  (binding [se/current-project (calc-project-path directory)]
    (if reload?
      (reload live? trace?)
      (if trace?
        (spin-traced live?)
        (spin-handled live?)))))
