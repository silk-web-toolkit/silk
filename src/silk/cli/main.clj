(ns silk.cli.main
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
  [args]
  (io/display-spin-start)
  (io/check-silk-configuration)
  (io/check-silk-project-structure)
  (io/side-effecting-spin-io)
  (pipes/preprocessor-> (first args))
  (io/create-view-driven-pages (pipes/view-driven-pipeline-> (first args)))
  (io/create-data-driven-pages (first args))
  (io/store-project-dir)
  (io/display-spin-end))

(def spin-handled (io/handler spin io/handle-silk-project-exception))

(defn- reload-report
  [payload]
  (io/display-files-changed payload)
  (spin-handled ["spin"])
  (println "Press enter to exit"))

(defonce hidden-paths (str (do/pwd) (do/fs) "."))

(defn- ignore-directories
  "A file filter that removes Silk Site and hidden directories."
  [f]
  (not (or (.startsWith (.getCanonicalPath f) se/site-path)
              (.startsWith (.getCanonicalPath f) hidden-paths))))

(defn- reload
  []
  (future (watch/watcher [(do/pwd)]
    (watch/rate 500) ;; poll every 500ms
    (watch/file-filter watch/ignore-dotfiles) ;; ignore any dotfiles
    (watch/file-filter ignore-directories) ;; ignore file in Silk site directory
    (watch/notify-on-start? true)   ;; Determines whether notifications are made
    (watch/on-modify #(reload-report %))
    (watch/on-add #(reload-report %))
    (watch/on-delete #(reload-report %))))

  (loop [input (read-line)]
    (when-not (= "\n" input)
      (System/exit 0)
      (recur (read-line)))))

(defn sites
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

(def sites-handled (io/handler sites io/handle-silk-project-exception))

(defn launch
  [args]
  (io/cli-app-banner-display)
  (cond
   (= (first args) "reload") (reload)
   (= (first args) "sites")  (sites-handled)
   :else (spin-handled args)))

;; =============================================================================
;; Application entry point
;; =============================================================================

(defn -main
  [& args]
  (launch args))
