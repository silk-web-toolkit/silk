(ns silk.input.file
  "File input functions including template and component."
  (:require [clojure.java.io :refer [file]]
            [silk.input.env :as se]))

(defn runtime-template 
  "Return a runtime Silk template from the runtime silk template directory given a 
   filename f.
   A runtime Silk template is the result of a Silk spin process."
  [f] 
  (file (str se/runtime-templates-path f)))

(defn template
  "Return a Silk template template from the silk template directory given a filename f.
   A Silk template is raw markup."
  [f]
  (file (str se/templates-path f)))

(defn component
  "Return a Silk component from the silk components directory given a filename f.
  A Silk component is raw markup."
  [f]
  (file (str se/components-path f)))
