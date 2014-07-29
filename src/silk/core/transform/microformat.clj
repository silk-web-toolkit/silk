(ns silk.core.transform.microformat
  (:require [me.raynes.laser :as l]
            [me.rossputin.diskops :as do]
            [silk.core.input.env :as se]
            [silk.core.input.file :as sf]
            [silk.core.transform.path :as sp])
  (:use [clojure.string :only [capitalize]])
  (:import java.io.File))

(defn- el [n e] (l/select n (l/element= e)))

(defn- attr-value[el attr] (get (get (first el) :attrs) attr))

(defn- text [s] (first (:content (first s))))

(defn- fname->title
  "Turns a file name into a UI ready label."
  [n]
  (capitalize (sp/basename n)))

(defn- menu!
  "Generate a simple menu nav component.
   One level deep.
   Designer of component markup may opt to add microformats or other
   semantic markup."
  [views]
  (let [filtered (filter
                   (fn
                     [{:keys [name parsed]}]
                     (not-empty (l/select parsed (l/class= "u-url"))))
                   views)]
    (doseq [{:keys [name path parsed]} filtered]
      (let [title (or (text (el parsed "title")) (fname->title name))
            rel (sp/relativise-> (.getParent (File. path)) se/views-path)
            calc-path (if (= rel ".") name (str rel "/" name))
            body (el parsed "body")
            priority (attr-value body :data-sw-priority)
            data (assoc {} :title title :path calc-path :priority priority)
            menu-path  (str (do/pwd) "/data" "/.menu")]
        (.mkdirs (File. menu-path))
        (spit (str menu-path "/" name) (pr-str data))))
      ))

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
