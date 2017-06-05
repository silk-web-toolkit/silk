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

(defn- relativise
  "Relativise a value v using the source of the attributes location
   within a project (view location).
   Mode enables different behaviour across different intended environments."
  [project v f vf live?]
  (if (.getParent f)
    (if (valid-asset? v)
      (str (sp/relativise-> (.getParent vf) (se/views-path project)) "/" v)
      v)
    (if live?
      (if (valid-asset? v) (str "/" v) v)
      v)))

;; =============================================================================
;; Element transformation functions, see namespace comment
;; =============================================================================

(defn relativise-attrs
  "Selects elements for re-writing of attributes.
   Manipulates attributes on elements using payload and mode.
   Payload is a map constructed with :path and :content keys where path
   points to content.
   Mode enables different behaviour across different intended environments."
  [project tags payload live?]
  (let [hick (:content payload)
        f  (io/file (:path payload))
        vf (io/file (se/views-path project) f)
        mc (sw/map-content hick
             (fn [h]
               (let [attr (get tags (:tag h))]
                 (if-let [v (get-in h [:attrs attr])]
                   (assoc-in h [:attrs attr] (relativise project v f vf live?))
                   h))))]
    (assoc payload :content mc)))
