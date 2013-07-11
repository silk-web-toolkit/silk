(ns silk.transform.path
  "Path transformations.
   Silk specific, ie component namespaces, OS and relativisation related."
   (:require [pathetic.core :as path])
   (:use [clojure.string :only [split]]))

;; =============================================================================
;; Path transformation functions, see namespace comment
;; =============================================================================

(defn win2nix->
  "Convert a Windows path to a *nix path."
  [p]
  (if (re-find #"indow" (System/getProperty "os.name"))
    (.replaceAll ((split p #":") 1) "\\\\" "/")
    p))

(defn relativise->
  "Calculate the difference between a src path and destination path."
  [a b]
  (path/relativize (win2nix-> a) (win2nix-> b)))
