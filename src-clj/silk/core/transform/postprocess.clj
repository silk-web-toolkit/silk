(ns silk.core.transform.postprocess
  ""
  (:require [com.rpl.specter :as spec]
            [hickory.core :as h]
            [silk.core.transform.path :as sp]
            [clojure.string :as string]))

;; =============================================================================
;; Helper functions
;; =============================================================================

(defn- silk-text-nodes
  [html]
  (spec/select
    (spec/walker #(get-in % [:attrs :data-sw-content]))
    (h/as-hickory (h/parse html))))

(defn text
  "Returns the text value of a node and its contents."
  [node]
  (cond
   (string? node) node
   (and (map? node)
        (not= (:type node) :comment)) (string/join (map text (:content node)))
   :else ""))

(defn- get-title
  "Get the title from the markup or filename and add section bookmark"
  [page-title path node]
  (let [t (if (not (empty? page-title)) page-title (sp/basename path))
        s (get-in node [:attrs :data-sw-nav])]
    (if (not (empty? s)) (str t " / " s) t)))

(defn- condensed
  [text]
  (string/replace text #"\s+" " "))

(defn- escaped
  [text]
  (org.apache.commons.lang3.StringEscapeUtils/escapeHtml4 text))

(defn- get-location
  [path node]
  (let [id (get-in node [:attrs :id])]
    (str
      (sp/update-extension path "html")
      (when (not (empty? id)) (str "#" id)))))

;; =============================================================================
;; Post process transformation functions.
;; =============================================================================

(defn get-text->
  "Gets the sites contents."
  [items]
  (when-let [f (seq (filter #(not-empty (silk-text-nodes (:content %))) items))]
    {:pages
      (flatten
        (for [{:keys [nav path content]} f]
          (for [node (silk-text-nodes content)]
            {:title (get-title nav path node)
             :text (escaped (condensed (text node)))
             :tags ""
             :loc (get-location path node)})))}))
