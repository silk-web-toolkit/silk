(ns silk.ast.select
  "AST selection."
  (:require [clojure.zip :as z]
            [me.raynes.laser :as l]))

(defn repeating?
  []
  (fn [loc]
    (or
     (= :tr (-> loc z/node :tag))
     (= :li (-> loc z/node :tag)))))

(defn singular?
  []
  (fn [loc]
    (and
     (l/negate (l/descendant-of (l/element= :tr) (l/attr? "data-sw-text")))
     (l/negate (l/descendant-of (l/element= :li) (l/attr? "data-sw-text")))
     (l/attr? "data-sw-text"))))
