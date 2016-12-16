(ns silk.core.transform.view
  "View related transformations.
   Principally view driven."
  (:require [hickory.select :as hs]
            [com.rpl.specter :as spec]
            [silk.core.input.env :as se]
            [silk.core.input.file :as sf]
            [silk.core.transform.path :as sp]
            [silk.core.transform.walk :as sw])
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
  (let [vhick (sf/hick-file v)
        vtitle (-> (hs/select (hs/tag :title) vhick) first :content)
        bhick (hs/select (hs/child (hs/tag :body) hs/any) vhick)
        vbody-attrs (-> (hs/select (hs/tag :body) vhick) first :attrs)
        meta (meta-map vhick)
        name (.getName v)]
    {:name name
     :path (sp/relativise-> (se/views-path) (.getPath v))
     :nav (:data-sw-nav vbody-attrs)
     :priority (:data-sw-priority vbody-attrs)
     :content (sw/map-content
                (sf/hick-file (template-path meta))
                #(cond
                  (and vtitle (= (:tag %) :title))  (assoc % :content vtitle)
                  (= (:tag %) :meta)                (add-meta-data % meta)
                  (get-in % [:attrs :data-sw-view]) (assoc % :content bhick)
                  :else                             %))}))

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
            (spec/walker #(get-in % [:attrs :data-sw-view]))
            #(update % :attrs merge {:data-sw-source r})
            (:content w)))))))
