(ns silk.ast.select
  "AST selection."
  (:require [clojure.zip :as z]
            [me.raynes.laser :as l]))

;; =============================================================================
;; Selection functions, see namespace comment
;; =============================================================================

;; low level selectors - HTML
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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
  "Is not a descendant of a repeating element type and is a data writeable ?"
  []
  (l/and
    (l/negate (l/descendant-of (l/element= :table) (l/attr? :data-sw-text)))
    (l/negate (l/descendant-of (l/element= :ul)    (l/attr? :data-sw-text)))))

(defn singular?
  "Is both writeable and not a descended from a repeating element type ?"
  []
  (l/and (writeable?) (repeat-orphaned?)))

(defn repeat-orphaned-permissive?
  "Is not a descendant of a repeating element type ?"
  []
  (l/and
    (l/negate (l/descendant-of (l/element= :tr) (l/any)))
    (l/negate (l/descendant-of (l/element= :li) (l/any)))))

(defn repeat-node?
  []
  (l/or (l/element= :tr) (l/element= :li)))

(defn repeating?
  []
  (l/and (repeat-orphaned-permissive?) (repeat-node?)))


;; high level selectors - Silk domain
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn template
  [v]
  (l/select v (l/and (l/element= :meta) (l/attr= :name "template"))))
