(ns silk.core.transform.element
  "Artifact element transformation, for example attribute rewriting for
   page elements.  Initially mimetype for content is HTML5.  Principally
   we are working with a view driven pipeline."
  (:require [clojure.java.io :as io]
            [silk.core.input.env :as se]
            [silk.core.transform.path :as sp]
            [silk.core.common.walk :as sw]))

;; =============================================================================
;; Helper functions
;; =============================================================================

(defn- has-prefix? [uri prefixes] (some #(.startsWith uri %) prefixes))

(defn- valid-asset?
  "is asset relative and of the correct type"
  [asset]
  (let [root-ext #{"css" "less" "js" "png" "gif" "jpg" "html"}
        prefixes #{"http:" "https:" "/" "javascript:" "#" "mailto:" "tel:"}]
    (and
      (or
        (some #{(sp/extension asset)} root-ext)
        (= (.lastIndexOf asset ".") -1))
      (not (has-prefix? asset prefixes)))))

(defn- relativise-attr
  "Relativise an attribute value v using the source of the attributes location
   within a project (view location).
   Mode enables different behaviour across different intended environments."
  [project v p live?]
  (let [vp (.getParent (io/file p))]
    (if vp
      (if (valid-asset? v)
        (let [rel (sp/relativise-> (.getParent (io/file (se/views-path project) p))
                                   (se/views-path project))]
          (str rel "/" v))
        v)
      (if live?
        (if (valid-asset? v) (str "/" v) v)
        v))))

;; =============================================================================
;; Element transformation functions, see namespace comment
;; =============================================================================

(defn relativise-attrs
  "Selects elements for re-writing of attributes.
   Manipulates attributes on elements using payload and mode.
   Payload is a map constructed with :path and :content keys where path
   points to content.
   Mode enables different behaviour across different intended environments."
  [project tag attr payload live?]
  (assoc payload :content (sw/map-content (:content payload)
    (fn
      [h]
      (if-let [v (and (= (:tag h) tag) (get-in h [:attrs attr]))]
        (assoc-in h [:attrs attr] (relativise-attr project v (:path payload) live?))
        h)))))
