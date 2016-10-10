; (ns silk.core.transform.postprocess
;   ""
;   (:require [me.raynes.laser :as l]
;             [silk.core.transform.path :as sp]
;             [clojure.string :as str]))
;
; ;; =============================================================================
; ;; Helper functions
; ;; =============================================================================
;
; (defn- silk-text-node
;   [n]
;   (l/select (l/parse n) (l/attr? :data-sw-content)))
;
; (defn- get-title
;   "Get the title from the markup or filename and add section bookmark"
;   [page-title path node]
;   (let [t (if (not (empty? page-title)) page-title (sp/basename path))
;         s (get-in node [:attrs :data-sw-nav])]
;     (if (not (empty? s)) (str t " / " s) t)))
;
; (defn- condensed
;   [text]
;   (str/replace text #"\s+" " "))
;
; (defn- escaped
;   [text]
;   (org.apache.commons.lang3.StringEscapeUtils/escapeHtml4 text))
;
; (defn- get-location
;   [path node]
;   (let [id (get-in node [:attrs :id])]
;     (str
;       (sp/update-extension path "html")
;       (when (not (empty? id)) (str "#" id)))))
;
; ;; =============================================================================
; ;; Post process transformation functions.
; ;; =============================================================================
;
; (defn get-text->
;   "Gets the sites contents."
;   [items]
;   (let [f (filter #(not-empty (silk-text-node (:content %))) (distinct items))]
;     (if (.isEmpty f)
;       nil
;       {:pages
;         (flatten
;           (for [{:keys [nav path content]} f]
;             (for [node (silk-text-node content)]
;               { :title (get-title nav path node)
;                 :text (escaped (condensed (l/text node)))
;                 :tags ""
;                 :loc (get-location path node)})))})))
