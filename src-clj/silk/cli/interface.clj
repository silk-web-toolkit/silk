(ns silk.cli.interface
  "Utilities and functions for the command line interface.")

;; =============================================================================
;; Constants
;; =============================================================================

;; commands

(def sites "sites")
(def spin "spin")
(def reload "reload")

;; =============================================================================
;; Interface args verification functions
;; =============================================================================

(defn add-svc-err? [{:keys [name status-err]}] (or (not name) (not status-err)))

(defn set-svc-err-prob? [{:keys [name level]}] (or (not name) (not level)))

(defn doc? [{:keys [name file directory]}]
  (or (not name) (not file) (not directory)))

(defn visit? [{:keys [file body]}] (or (not file) (not body)))
