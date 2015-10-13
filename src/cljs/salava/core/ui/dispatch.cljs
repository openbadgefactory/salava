(ns salava.core.ui.dispatch
  (:require [reagent.core :as reagent :refer [atom]]
            [bidi.bidi :as b :include-macros true]
            [pushy.core :as pushy]
            [clojure.string :as str]
            [salava.core.common :as common]
            [salava.core.ui.layout :as layout]
            ))


(defn get-token []
  (let [uri js/window.location.pathname]
    (str (if (and (not (= "/" uri)) (.endsWith uri "/"))
           (subs uri 0 (dec (count uri)))
           uri)
         js/window.location.search)))


(defonce current-path (atom (get-token)))

(defonce history (pushy/pushy #(reset! current-path (get-token)) (constantly true)))

(pushy/start! history)


;;;


(defn resolve-plugin [kind plugin ctx]
  (let [resolver (aget js/window "salava" plugin "ui" "routes" kind)]
    (resolver ctx)))


(defn collect-routes [plugins ctx]
  (let [route-coll (apply common/deep-merge (map #(resolve-plugin "routes" % ctx) plugins))]
    ["" (common/deep-merge route-coll (resolve-plugin "routes" "core" ctx))]))


(defn collect-navi [plugins ctx]
  (let [navi-coll  (apply common/deep-merge (map #(resolve-plugin "navi" % ctx) plugins))]
    (common/deep-merge navi-coll (resolve-plugin "navi" "core" ctx))))

(defn collect-headings [plugins ctx]
  (let [heading-coll (apply common/deep-merge (map #(resolve-plugin "heading" % ctx) plugins))]
    (common/deep-merge heading-coll (resolve-plugin "heading" "core" ctx))))


;;;


(defn navi-parent [path]
  (let [sections (str/split path #"/")]
      (second sections)))

(defn filtered-navi-list [navi key-list]
  (let [map-fn (fn [[tr nv]]
                 (assoc nv :target tr))]
    (sort-by :weight (map map-fn (select-keys navi key-list)))))

(defn top-navi-list [navi]
  (let [key-list (filter #(<= (count (str/split % #"/")) 2) (keys navi))]
    (filtered-navi-list navi key-list)))

(defn sub-navi-list [parent navi]
  (let [parent-filter #(and (not= (str "/" parent "/") %) (= parent (navi-parent %)))
        key-list (filter parent-filter (keys navi))]
    (when parent
      (js/console.log (pr-str key-list))
      (filtered-navi-list navi key-list))))

(defn current-heading [parent headings]
  (str (get headings parent)))
;;;

(defn main-view [ctx]
  (let [plugins    (get-in ctx [:plugins :all])
        routes     (collect-routes plugins ctx)
        navi-items (collect-navi plugins ctx)
        headings   (collect-headings plugins ctx)]
  (fn []
    (let [{:keys [handler route-params]} (b/match-route routes @current-path)
          top-navi (atom (top-navi-list navi-items))
          sub-navi (atom (sub-navi-list (navi-parent @current-path) navi-items))
          heading (current-heading @current-path headings)
          content  (handler route-params)]
      (layout/default top-navi sub-navi heading content)))))
