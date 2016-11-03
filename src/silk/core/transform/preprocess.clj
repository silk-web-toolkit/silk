(ns silk.core.transform.preprocess
  (:require [me.rossputin.diskops :as do]
            [silk.core.input.env :as se]
            [silk.core.transform.path :as sp])
  (:use [clojure.string :only [capitalize]])
  (:import java.io.File))

;; =============================================================================
;; Helper functions
;; =============================================================================

(defn- view-data
  "Create .edn data files for each view, one level deep."
  [name path nav priority content]
  (when nav
    (let [data (assoc {} :title nav :path path :priority priority)
          fname (str se/sw-views-path (do/fs) (sp/update-extension name "edn"))]
      (.mkdirs (File. se/sw-views-path))
      (spit fname (pr-str data)))))

(defn- bookmark-data
  "Create .edn data files foreach fragment id, page jumps, inside a view, one
  level deep."
  [name path nav priority content]
  (let [calc-path (str se/sw-bookmarks-path (do/fs) (sp/basename path))
        sections nil]
        ; sections (l/select content (l/and
        ;                               (l/negate (l/element= "body"))
        ;                               (l/attr? :data-sw-nav)
        ;                               (l/or (l/attr? :id)
        ;                                     (l/attr? :name))))]
    (doseq [[idx sec] (map-indexed vector sections)]
      (let [id (get-in sec [:attrs :id])
            title (get-in sec [:attrs :data-sw-nav])
            priority (format "%03d" idx)
            data (assoc {} :title title :path id :priority priority)
            fname (str calc-path (do/fs) priority "-" id ".edn")]
        (.mkdirs (File. calc-path))
        (spit fname (pr-str data))))))

; (defn- furniture-sitemap-data
;   "Generate a site wide physical sitemap."
;   [])

; (defn- cache-manifest-data
;   "Generate a cache manifest for the site."
;   [])


; ;; =============================================================================
; ;; Preprocess transformation functions.
; ;; =============================================================================

(defn preprocess->
  "Generates navigation data for menu components."
  [payload]
  (doseq [{:keys [name path nav priority content]} payload]
    (view-data name path nav priority content)
    (bookmark-data name path nav priority content)))
