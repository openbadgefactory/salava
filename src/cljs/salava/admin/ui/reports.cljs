(ns salava.admin.ui.reports
  (:require [reagent.core :refer [atom cursor]]
            [reagent.session :as session]
            [reagent-modals.modals :as m]
            [clojure.string :refer [trim]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.layout :as layout]
            [salava.core.ui.grid :as g]
            [salava.core.ui.helper :refer [path-for]]
            [salava.core.i18n :refer [t]]
            [salava.core.helper :refer [dump]]))



(defn content [state]
  [:div
   [:h1 "MOI JOU JEE"]])

(defn init-state [state]
  (ajax/GET "/obpv1/hello/counter"
            {:handler (fn [data]
                        (let [counter (get data "value" 0)]
                          (reset! state counter)))}))


(defn handler [site-navi]
  (let [state (atom 0)]
    (fn []
      (layout/default site-navi (content state)))))
