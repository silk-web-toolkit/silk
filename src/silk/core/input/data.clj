(ns silk.core.input.data
  "Data input functions."
  (:require [clojure.edn :as edn]))

;; =============================================================================
;; Helper functions
;; =============================================================================

(defn- enhance-datum-content
  [datum]
  (assoc datum :content (edn/read-string (slurp (:path datum)))))


;; =============================================================================
;; Metadata and content data extraction, see namespace comment
;; =============================================================================

(defn datum-extract
  "Determine if the data item we want is in the datum, if not try supplementing
   by loading edn file content."
  [datum item]
  (or (item datum) (item (:content (enhance-datum-content datum)))))
