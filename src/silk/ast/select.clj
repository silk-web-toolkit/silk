(ns silk.ast.select
  "AST selection."
  (:require [clojure.zip :as z]
            [me.raynes.laser :as l]))

(defn writeable?
  "Silk can transform this with data ?"
  []
  (l/or
   (l/attr? :data-sw-text)
   (l/attr? :data-sw-href)
   (l/attr? :data-sw-src)
   (l/attr? :data-sw-class)
   (l/attr? :data-sw-title)))

(defn repeat-orphaned?
  "Is not a descendant of a repeating element type ?"
  []
  (l/and
    (l/negate (l/descendant-of (l/element= :table) (l/attr? :data-sw-text)))
    (l/negate (l/descendant-of (l/element= :ul)    (l/attr? :data-sw-text)))))

(defn singular?
  "Is both writeable and not a descended from a repeating element type ?"
  []
  (l/and (writeable?) (repeat-orphaned?)))

(defn repeating?
  []
  (fn [loc]
    (or
     (= :tr (-> loc z/node :tag))
     (= :li (-> loc z/node :tag)))))
