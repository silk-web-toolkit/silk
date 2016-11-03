(ns silk.core.transform.view
  "View related transformations.
   Principally view driven."
  (:require [hickory.select :as hs]
            [com.rpl.specter :as spec]
            [silk.core.input.env :as se]
            [silk.core.input.file :as sf]
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
  (let [hick (sf/hick-file v)
        vtitle (-> (hs/select (hs/tag :title) hick) first :content)
        vbody-attrs (-> (hs/select (hs/tag :body) hick) first :attrs)
        meta (meta-map hick)
        name (.getName v)]
    {:name name
     :path (sp/relativise-> (se/views-path) (.getPath v))
     :nav (:data-sw-nav vbody-attrs)
     :priority (:data-sw-priority vbody-attrs)
     :content
      (->> (sf/hick-file (template-path meta))
           (spec/transform
             (spec/walker #(= (:tag %) :title))
             #(assoc % :content vtitle))
           (spec/transform
             (spec/walker #(= (:tag %) :meta))
             #(add-meta-data % meta))
           (spec/transform
             (spec/walker #(get-in % [:attrs :data-sw-view]))
             #(assoc % :content (hs/select (hs/child (hs/tag :body) hs/any) hick))))}))


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
      (let [r (sp/relativise-> (se/project-data-path) (.getPath p))]
        (assoc w :path r :content
          (spec/transform
            (spec/walker #(get-in % [:attrs :data-sw-source]))
            #(update % :attrs conj [:data-sw-source r])
            (:content w)))))))
