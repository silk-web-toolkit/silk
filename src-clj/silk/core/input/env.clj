(ns silk.core.input.env
  "Configuration of environment."
  (:require [clojure.java.io :refer [file]]
            [me.rossputin.diskops :as do])
  (:import java.io.File))

;; =============================================================================
;; Environmental properties, see namespace comment
;; =============================================================================

(defonce os (System/getProperty "os.name"))

(defonce user-home (System/getProperty (str "user.home")))

;; used for last spun time and silk project list
(defonce silk-home
  (get (System/getenv)
    "SILK_PATH"
    (str user-home (do/fs) ".silk")))

(defonce spun-projects-file (file (str silk-home (do/fs) "spun-projects.txt")))

;; used by quantum-resource in static spins to get component and fallback
;; local/env var/shared
;; used by server compile time 'component' artefact caching
(defonce components-path
  (get (System/getenv)
    "SILK_COMPONENTS_PATH"
    (str silk-home (do/fs) "components")))

(defn meta-path [project] (str project (do/fs) "meta" (do/fs)))

(defn resource-path [project] (str project (do/fs) "resource" (do/fs)))

(defn site-path [project] (str project (do/fs) "site" (do/fs)))

(defn views-path [project] (str project (do/fs) "view" (do/fs)))

(defn project-data-path [project] (str project (do/fs) "data" (do/fs)))

;; configured to work with static spins and server compile time 'page' artefact caching
(defn project-templates-path
  [project]
  (get (System/getenv)
    "SILK_TEMPLATES_PATH"
    (str project (do/fs) "template" (do/fs))))

(defn project-details-path [project] (str (project-templates-path project) "detail" (do/fs)))

(defonce data-path (str silk-home (do/fs) "data"))

(defonce sw-path (str silk-home (do/fs) "sw"))

(defonce sw-views-path (str sw-path (do/fs) "views"))

(defonce sw-bookmarks-path (str sw-path (do/fs) "bookmarks"))
