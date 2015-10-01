(ns salava.core.ui.main
  (:require [reagent.core :as reagent]
            [salava.resolver]
            [salava.core.ui.dispatch :as d]))

(defn get-ctx []
   (let [core-ctx (aget js/window "salavaCoreCtx")]
     (js->clj (core-ctx) :keywordize-keys true)))

(defn init! []
  (reagent/render [(d/main-view (get-ctx))] (js/document.getElementById "app")))

(init!)
