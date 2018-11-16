(ns salava.metabadge.ui.my
  (:require [salava.core.ui.layout :as layout]
            [salava.core.ui.ajax-utils :as ajax]
            [reagent.core :refer [atom]]
            [salava.core.ui.helper :refer [path-for]]
            [reagent-modals.modals :as m]
            [salava.core.i18n :as i18n :refer [t]]
            ))

(defn init-data [state]
  (ajax/GET
    (path-for "/obpv1/metabadge" true)
    {:handler (fn [data]
                (swap! state assoc :metabadges data
                                    :initializing false))}))
(defn content [state]
  [:div {:id "my-badges"}
   [m/modal-window]]
     (if (:initializing @state)
     [:div.ajax-message
      [:i {:class "fa fa-cog fa-spin fa-2x "}]
      [:span (str (t :core/Loading) "...")]]
     [:div
      "TEST"
      #_[badge-grid-form state]
      #_(cond
        (not-activated?) (not-activated-banner)
        ;(empty? (:badges @state)) [no-badges-text]
        :else [badge-grid state])]
     )
  )

(defn handler [site-navi]
  (let [state (atom {:initializing true})]
    (init-data state)
    (fn []
      (layout/default site-navi (content state)))))
