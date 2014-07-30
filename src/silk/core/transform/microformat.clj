(ns silk.core.transform.microformat
  (:require [me.raynes.laser :as l]
            [me.rossputin.diskops :as do]
            [silk.core.input.env :as se]
            [silk.core.input.file :as sf]
            [silk.core.transform.path :as sp])
  (:use [clojure.string :only [capitalize]])
  (:import java.io.File))

(defn- el [n e] (l/select n (l/element= e)))

(defn- attr-value[el attr] (get-in (first el) [:attrs attr]))

(defn- text [s] (first (:content (first s))))

(defn- view-source
  "Create .edn files for each view, one level deep."
  [name path parsed base]
  (let [rel (sp/relativise-> (.getParent (File. path)) se/views-path)
        calc-path (if (= rel ".") name (str rel "/" name))
        body (el parsed "body")
        title (attr-value body :data-sw-nav)
        priority (attr-value body :data-sw-priority)
        data (assoc {} :title title :path calc-path :priority priority)
        dir (str base (do/fs) "view")
        fname (str dir (do/fs) (sp/update-extension name "edn"))]
    (.mkdirs (File. dir))
    (spit fname (pr-str data))))

(defn- content-source
  "Create .edn files foreach nav section inside view, one level deep."
  [name parsed base]
  (let [dir (str base (do/fs) "content" (do/fs) (sp/basename name))
        sections (l/select parsed (l/and (l/attr? :id) (l/attr? :data-sw-nav)))]
    (doseq [[idx sec] (map-indexed vector sections)]
      (let [id (get-in sec [:attrs :id])
            title (get-in sec [:attrs :data-sw-nav])
            priority (format "%03d" idx)
            data (assoc {} :title title :path id :priority priority)
            fname (str dir (do/fs) priority "-" id ".edn")]
        (.mkdirs (File. dir))
        (spit fname (pr-str data))))))

(defn- menu!
  "Generates navigation data for menu components.
   Designer of component markup may opt to add microformats or other
   semantic markup."
  [views]
  (let [base (str (do/pwd) (do/fs) "data" (do/fs) ".nav")
        filtered (filter
                   (fn
                     [{:keys [name parsed]}]
                     (not-empty (l/select parsed (l/attr? :data-sw-nav))))
                   views)]
    (doseq [{:keys [name path parsed]} filtered]
      (view-source name path parsed base)
      (content-source name parsed base))))

; (defn- furniture-sitemap
;   "Generate a site wide physical sitemap."
;   [])

; (defn- cache-manifest
;   "Generate a cache manifest for the site."
;   [])

(defn microformat-edn-> []
  (let [view-files (sf/get-views)
        views (map
                #(hash-map
                  :name (.getName %)
                  :path (.getPath %)
                  :parsed (l/parse %))
                view-files)]
    (menu! views)
    views))
