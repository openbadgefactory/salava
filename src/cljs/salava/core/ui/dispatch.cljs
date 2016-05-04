(ns salava.core.ui.dispatch
  (:require [reagent.core :as reagent :refer [atom]]
            [bidi.bidi :as b :include-macros true]
            [pushy.core :as pushy]
            [reagent.session :as session]
            [salava.core.common :as common]
            [salava.core.helper :refer [dump]]
            [salava.registry]
            [salava.translator.ui.routes]
            [salava.core.ui.routes]
            [salava.core.ui.helper :refer [current-path]]))

(defn get-ctx []
   (let [core-ctx (aget js/window "salavaCoreCtx")]
     (js->clj (core-ctx) :keywordize-keys true)))

(def ctx (get-ctx))

(session/put! :user (:user ctx))
(session/put! :facebook-app-id (:facebook-app-id ctx))
(session/put! :linkedin-app-id (:linkedin-app-id ctx))
(session/put! :flash-message (:flash-message ctx))
(session/put! :site-url (get-in ctx [:site-url]))
(session/put! :site-name (get-in ctx [:site-name]))
(session/put! :base-path (get-in ctx [:base-path]))
(session/put! :i18n-editable (some #(= "translator" %1) (get-in ctx [:plugins :all])))
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


(defn collect-site-navi []
  (let [plugins (get-in ctx [:plugins :all])]
    {:plugins    plugins
     :routes     (collect-routes plugins ctx)
     :navi-items (collect-navi plugins ctx)}))

(def site-navi (collect-site-navi))



;;;

(defonce current-route (atom (b/match-route (:routes site-navi) (current-path))))

(defn set-route! [route]
  (reset! current-route route))

(defn main-view []
  (fn []
    (let [{:keys [handler route-params]} @current-route]
      [ (handler site-navi route-params) ])))

(defonce history (pushy/pushy set-route! (partial b/match-route (:routes site-navi))))

(pushy/start! history)

