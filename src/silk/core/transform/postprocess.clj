(ns silk.core.transform.postprocess
  ""
  (:require [me.raynes.laser :as l]
            [silk.core.transform.path :as sp]
            [clojure.string :as str]))

;; =============================================================================
;; Helper functions
;; =============================================================================

(defn- silk-text-node
  [n]
  (l/select (l/parse n) (l/attr? :data-sw-content)))

(defn- condensed
  [text]
  (str/replace text #"\s+" " "))

;; =============================================================================
;; Post process transformation functions.
;; =============================================================================

(defn get-text->
  "Gets the sites contents."
  [items]
  (let [f (filter #(not-empty (silk-text-node (:content %))) (distinct items))]
    (if (.isEmpty f)
      nil
      {:pages
        (for [{:keys [nav path content]} f]
          { :title (if (empty? nav) (sp/basename path) nav)
            :text (condensed (l/text (first (silk-text-node content))))
            :tags ""
            :loc (sp/update-extension path "html")})})))
