(ns silk.transform.element
  "Artifact element transformation, for example attribute rewriting for
   page elements.  Initially mimetype for content is HTML5.  Principally
   we are working with a view driven pipeline."
   (:require [me.raynes.laser :as l]
             [pathetic.core :as path]
             [silk.input.env :as se]
             [silk.transform.path :as sp])
   (import java.io.File))

;; =============================================================================
;; Element transformation functions, see namespace comment
;; =============================================================================

(def ROOT-EXT #{"css" "js" "png" "gif" "jpg"})

(defn- relativise-attr
  "Relativise an attribute value v using the source of the attributes location
   within a project (view location).
   Mode enables different behaviour across different intended environments."
  [v p m]
  (let [vp (.getParent (File. p))]
    (if-not (nil? vp)
      (let [pv (path/parse-path v)
            parsed-uri (some #{"http:" "https:" "mailto:"} pv)]
        (if-not (nil? parsed-uri)
          v
          (let [rel (sp/relativise->
                     (.getParent (File. se/views-path p))
                     se/views-path)]
            (str rel "/" v))))
      (if (= m "live") 
        (if (some #{(sp/extension v)} ROOT-EXT) (str "/" v) v)
        v))))

(defn relativise-attrs
  "Selects elements for re-writing of attributes.
   Manipulates attributes on elements using payload and mode.
   Payload is a map constructed with :path and :content keys where path
   points to content.
   Mode enables different behaviour across different intended environments."
  [e a p m]
  (let [page (l/parse (:content p))
        a-tx (l/document
                 page
                 (l/and (l/element= e) (l/attr? a))
                   (l/update-attr a relativise-attr (:path p) m))]
    (assoc p :content a-tx)))
