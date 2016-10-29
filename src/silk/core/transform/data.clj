(ns silk.core.transform.data
  "Data related transformations.  "
  (:require [hickory.core :as h]
            [hickory.select :as hs]
            [com.rpl.specter :as spec]
            [clojure.java.io :refer [file]]
            [clojure.edn :as edn]
            ; [clojure.walk :as walk]
            ; [me.raynes.laser :as l]
            ; [silk.core.input.ast :as ds]
            [silk.core.input.env :as se]
            ; [silk.core.input.data :as dt]
            [silk.core.input.file :as sf]
            ; [silk.core.transform.ast :as tx]
            [silk.core.transform.coerce :as sc]
            [silk.core.transform.path :as sp])
  (:use [clojure.string :only [split]]
        [clojure.set    :only [rename-keys]]))

;; =============================================================================
;; Helper functions
;; =============================================================================

(defn- sort->
  "Default to descending sort."
  [data sort dir]
  (let [ascending (sort-by (keyword sort) data)]
    (if-let [d dir]
      (if (= d "ascending") ascending (reverse ascending))
      ascending)))
;
; (defn- get-component-datasource
;   [{source :data-sw-source limit :data-sw-limit sort :data-sw-sort
;     dir :data-sw-sort-dir parent :data-sw-parent}]
;   (if-let [src (if parent (str "protean-api/" parent "/" source) source)]
;     (let [res (sf/data src)
;           data (sf/get-data-meta res)
;           sorted (if-let [srt sort] (sort-> data srt dir) data)]
;       (if-let [lim limit] (take (sc/int-> lim) sorted) sorted))
;     '()))
;
; ;; fatigued, will abstract this out later :-(
; (defn- get-component-datasource-tree
;   [data-params]
;   (if-let [source (:data-sw-source data-params)]
;     (let [res (sf/data source)]
;       (sf/get-data-meta-tree res))
;     {}))
;
; (defn- transcend-dir
;   [datum]
;   (let [rl {:type :element :tag :li :attrs {:class "folder"} :content [(:name datum)]}
;         cont (if (every? #(= (:tag %) :li) (:content datum))
;                [(update-in rl [:content] into [{:type :element :tag :ul :content (:content datum)}])]
;                [(update-in rl [:content] into (:content datum))])]
;     (-> datum
;         (assoc :type :element :tag :ul :content cont))))
;
; (defn- transcend-file
;   [datum]
;   (let [rel (sp/relativise-> (se/project-data-path) (:path datum))
;         conv-p (sp/update-extension rel "html")
;         cont {:type :element :tag :a :attrs {:href conv-p} :content (:content datum)}
;         mod (assoc datum :type :element :tag :li :content [cont])]
;     mod))
;
; (defn- eval-element
;   [datum]
;   (cond
;    (= (:node-type datum) :directory) (transcend-dir datum)
;    (= (:node-type datum) :file) (transcend-file datum)
;     :else datum))
;
;
; (defn- prepare-keys [attrs]
;   (let [sk (select-keys attrs (ds/get-component-attribs))]
;     (if-let [p (:parent attrs)]
;       (assoc (assoc sk :data-sw-parent p) :data-sw-source (:data-sw-source attrs))
;       sk)))
;

; data-sw-* add as attriute *
; data-sw-content add as content
; use / to access path e.g. data-sw-content "item/name"
; li ol and tr should be treated as loops

(defn- repeating-tag? [t] (.contains [:ul :ol :tbody] t))

(defn- silk-attr? [k] (re-find #"data-sw-(\S+)" (name k)))

(defn- get-data [d v] (str (get d (keyword (last (split v #"\."))))))

(defn- inject-text
  [hick d]
  (if-let [v (get-in hick [:attrs :data-sw-text])]
    (assoc hick :content [(get-data d v)])
    hick))

(defn- inject-attr
  [hick d]
  (assoc hick :attrs (into (sorted-map)
    (for [[k v] (:attrs hick)]
      (if-let [k2 (keyword (last (silk-attr? k)))]
        {k2 (get-data d v)}
        {k v})))))

(defn- data-level
  "Data level based on deepest data-sw-* value .e.g items.childs"
  [hick data]
  (let [selects (spec/select (spec/walker
                  #(and (not (repeating-tag? (:tag %)))
                        (some (fn [k] (silk-attr? (first k))) (:attrs %))))
                  hick)
        at-vals (flatten (map #(vals (:attrs %)) selects))
        deepest (last (sort-by #(count (re-seq #"\." %)) at-vals))]
    (seq (map #(keyword %) (drop-last (split deepest #"\."))))))

; (defn- inject-single
;   [hick data]
;   (spec/transform (spec/walker #(and (not (repeating-tag? (:tag %)))
;                                      (some (fn [k] (silk-attr? (first k))) (:attrs %))))
;     (fn [h]
;       (-> h
;           (inject-text data)
;           (update-in [:attrs] dissoc :data-sw-text)
;           (inject-attr data)))
;     hick))


; (defn- walk-content
;   [hick]
;   (if (vector? hick)
;     (doall (for [h hick] (walk-content h)))
;     (do
;       ; (prn hick)
;       ; (println "--------------------------")
;       (if-let [h (recur ]
;         (recur h)
;         hick))))


; (defn- walk-content
;   [hick]
;   (cond
;     (vector?  hick) (do (doseq [h hick] (walk-content h)) hick)
;     (:content hick) (walk-content (:content hick))
;     :else     hick))


; (defn- walk-content
;   [hick]
;   (cond
;     (vector? hick)  (doall (for [h hick] (walk-content h)))
;     (:content hick) (do
;                       (prn (dissoc hick :content))
;                       (recur (:content hick)))
;     :else           (prn hick)))


; (def update-last-vec
;   [hick path tag]
;   (assoc-in hick [:content 0 :content 1 :content 3] {:tag :looooooooooooooool})
;   (map)
;   (update-in l p conj (assoc tag :content []))


(defn- keys-with-last-index
  [hick keys]
  (loop [k keys h hick indexes []]
    (if-let [fk (first k)]
      (recur (next k) (last (fk h)) (into indexes [fk (dec (count (fk h)))]))
      (vec (drop-last indexes)))))

(defn- walk-content
  [hick]
  (loop [h hick hl nil n nil nl nil r-hick nil]
    (if (or (vector? h) (seq? h))
      (if-let [c (:content (first h))]
        (do
          (prn (keys-with-last-index r-hick hl) "---1---" (assoc (first h) :content []))
          (recur
            c
            (conj hl :content)
            (concat (next h) n)
            (concat (for [i (next h)] hl) nl)
            (update-in r-hick (keys-with-last-index r-hick hl) conj (assoc (first h) :content []))))
        (do
          (prn (keys-with-last-index r-hick hl) "---2---" (first h))
          (recur
            (next h)
            hl
            n
            nl
            (update-in r-hick (keys-with-last-index r-hick hl) conj (first h)))))
      (if-let [c (:content h)]
        (do
          (prn hl "---3---" (assoc h :content []))
          (recur
            c
            [:content]
            n
            nl
            (assoc h :content [])))
        (if n
          (do
            (prn (keys-with-last-index r-hick (first nl)) "---4---" (first n))
            (recur
              nil
              nil
              (next n)
              (next nl)
              (update-in r-hick (keys-with-last-index r-hick (first nl)) conj (first n))))
          r-hick)))))

; (defn- walk-content
;   [hick]
;   (loop [h hick l hick n hick p []]
;     (if (or (vector? h) (seq? h))
;       (if-let [c (:content (first h))]
;         (do (prn "---1---" l p) (recur c        (update-in l p conj (assoc (first h) :content [])) n (conj p 0 :content)))
;         (do (prn "---2---" l p) (recur (next h) (update-in l p conj (first h))                     n (update p (- (count p) 2) inc))))
;       (if-let [c (:content h)]
;         (do (prn "---3---" l  ) (recur c        (assoc l :content []) n [:content]))
;         (do (prn "returns" l  ) n)))))


(defn- inject-list
  [hick data]
  ; (println "------- expected ----------")
  ; (prn hick)
  (println "-----------------")
  (prn (walk-content hick))
  (walk-content hick))


  ; (spec/transform (spec/walker #(:content %))
  ;   (fn [h]
  ;     (prn h)
  ;     h
  ;   )
  ;   ;   (assoc h :content (flatten (map #(:content %)
  ;   ;     (for [d data]
  ;   ;       (spec/transform (spec/walker #(:content %))
  ;   ;         (fn [h2]
  ;   ;           (prn (= h2 h))
  ;   ;           (println "--------------" (get-in h2 [:attrs :data-sw-text]))
  ;   ;           ; (when-let [dl (data-level h d)] (inject-list h (get-in d dl)))
  ;   ;           (-> h2
  ;   ;               (inject-text d)
  ;   ;               (update-in [:attrs] dissoc :data-sw-text)
  ;   ;               (inject-attr d)))
  ;   ;         h))))))
  ;   hick))

(defn- source
  [hick]
  (let [src    (:data-sw-source (:attrs hick))
        param  (:data-sw-sort (:attrs hick))
        direc  (:data-sw-sort-dir (:attrs hick))
        edn    (edn/read-string (slurp (sf/data src)))
        sorted (if-let [p param] (sort-> edn p direc) edn)
        limit  (if-let [s (:data-sw-limit (:attrs hick))] (sc/parse-int s) (count sorted))
        data   (take limit sorted)]
   (-> hick
       (update-in [:attrs] dissoc :data-sw-source :data-sw-limit :data-sw-sort :data-sw-sort-dir)
       (inject-list data))))
      ;  (inject-single (first data)))))

;; =============================================================================
;; Data transformations
;; =============================================================================

(defn process-data
  "Looks for data-sw-source and injects into it"
  [hick]
  (spec/transform (spec/walker #(get-in % [:attrs :data-sw-source])) #(source %) hick))
