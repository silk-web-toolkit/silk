(ns silk.core.transform.view
  "View related transformations.
   Principally view driven."
  (:require [hickory.core :as h]
            [hickory.select :as hs]
            [com.rpl.specter :as spec]
            [me.raynes.laser :as l]
            [silk.core.input.env :as se]
            [silk.core.input.ast :as ds]
            [silk.core.input.file :as sf]
            [silk.core.transform.ast :as tx]
            [silk.core.transform.path :as sp])
  (:use [clojure.string :only [lower-case split]]))

;; =============================================================================
;; Helper functions
;; =============================================================================

(defn- template-path
  "Generates the template path, default template loaded if not specifed"
  [meta]
  (if-let [t (:template meta)]
    (sf/template (str t ".html"))
    (sf/template "default.html")))

(defn- meta-map
  "Map of name and content values found in meta tags"
  [html-hickory]
  (into (sorted-map) (map #(hash-map (keyword (lower-case (:name (:attrs %))))
                                     (lower-case (str (:content (:attrs %)))))
                          (hs/select (hs/tag :meta) html-hickory))))

(defn- parse-file
  "Converts a HMTL file into hickory"
  [f]
  (h/as-hickory (h/parse (slurp f))))

(defn- add-meta-data
  [meta-node meta-map]
  (if-let [m (find meta-map (keyword (get-in meta-node [:attrs :name])))]
    (assoc-in meta-node [:attrs :content] (str (second m)))
    meta-node))

; TODO add title if not in template
; TODO add meta if not in template
; TODO add silk view class (str "silk-view-" (first (split name #"\.")))
(defn- view-inject
  "Wraps the view with the template specifed in the HMTL meta header"
  [v]
  (let [parsed-view (parse-file v)
        vtitle (-> (hs/select (hs/tag :title) parsed-view) first :content first)
        vbody-attrs (-> (hs/select (hs/tag :body) parsed-view) first :attrs)
        meta (meta-map parsed-view)
        parsed-template (parse-file (template-path meta))
        name (.getName v)]
    {:name name
     :path (sp/relativise-> (se/views-path) (.getPath v))
     :nav (:data-sw-nav vbody-attrs)
     :priority (:data-sw-priority vbody-attrs)
     :content (->> parsed-template
                   (spec/transform (spec/walker #(= (:tag %) :title))
                                   #(assoc % :content vtitle))
                   (spec/transform (spec/walker #(= (:tag %) :meta))
                                   #(add-meta-data % meta))
                   (spec/transform (spec/walker #(:data-sw-view (:attrs %)))
                                   #(assoc % :content (:content parsed-view)))
                   h/hickory-to-html)}))


;; =============================================================================
;; Payload transformation functions, see namespace comment
;; =============================================================================

(defn template-wrap->
  "Adds the template to the view"
  []
  (map #(view-inject %) (sf/get-views)))

(defn template-wrap-detail->
  [{path :path template :template}]
  (let [wrapped (map #(view-inject %) (take (count path) (repeat template)))]
    (for [p path w wrapped]
      (let [rel-p (sp/relativise-> (se/project-data-path) (.getPath p))
            data-inj
            (l/document
             (l/parse (:content w))
             (l/attr? :data-sw-component)
             (l/attr :data-sw-source rel-p))]
        (assoc w :path rel-p :content data-inj)))))
