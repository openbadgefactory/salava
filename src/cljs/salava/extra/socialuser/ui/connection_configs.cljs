(ns salava.extra.socialuser.ui.connection-configs
  (:require [reagent.core :refer [atom cursor]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.helper :refer [dump]]
            [salava.core.ui.helper :refer [path-for]]
            [salava.core.i18n :refer [t]])
  )


(defn init-data [state]
  (ajax/GET
   (path-for (str "/obpv1/socialuser/user-connection-config" ))
   {:handler (fn [data]
               (reset! state data))}))




(defn change-status [status]
  (ajax/POST
   (path-for (str "/obpv1/socialuser/user-connection-config/" status))
   {:response-format :json
    :keywords?       true          
    :handler         (fn [data]
                       (do
                         ))
    :error-handler   (fn [{:keys [status status-text]}]
                       (.log js/console (str status " " status-text)))}))

(defn content [state]
  [:div.form-group
   [:label {:for   "input-email-notifications"
            :class "col-md-3"}
    (t :social/Userfollowingconfig)]
   [:div.col-md-9
    [:select {:id        "input-country"
              :class     "form-control"
              :value     @state
              :on-change #(do
                            (reset! state (.-target.value %))
                            (change-status (.-target.value %)))}
     
     [:option {:value "accepted"
               :key   "accepted"} (t :social/Configaccepting)]
     [:option {:value "pending"
               :key   "pending"} (t :social/Configpending)]]
    #_(case @state
      "pending"  [:div "PENDING TEKSTI"]
      "accepted" [:div "ACCEPTED teksti"]
      "")]])



(defn handler []
  (let [state (atom "pending")]
    (init-data state)
    (fn []
      (content state))))
