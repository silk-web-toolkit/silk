(ns silk.input.env
  "Configuration of environment."
  (:require [clojure.java.io :refer [file]])
  (:import java.io.File))

;; =============================================================================
;; Environmental properties, see namespace comment
;; =============================================================================

(defonce os (System/getProperty "os.name"))

(defonce pwd (. (file ".") getCanonicalPath))

(defonce user-home (System/getProperty (str "user.home")))

(defonce fs (File/separator))

(defonce silk-home 
  (get (System/getenv)
    "SILK_PATH"
    (str user-home fs ".silk")))

(defonce spun-projects-file (file (str silk-home fs "spun-projects.txt")))

(defonce runtime-templates-path
  (get (System/getenv)
    "SILK_RUNTIME_TEMPLATES_PATH"
    (str pwd fs "silk" fs "site" fs)))

(defonce templates-path
  (get (System/getenv)
    "SILK_TEMPLATES_PATH"
    (str pwd fs "template" fs)))

(defonce components-path
  (get (System/getenv)
    "SILK_COMPONENTS_PATH"
    (str silk-home fs "components")))

(defonce views-path (str pwd fs "view" fs))

(defonce site-path (str pwd fs "site" fs))

(defonce data-path (str silk-home fs "data"))
