(ns silk.core.transform.view
  "View related transformations.
   Principally view driven."
  (:require [me.raynes.laser :as l]
            [me.rossputin.diskops :as do]
            [silk.core.input.env :as se]
            [silk.core.input.ast :as ds]
            [silk.core.input.file :as sf]
            [silk.core.transform.ast :as tx]
            [silk.core.transform.path :as sp])
  (:use [clojure.string :only [split]]))

;; =============================================================================
;; Helper functions
;; =============================================================================

(defn- el [n e] (l/select n (l/element= e)))

(defn- el-attr[n e attr] (get-in (first (el n e)) [:attrs attr]))

(defn- get-template [t]
  (if-not (nil? (first t))
                   (sf/template
                    (str (:content (:attrs (first t))) ".html"))
                   (sf/template "default.html")))

(defn- title [t] (if t (first (:content (first t))) nil))

(defn- title! [t] (if t (l/content t) (fn [n] n)))

(defn- view-inject [v]
  (let [parsed-view (l/parse v)
        meta-template (ds/template parsed-view)
        template (get-template meta-template)
        vtitle (title (ds/title parsed-view))
        name (.getName v)]
    {:name name
     :path (sp/relativise-> se/views-path (.getPath v))
     :nav (el-attr parsed-view  "body" :data-sw-nav)
     :priority (el-attr parsed-view  "body" :data-sw-priority)
     :content (l/document
                (l/parse template)
                (l/element= :title) (title! vtitle)
                (l/attr? "data-sw-view")
                  (l/replace (ds/body-content parsed-view))
                (l/element= :body) (tx/write-template-class meta-template)
                (l/element= :body)
                  (l/add-class (str "silk-view-" (first (split name #"\.")))))}))


;; =============================================================================
;; Payload transformation functions, see namespace comment
;; =============================================================================

(defn template-wrap-> []
  (let [views (sf/get-views)]
    (map #(view-inject %) views)))

(defn template-wrap-detail-> [{path :path template :template}]
  (let [wrapped (map #(view-inject %) (take (count path) (repeat template)))]
    (for [p path w wrapped]
      (let [rel-p (sp/relativise-> (str (do/pwd) (do/fs) "data" (do/fs)) (.getPath p))
            data-inj
            (l/document
             (l/parse (:content w))
             (l/attr? :data-sw-component)
             (l/attr :data-sw-source rel-p))]
        (assoc w :path rel-p :content data-inj)))))
