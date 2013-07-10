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

(defonce silk-home (str user-home fs ".silk"))

(defonce runtime-templates-path
  (get (System/getenv)
    "SILK_RUNTIME_TEMPLATES_PATH"
    (str pwd fs "silk" fs "site" fs)))

(defonce templates-path
  (get (System/getenv)
    "SILK_TEMPLATES_PATH"
    (str pwd fs "template" fs)))

(defonce components-path (System/getenv "SILK_COMPONENTS_PATH"))

(defonce views-path
  (get (System/getenv)
    "SILK_VIEWS_PATH"
    (str pwd fs "view" fs)))

(defonce site-path
  (get (System/getenv)
    "SILK_SITE_PATH"
    (str pwd fs "site" fs)))

(defonce data-path (System/getenv "SILK_DATA_PATH"))
