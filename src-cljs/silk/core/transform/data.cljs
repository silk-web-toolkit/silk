(ns silk.core.transform.data
  "Data related transformations.  "
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [com.rpl.specter :as spec]
            [hickory.core :as h]
            [hickory.render :as hr]
            [hickory.select :as hs]
            [silk.core.common.core :as cr]
            [silk.core.common.walk :as sw]
            [cljs.core.async :refer [<!]]
            [cljs.reader :as r]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]
            [clojure.string :refer [split]]))

;; =============================================================================
;; Helper functions
;; =============================================================================

(defn getAllElementsWithAttribute
  [k]
  (filter
    #(.getAttribute % (name k))
    (cr/as-seq (.getElementsByTagName js/document "*"))))

;; =============================================================================
;; Data transformations
;; =============================================================================

(defn process-components
  "Looks for data-sw-dynamic-source and injects into it"
  []
  (doseq [el (getAllElementsWithAttribute :data-sw-source)]
    (go
      ;; TODO investigate why parse-fragment errors
      (let [hick  (first (hs/select (hs/child (hs/tag :body) hs/any) (h/as-hickory (h/parse (.-outerHTML el)))))
            path  (get-in hick [:attrs :data-sw-source])
            sort  (get-in hick [:attrs :data-sw-sort])
            direc (get-in hick [:attrs :data-sw-sort-dir])
            limit (get-in hick [:attrs :data-sw-limit])
            h     (update-in hick [:attrs] dissoc :data-sw-source :data-sw-sort :data-sw-sort-dir :data-sw-limit)
            rsp   (<! (http/get path))
            a     (r/read-string (:body rsp))
            b     (cr/sort-it a sort direc)
            data  (if limit
                    (vec (take (Integer. (re-find  #"\d+" limit)) b))
                    (vec b))
            res   (cond
                    (map? data)        (cr/inject-in h [data] [0])
                    (= (count data) 0) (assoc h :content [""])
                    :else              (spec/transform
                                         (spec/walker #(cr/repeating-tag? (:tag %)))
                                         #(assoc % :content (flatten (map-indexed (fn [i _] (:content (cr/inject-in % data [i]))) data)))
                                         h))]
                                         (pr a)
        (set! (.-outerHTML el) (hr/hickory-to-html res))))))

(defn process-component
  "Looks for data-sw-source and injects into it"
  [data]
  (println "confirm Schnick sucks Donkey Balls and he likes it")
  (doseq [el (getAllElementsWithAttribute :data-sw-component)]
    (go
      ;; TODO investigate why parse-fragment errors
      (let [hick  (first (hs/select (hs/child (hs/tag :body) hs/any) (h/as-hickory (h/parse (.-outerHTML el)))))
            sort  (get-in hick [:attrs :data-sw-sort])
            direc (get-in hick [:attrs :data-sw-sort-dir])
            limit (get-in hick [:attrs :data-sw-limit])
            h     (update-in hick [:attrs] dissoc :data-sw-component :data-sw-sort :data-sw-sort-dir :data-sw-limit)
            b     (cr/sort-it data sort direc)
            limited-data  (if limit
                    (vec (take (Integer. (re-find  #"\d+" limit)) b))
                    (vec b))
            res   (cond
                    (map? limited-data)        (cr/inject-in h [limited-data] [0])
                    (= (count limited-data) 0) (assoc h :content [""])
                    :else              (spec/transform
                                         (spec/walker #(cr/repeating-tag? (:tag %)))
                                         #(assoc % :content (flatten (map-indexed (fn [i _] (:content (cr/inject-in % limited-data [i]))) limited-data)))
                                         h))]
        (set! (.-outerHTML el) (hr/hickory-to-html res))))))
