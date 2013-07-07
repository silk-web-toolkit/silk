(ns silk.transform.component
  "Component related transformations.  "
  (:require [clojure.java.io :refer [file]]
            [me.raynes.laser :as l]
            [silk.input.env :as se]
            [silk.input.file :as sf]
            [clojure.walk :as walk])
  (:use [clojure.string :only [split]]))

;; =============================================================================
;; Component transformations, see namespace comment
;; =============================================================================

(defn- get-component-markup
  [path]
  (let [comp-str (str path ".html")
        lcp (str se/pwd se/fs "components" se/fs comp-str)
        c-path (if (.exists (file lcp)) (file lcp) (sf/component comp-str))
        parsed-comp (l/parse c-path)]
    (l/select parsed-comp
              (l/child-of (l/element= :body) (l/any)))))

(defn- get-component-datasource
  [data-params]
  (let [source (:data-sw-source data-params)
        data (sf/get-data-meta source)]
    data))

(defn- keys? [m keys]
  (apply = (map count [keys (select-keys m keys)])))

(defn- transcend
  [node datum]
  (let [attr (keyword (:data-sw-rtext (:attrs node)))]
    (assoc node :content [(attr datum)])))

(defn- eval-element
  [node datum]
  (if (and (= :element (:type node)) (keys? (:attrs node) [:data-sw-rtext]))
    (transcend node datum)
    node))

(defn- repeated-transform
  [node datum]
  (walk/postwalk #(eval-element % datum) node))

(defn- repeat-component
  [data]
  (fn [node] (map #(repeated-transform node %) data)))

(defn- build-component
  [comp-params]
  (let [path (:data-sw-component comp-params)
        raw-markup (l/parse-fragment (get-component-markup path))
        markup (filter #(= (type (first %)) clojure.lang.PersistentArrayMap) raw-markup)
        data (get-component-datasource comp-params)]
    ;; parse repeatable components (eat lists)
    (l/parse (l/to-html
              (l/at (first markup)
                    (l/attr? "data-sw-r") (repeat-component data))))
    ;; parse singleton components (eat map entries)

    ))

(defn process-components
  [t]
  (let [comps (l/select (l/parse (:content t)) (l/attr? "data-sw-component"))
        comp-ids (map #(select-keys (:attrs %) [:data-sw-component :data-sw-source]) comps)]
    (assoc t :content
      (reduce
      (fn [c i]
        (l/document
          (l/parse c)
          (l/attr= "data-sw-component" (:data-sw-component i))
          (l/replace (build-component i))))
      (:content t)
      (seq comp-ids)))))
