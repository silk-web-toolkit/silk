(ns silk.core.transform.element
  "Artifact element transformation, for example attribute rewriting for
   page elements.  Initially mimetype for content is HTML5.  Principally
   we are working with a view driven pipeline."
  (:require [me.raynes.laser :as l]
            [pathetic.core :as path]
            [silk.core.input.env :as se]
            [silk.core.transform.path :as sp])
  (import java.io.File))

;; =============================================================================
;; Element transformation functions, see namespace comment
;; =============================================================================

(def ROOT-EXT #{"css" "less" "js" "png" "gif" "jpg"})

(defn- external-uri?
  "determine if asset is pointed to by external uri"
  [asset]
  (some #{"http:" "https:" "mailto:"} (path/parse-path asset)))

(defn- valid-asset?
  "is asset relative and of the correct type"
  [asset]
  (and
    (or
      (some #{(sp/extension asset)} ROOT-EXT)
      (= (.lastIndexOf asset ".") -1))
    (not (.startsWith asset "/"))
    (not (.startsWith asset "javascript:"))
    (not (external-uri? asset))))


(defn- relativise-attr
  "Relativise an attribute value v using the source of the attributes location
   within a project (view location).
   Mode enables different behaviour across different intended environments."
  [v p m]
  (let [vp (.getParent (File. p))]
    (if vp
      (if (valid-asset? v)
        (let [rel (sp/relativise-> (.getParent (File. se/views-path p))
                   se/views-path)]
          (str rel "/" v))
        v)
      (if (= m "live")
        (if (valid-asset? v) (str "/" v) v)
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
