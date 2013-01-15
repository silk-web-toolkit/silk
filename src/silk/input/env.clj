(ns silk.input.env
  "Configuration of environment."
  (:require [clojure.java.io :refer [file]])
  (:import java.io.File))

(defonce pwd (. (file ".") getCanonicalPath))

(defonce user-home (System/getProperty (str "user.home")))

(defonce fs (File/separator))

(defonce silk-home (str user-home fs ".silk"))

(defonce templates-path
  (get (System/getenv)
    "SILK_TEMPLATES_PATH" 
    (str pwd fs "silk" fs "site" fs)))

(defonce components-path
  (get (System/getenv)
    "SILK_COMPONENTS_PATH" 
    (str silk-home fs "repositories" fs "components" fs)))
