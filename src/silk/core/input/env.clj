(ns silk.core.input.env
  "Configuration of environment."
  (:require [clojure.java.io :refer [file]]
            [me.rossputin.diskops :as do])
  (:import java.io.File))

;; =============================================================================
;; Environmental properties, see namespace comment
;; =============================================================================

(def ^:dynamic current-project "")

(defonce os (System/getProperty "os.name"))

(defonce user-home (System/getProperty (str "user.home")))

;; used for last spun time and silk project list
(defonce silk-home
  (get (System/getenv)
    "SILK_PATH"
    (str user-home (do/fs) ".silk")))

(defonce spun-projects-file (file (str silk-home (do/fs) "spun-projects.txt")))

;; configured to work with static spins and server compile time 'page' artefact caching
(defn templates-path []
  (get (System/getenv)
    "SILK_TEMPLATES_PATH"
    (str current-project (do/fs) "template" (do/fs))))

(defonce templates-details-path
  (str templates-path "detail" (do/fs)))

;; used by quantum-resource in static spins to get component and fallback
;; local/env var/shared
;; used by server compile time 'component' artefact caching
(defonce components-path
  (get (System/getenv)
    "SILK_COMPONENTS_PATH"
    (str silk-home (do/fs) "components")))

(defn views-path [] (str current-project (do/fs) "view" (do/fs)))

(defn site-path [] (str current-project (do/fs) "site" (do/fs)))

(defn project-data-path [] (str current-project (do/fs) "data" (do/fs)))

(defonce data-path (str silk-home (do/fs) "data"))

(defonce sw-path (str silk-home (do/fs) "sw"))

(defonce sw-views-path (str sw-path (do/fs) "views"))

(defonce sw-bookmarks-path (str sw-path (do/fs) "bookmarks"))
