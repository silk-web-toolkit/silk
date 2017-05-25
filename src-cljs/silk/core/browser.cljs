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

(defn- elementById [markup id] (.getElementById markup (name id)))

(defn- elements [markup] (.getElementsByTagName markup "*"))

(defn- elementsWithAttribute
  [markup k]
  (filter #(.getAttribute % (name k)) (cr/as-seq (elements markup))))

(defn ^:export spin-by-id
  "Splices markup restricted by given id and data"
  [markup id data]
  (let [template (elementById markup id)
        hick (hickorify template)
        res (cr/splice-hick-with-data hick data)]
    (set! (.-outerHTML template) (hr/hickory-to-html res))))

(defn ^:export spin-by-data-sw-source
  "Splices markup restricted by data-sw-source, data is provided by data-sw-source attribute value"
  [markup]
  (doseq [el (elementsWithAttribute markup :data-sw-source)]
    (go
      (let [hick (hickorify el)
            path (get-in hick [:attrs :data-sw-source])
            rsp  (<! (http/get path))
            data (r/read-string (:body rsp))
            res  (cr/splice-hick-with-data hick data)]
        (set! (.-outerHTML el) (hr/hickory-to-html res))))))
