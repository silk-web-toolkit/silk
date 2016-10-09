(ns silk.core.input.ast
  "AST input functions, describes the shape of aspects of the AST and enables
   selection over them."
  (:require [me.raynes.laser :as l]
            [me.rossputin.diskops :as do]
            [silk.core.transform.element :as sel]))

;; =============================================================================
;; Helper functions
;; =============================================================================

(defn- split-keyword-args
  "Split an argument list into a map of keyword arguments and their values
   and a seq of the rest of the arguments. Assumes all of the keyword arguments
   come first in the list."
  [args]
  (let [[others keyword-args] ((juxt drop-while take-while)
                               (comp keyword? first)
                               (partition-all 2 args))]
    [(into {} (map vec keyword-args)) (apply concat others)]))


;; =============================================================================
;; AST domain description functions, see namespace comment
;; =============================================================================


(defn get-component-attribs
  []
  [:data-sw-component :data-sw-source :data-sw-type :data-sw-limit :data-sw-sort :data-sw-sort-dir :data-sw-parent])

;; TODO: very proto code (POC)
(defn get-dynamic-attribs
  []
  [:data-sw-text :data-sw-href :data-sw-class :data-sw-src :data-sw-title :data-sw-id :data-sw-parent])


;; =============================================================================
;; Selection input functions, see namespace comment
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
   (l/attr? :data-sw-title)
   (l/attr? :data-sw-id)
   (l/attr? :data-sw-parent)))

(defn repeat-orphaned?
  "Is not a descendant of a repeating element type and is a data writeable ?"
  []
  (l/and
    (l/negate (l/descendant-of (l/element= :tbody) (l/attr? :data-sw-text)))
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
  (l/or
    (l/and (l/element= :tr) (l/descendant-of (l/element= :tbody) (l/any)))
    (l/element= :li)))

(defn repeating?
  []
  (l/and (repeat-orphaned-permissive?) (repeat-node?)))


;; high level selectors - Silk domain
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn meta-el
  "Get a specific meta with name attribue value matching name param."
  [v name]
  (l/select v (l/and (l/element= :meta) (l/attr= :name name))))

(defn body-content
  "Get sequence of children of a body element."
  [m]
  (l/select m (l/child-of (l/element= :body) (l/any))))


;; document processing
;;;;;;;;;;;;;;;;;;;;;;

(defmacro defdocrel
  "Relativises the result of this document inclusion.
   Must be used if the template pulls in components for example."
  [name s fargs & args]
  (let [[{bindings :let, parser :parser, resource :resource} fns]
          (split-keyword-args args)]
    `(let [html# (l/parse ~s :parser ~parser :resource ~resource)]
       (defn ~name ~fargs
         (let ~(or bindings [])
           (:content (sel/relativise-attrs :a :href
                                   {:path (.getPath ~s)
                                    :content (l/document html# ~@fns)}
                                   true)))))))
