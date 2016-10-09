(ns silk.core.input.data
  "Data input functions."
  (:require [clojure.edn :as edn])
  (:import java.net.URLDecoder))

;; =============================================================================
;; Helper functions
;; =============================================================================

(def cached-datums [])

(defn read-datum [datum]
  (if-let [f (seq (filter #(= (:path %) (:sw/path datum)) cached-datums))]
    (:content (first f))
    (let [p (:sw/path datum)
          c (edn/read-string (slurp p))]
      (def cached-datums (conj cached-datums {:path p :content c}))
      c)))

(defn- enhance-datum-content [datum] (assoc datum :content (read-datum datum)))


;; =============================================================================
;; Metadata and content data extraction, see namespace comment
;; =============================================================================

(defn datum-extract
  "Determine if the data item we want is in the datum, if not try supplementing
   by loading edn file content."
  [datum item]
  (if-let [ext (or (item datum) (item (:content (enhance-datum-content datum))))]
    (if (.contains (name item) "-html")
      (URLDecoder/decode ext)
      ext)
    (name item)))
