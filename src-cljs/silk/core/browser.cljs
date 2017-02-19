(ns silk.core.browser
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [com.rpl.specter :as spec]
            [hickory.core :as h]
            [hickory.render :as hr]
            [hickory.select :as hs]
            [silk.core.common.core :as cr]
            [cljs.core.async :refer [<!]]
            [cljs.reader :as r]
            [cljs-http.client :as http]))

(defn- hickorify [template]
  (first (hs/select (hs/child (hs/tag :body) hs/any) (h/as-hickory (h/parse (.-outerHTML template))))))

(defn- elementById [tpl id] (.getElementById tpl (name id)))

(defn- elements [tpl] (.getElementsByTagName tpl "*"))

(defn- elementsWithAttribute
  [tpl k]
  (filter #(.getAttribute % (name k)) (cr/as-seq (elements tpl))))

(defn spin-component-with-data
  "Splices data with component with given id"
  [tpl id data]
  (let [template (elementById tpl id)
        hick (hickorify template)
        res (cr/process-component-with-data "" hick data)]
    (set! (.-outerHTML template) (hr/hickory-to-html res))))

(defn spin-components
  "Looks for data-sw-source and injects into it"
  [tpl]
  (doseq [el (elementsWithAttribute tpl :data-sw-source)]
    (go
      (let [hick (hickorify el)
            path (get-in hick [:attrs :data-sw-source])
            rsp  (<! (http/get path))
            data (r/read-string (:body rsp))
            res  (cr/process-component-with-data "" hick data)]
        (set! (.-outerHTML el) (hr/hickory-to-html res))))))
