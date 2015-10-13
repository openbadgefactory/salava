(ns salava.core.ui.main
  (:require [reagent.core :as reagent]
            [reagent.session :as session]
            [salava.resolver]
            [salava.core.ui.dispatch :as d]))

(session/put! :lang :en)

(defn get-ctx []
   (let [core-ctx (aget js/window "salavaCoreCtx")]
     (js->clj (core-ctx) :keywordize-keys true)))

(defn init! []
  (reagent/render [(d/main-view (get-ctx))] (js/document.getElementById "app")))

(init!)
