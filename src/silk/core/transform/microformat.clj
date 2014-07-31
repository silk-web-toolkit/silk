(ns silk.core.transform.microformat
  (:require [me.raynes.laser :as l]
            [me.rossputin.diskops :as do]
            [silk.core.transform.path :as sp])
  (:use [clojure.string :only [capitalize]])
  (:import java.io.File))


(defn- view-source
  "Create .edn files for each view, one level deep."
  [name path nav priority parsed base]
  (if nav
    (let [data (assoc {} :title nav :path path :priority priority)
          path (str base (do/fs) "view")
          fname (str path (do/fs) (sp/update-extension name "edn"))]
      (.mkdirs (File. path))
      (spit fname (pr-str data)))))

(defn- content-source
  "Create .edn files foreach nav section inside view, one level deep."
  [name path nav priority parsed base]
  (let [path (str base (do/fs) "content" (do/fs) (sp/basename path))
        sections (l/select parsed (l/and
          (l/negate (l/element= "body")) (l/attr? :data-sw-nav) (l/attr? :id)))]
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

(defn microformat-edn->
  "Generates navigation data for menu components.
   Designer of component markup may opt to add microformats or other
   semantic markup."
  [t]
  (let [base (str (do/pwd) (do/fs) "data" (do/fs) ".nav")]
    (doseq [{:keys [name path nav priority content]} t]
      (view-source name path nav priority (l/parse content) base)
      (content-source name path nav priority (l/parse content) base))))
