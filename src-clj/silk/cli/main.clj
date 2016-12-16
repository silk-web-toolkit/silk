(ns silk.cli.main
  "A basic command line interface for Protean."
  (:require [clojure.string :as s]
            [clojure.java.io :refer [file]]
            [clojure.tools.cli :refer [parse-opts]]
            [io.aviso.ansi :as aa]
            [silk.cli.api :as api]
            [silk.cli.interface :as i])
  (:gen-class))

;; =============================================================================
;; Helper functions
;; =============================================================================

(defmacro get-version []
  (System/getProperty "silk.version"))

(defn- cli-banner []
  (println      "    _ _ _")
  (println      " __(_) | |__")
  (println      "(_-< | | / /")
  (println (str "/__/_|_|_\\_\\ " "v" (get-version)))
  (println ""))

(defn- nice-keys [m] (into {} (for [[k v] m] [(keyword k) v])))

(defn- nice-vals [v] (into [] (map #(keyword %) v)))

(defn- sane-corpus [m] (-> m nice-keys (update-in [:commands] nice-vals)))

(def cli-options
   [["-a" "--auto" "(Auto spin on file updates)"]
    ["-d" "--directory DIRECTORY" "Path to site"]
    ["-l" "--live" "Live relativisation paths"]
    ["-t" "--trace" "Display error stack trace (For development)"]
    ["-h" "--help"]
    ["-v" "--version"]])

(defn- usage-hud [options-summary]
  (->> [""
        "Usage: silk [options] action"
        ""
        "Please note if you do not specify a directory Silk will spin/reload the current one."
        ""
        "Options:"
        options-summary
        ""
        "Actions:"
        "sites                   (List spun Sites)"
        "spin                    -d mysite (Spin once)"]
       (s/join \newline)))

(defn- usage [options-summary] (cli-banner) (usage-hud options-summary))

(defn- usage-exit [options-summary] (usage-hud options-summary))

(defn- error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (s/join \newline errors)))

(defn- exit [status msg] (println msg) (System/exit status))

;; =============================================================================
;; Application entry point
;; =============================================================================

(defn- bomb [summary] (exit 0 (usage summary))) ; exit nicely and print usage

(defn- handle-errors
  [{:keys [name directory] :as options} arguments errors summary]
  (let [cmd (first arguments)]
    (cond
      (:help options) (bomb summary)
      (:version options) (exit 0 (get-version))
      (not= (count arguments) 1) (exit 1 (usage summary))
      errors (exit 1 (error-msg errors)))))

(defn -main [& args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)
        cmd (first arguments)
        auto? (:auto options)
        directory (:directory options)
        live? (:live options)
        trace? (:trace options)
        help? (:help options)
        version? (:version options)]
    (handle-errors options arguments errors summary)
    (cond
      (= cmd i/sites) (api/sites-handled)
      (= cmd i/spin)  (api/spin-or-reload auto? directory live? trace?)
      :else (exit 1 (usage-exit summary)))))
