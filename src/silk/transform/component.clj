(ns silk.transform.component
  "Component related transformations.  "
  (:require [clojure.java.io :refer [file]]
            [me.raynes.laser :as l]
            [silk.input.env :as se]
            [silk.input.file :as sf])
  (:use [clojure.string :only [split]]))

;; =============================================================================
;; Component transformations, see namespace comment
;; =============================================================================

(defn- build-component
  [i]
  (let [comp-str (str ((split i #":") 1) ".html")
        lcp (str se/pwd se/fs "components" se/fs comp-str)
        c-path (if (.exists (file lcp)) (file lcp) (sf/component comp-str))
        parsed-comp (l/parse c-path)]
    (l/select parsed-comp
              (l/child-of (l/element= :body) (l/any)))))

(defn process-components
  [t]
  (let [comps (l/select (l/parse (:content t)) (l/re-id #"silk-component"))
        comp-ids (map #(:id (:attrs %)) comps)]
    (assoc t :content
      (reduce
      (fn [c i]
        (l/document
          (l/parse c)
          (l/id= i)
          (l/replace (build-component i))))
      (:content t)
      (seq comp-ids)))))
