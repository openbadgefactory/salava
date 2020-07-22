(ns salava.extra.spaces.ui.message-tool
  (:require
   [salava.core.i18n :refer [t]]
   [salava.core.ui.helper :refer [path-for]]
   [salava.core.ui.ajax-utils :as ajax]
   [salava.core.ui.modal :as mo]
   [reagent-modals.modals :as m]
   [reagent.core :refer [cursor atom]]
   [reagent.session :as session]))

(defn init-message-tool-settings [space-id state]
  (ajax/GET
   (path-for (str "/obpv1/space/message_tool/settings/" space-id))
   {:handler (fn [data]
               (reset! (cursor state [:message_setting]) data)
               #_(reset! (cursor state [:message_setting :issuers]) (:issuers data))
               #_(reset! (cursor state [:message_setting :messages_]))
               (reset! (cursor state [:message_setting :enabled_issuers])  (mapv :issuer_name (filterv :enabled (:issuers data)))))}))

(defn manage-message-tool [space-id state show]
  (when (and (= "admin" (session/get-in [:user :role] "user")) show) 
   (when (empty? (:message_setting @state)) (init-message-tool-settings  space-id state))
   ;(fn []
   [:div
    [:div.form-group
     [:div.checkbox
      [:label
       [:input {:type "checkbox"
                :on-change #(reset! (cursor state [:message_setting :messages_enabled]) (not @(cursor state [:message_setting :messages_enabled])))
                :checked (pos? @(cursor state [:message_setting :messages_enabled]))}]
       "Can use the messaging tool to send message to badge earners"]]]
    (when @(cursor state [:message_setting :messages_enabled])
      [:div.form-group
       [:span._label (t :admin/Messagesetting)]
       [:div.add-admins-link
        [:a
         {:href "#" :on-click #(mo/open-modal [:space :message_setting] state)}
         (t :admin/Manage-issuer-list)]]
       (when (seq @(cursor state [:message_setting :enabled_issuers]))
        [:div.well.well-sm {:style {:max-height "500px" :overflow "auto" :margin "10px auto"}}
         [:div.col-md-12
          [:p "Messages can be sent to email addresses that have been issued badges by the following issuers "]
          (reduce
           #(conj %1 [:li [:b %2]])
            [:ul]
            @(cursor state [:message_setting :enabled_issuers]))]])])]))
