(ns silk.core.transform.preprocess
  (:require [me.raynes.laser :as l]
            [me.rossputin.diskops :as do]
            [silk.core.transform.path :as sp])
  (:use [clojure.string :only [capitalize]])
  (:import java.io.File))


(defn- view-data
  "Create .edn data files for each view, one level deep."
  [name path nav priority parsed base]
  (if nav
    (let [data (assoc {} :title nav :path path :priority priority)
          path (str base (do/fs) "views")
          fname (str path (do/fs) (sp/update-extension name "edn"))]
      (.mkdirs (File. path))
      (spit fname (pr-str data)))))

(defn- bookmark-data
  "Create .edn data files foreach fragment id, page jumps, inside a view, one
  level deep."
  [name path nav priority parsed base]
  (let [path (str base (do/fs) "bookmarks" (do/fs) (sp/basename path))
        sections (l/select parsed (l/and
          (l/negate (l/element= "body")) (l/attr? :data-sw-nav)
          (l/or (l/attr? :id) (l/attr? :name))))]
    (doseq [[idx sec] (map-indexed vector sections)]
      (let [id (get-in sec [:attrs :id])
            title (get-in sec [:attrs :data-sw-nav])
            priority (format "%03d" idx)
            data (assoc {} :title title :path id :priority priority)
            fname (str path (do/fs) priority "-" id ".edn")]
        (.mkdirs (File. path))
        (spit fname (pr-str data))))))

; (defn- furniture-sitemap
;   "Generate a site wide physical sitemap."
;   [])

; (defn- cache-manifest
;   "Generate a cache manifest for the site."
;   [])

(defn preprocess->
  "Generates navigation data for menu components."
  [t]
  (let [base (str (do/pwd) (do/fs) "data" (do/fs) ".nav")]
    (doseq [{:keys [name path nav priority content]} t]
      (view-data name path nav priority (l/parse content) base)
      (bookmark-data name path nav priority (l/parse content) base))))
