(ns silk.input.file
  "File input functions including template and component."
  (:require [clojure.java.io :refer [file]]
            [silk.input.env :as se]))

(defn template 
  "Return a Silk template from the silk template directory given a filename f.
   A Silk template is the result of a Silk spin process."
  [f] 
  (file (str se/templates-path f)))

(defn component
  "Return a Silk component from the silk components directory given a filename f.
  A Silk component is a raw Silk component."
  [f]
  (file (str se/components-path f)))
