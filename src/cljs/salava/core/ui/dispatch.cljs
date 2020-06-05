(ns salava.core.ui.dispatch
  (:require [clojure.string :as str]
            [reagent.core :as reagent :refer [atom]]
            [bidi.bidi :as b :include-macros true]
            [pushy.core :as pushy]
            [reagent.session :as session]
            [salava.core.common :as common]
            [salava.core.helper :refer [dump]]
            [salava.registry]
            [salava.translator.ui.routes]
            [salava.core.ui.routes]
            [cljsjs.clipboard]
            [salava.core.ui.helper :refer [current-path]]))

(defn get-ctx []
   (let [core-ctx (aget js/window "salavaCoreCtx")]
     (js->clj (core-ctx) :keywordize-keys true)))

(def ctx (get-ctx))
(session/put! :user (:user ctx))
(session/put! :facebook-app-id (:facebook-app-id ctx))
(session/put! :linkedin-app-id (:linkedin-app-id ctx))
(session/put! :google-app-id (:google-app-id ctx))
(session/put! :flash-message (:flash-message ctx))
(session/put! :site-url (get-in ctx [:site-url]))
(session/put! :site-name (get-in ctx [:site-name]))
(session/put! :share (get-in ctx [:share]))
(session/put! :base-path (get-in ctx [:base-path]))
(session/put! :languages (get-in ctx [:languages]))
(session/put! :i18n-editable (some #(= "translator" %1) (get-in ctx [:plugins :all])))
(session/put! :plugins (get-in ctx [:plugins :all]))
(session/put! :private (:private ctx))
(session/put! :footer (:footer ctx))
(session/put! :factory-url (:factory-url ctx))
(session/put! :show-terms? (:show-terms? ctx))
(session/put! :filter-options (:filter-options ctx))
;;;


(defn resolve-plugin [kind plugin ctx]
  (try
    (let [resolver (apply aget (concat [js/window "salava"] (str/split plugin #"/") ["ui" "routes" kind]))]
      (resolver ctx))
    (catch js/Object _ {})))


(defn collect-routes [plugins ctx]
  (let [route-coll (apply common/deep-merge (map #(resolve-plugin "routes" % ctx) plugins))]
    ["" (common/deep-merge route-coll (resolve-plugin "routes" "core" ctx))]))

(defn collect-modal-routes [plugins ctx]
  (let [route-coll (apply common/deep-merge (map #(resolve-plugin "modalroutes" % ctx) plugins))]
    (common/deep-merge route-coll (resolve-plugin "modalroutes" "core" ctx))))


(defn collect-navi [plugins ctx]
  (let [navi-coll  (apply common/deep-merge (map #(resolve-plugin "navi" % ctx) plugins))]
    (common/deep-merge navi-coll (resolve-plugin "navi" "core" ctx))))


(defn collect-site-navi []
  (let [plugins (get-in ctx [:plugins :all])]
    {:plugins    plugins
     :routes     (collect-routes plugins ctx)
     :navi-items (collect-navi plugins ctx)
     :modal-routes (collect-modal-routes plugins ctx)}))

(def site-navi (collect-site-navi))



;;;

(defonce current-route (atom (b/match-route (:routes site-navi) (current-path))))

(defn set-route! [route]
  (reset! current-route route))

(defn main-view []
  (fn []
    (let [{:keys [handler route-params]} @current-route]
      [ (handler site-navi route-params)])))

(defonce history (pushy/pushy set-route! (partial b/match-route (:routes site-navi))))

(session/put! :history history)

(pushy/start! history)
