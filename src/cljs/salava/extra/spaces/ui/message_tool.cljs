(ns salava.extra.spaces.ui.message-tool
  (:require
   [salava.core.i18n :refer [t]]
   [salava.core.ui.helper :refer [path-for]]
   [salava.core.ui.ajax-utils :as ajax]
   [salava.core.ui.modal :as mo]
   [reagent-modals.modals :as m]
   [reagent.core :refer [cursor atom]]
   [reagent.session :as session]
   [salava.core.ui.layout :as layout]
   [salava.core.ui.modal :as mo]
   [reagent-modals.modals :as m]
   [salava.user.ui.input :as input]))

(defn init-message-tool-settings [space-id state]
  (ajax/GET
   (path-for (str "/obpv1/space/message_tool/settings/" space-id))
   {:handler (fn [data]
               (reset! (cursor state [:message_setting]) data)
               (reset! (cursor state [:message_setting :enabled_issuers])  (mapv :issuer_name (filterv :enabled (:issuers data)))))}))


(defn manage-message-tool [space-id state show]
  (when (and (= "admin" (session/get-in [:user :role] "user")) show)
   (when (empty? (get-in @state [:message_setting :issuers] [])) (init-message-tool-settings  space-id state))
   ;(fn []
   [:div
    [:div.form-group
     [:div.checkbox
      [:label
       [:input {:type "checkbox"
                :on-change #(reset! (cursor state [:message_setting :messages_enabled]) (not @(cursor state [:message_setting :messages_enabled])))
                :checked (pos? @(cursor state [:message_setting :messages_enabled]))}]
       [:b (t :extra-spaces/Canmessagerecipients)]]]]
    (when @(cursor state [:message_setting :messages_enabled])
      [:div.form-group
       [:span._label (t :extra-spaces/Messagetoolrestrictions)]
       [:div.row
         [:div.col-md-12
          ;[:div.form-group
           [:div.radio
            [:label {:for "enable-all-issuers"}
             [:input {:type      "radio"
                      :id        "enable-all-issuers"
                      :name      "issuerSetting"
                     ; :value @(cursor state [:message_setting :all-issuers-enabled])
                      :on-change #(reset! (cursor state [:message_setting :all_issuers_enabled]) true)
                      :default-checked   (or (pos? @(cursor state [:message_setting :all_issuers_enabled])) @(cursor state [:message_setting :all_issuers_enabled]))}]
             [:b (str (t :extra-spaces/Enableallissuers))]]
            [:div.radio
             [:label {:for "enable-issuers-list"}
              [:input {:type      "radio"
                       :id        "enable-issuers-list"
                       :name      "issuerSetting"
                       :on-change #(do (reset! (cursor state [:message_setting :all_issuers_enabled]) false))
                                       ;(init-message-tool-settings @(cursor state [:space :id]) state))
                       :default-checked   (or (zero? @(cursor state [:message_setting :all_issuers_enabled])) (not @(cursor state [:message_setting :all_issuers_enabled])))}]
              [:b (str (t :extra-spaces/Enablesomeissuers))]]]]]]

       (when (or  (zero? @(cursor state [:message_setting :all_issuers_enabled])) (not @(cursor state [:message_setting :all_issuers_enabled])))
         [:div
          [:div.add-admins-link {:style {:margin "10px auto"}}
           [:a
            {:href "#" :on-click #(mo/open-modal [:space :message_setting] state #_{:shown (fn [] (init-message-tool-settings @(cursor state [:space :id]) state))})}
            [:span [:i.fa.fa-list-alt.fa-lg] " " (t :extra-spaces/Manageissuerlist)]]]
          (when (seq @(cursor state [:message_setting :enabled_issuers]))
           [:div.well.well-sm {:style {:max-height "500px" :overflow "auto" :margin "10px auto"}}
            [:div.col-md-12
             [:p (t :extra-spaces/Issuerlistinfo)]
             (reduce
              #(conj %1 [:li [:b %2]])
               [:ul]
               @(cursor state [:message_setting :enabled_issuers]))]])])])]))

(defn fetch-badge-earners [state]
 (let [badges (map :id @(cursor state [:selected-badges]))]
   (ajax/POST
    (path-for (str "/obpv1/space/message_tool/badge_earners"))
    {:params {:ids badges :received_all @(cursor state [:allbadgesreceived])}
     :handler (fn [data]
                (reset! (cursor state [:emails]) data))})))

(defn- add-or-remove [x coll]
   (if (some #(= x %) @coll)
     (reset! coll (->> @coll (remove #(= x %)) vec))
     (reset! coll (conj @coll x))))

(defn badge-grid-element [badge state]
 (let [{:keys [id badge_name badge_id badge_image issuer_name]} badge]

  [:div {:class "media grid-container" :style {:position "relative"}}
    [:input.pull-right
     {:id (str "checkbox-"id)
      :type "checkbox"
      :default-checked (some #(= (:id %) id) @(cursor state [:selected-badges]))
      :on-change #(add-or-remove badge (cursor state [:selected-badges]))}]
    [:a {:href "#" :on-click #(mo/open-modal [:gallery :badges] {:badge-id badge_id :gallery-id id})
         :title badge_name}
     [:div.media-content
      (if badge_image
        [:div.media-left
         [:img {:src (str "/" badge_image)
                :alt (str (t :badge/Badge) " " badge_name)}]])
      [:div.media-body
       [:div.media-heading
        [:p.heading-link badge_name]]
       [:div.media-issuer
        [:p issuer_name]]]]]]))


(defn element-visible? [element state]
  (if (and
       (or (clojure.string/blank? (:search @state))
           (not= (.indexOf
                  (.toLowerCase (:badge_name element))
                  (.toLowerCase (:search @state)))
                 -1))
       (or (clojure.string/blank? (:selected-issuer @state))
           (not= (.indexOf
                  (.toLowerCase (:issuer_name element))
                  (.toLowerCase (:selected-issuer @state)))
                 -1)))
    true false))

(defn badge-modal [state]
  (let [badges @(cursor state [:badges])]
   (fn []
    [:div#badge-gallery
     [:div.col-md-12
      [:div.well.well-sm
       [:div.form-group
        [:input#namesearch.form-control
         {:style {:max-width "300px"}
          :type "text"
          :name "search"
          :on-change #(reset! (cursor state [:search]) (.-target.value %))
          :placeholder "Filter by badge name"}]]
       [:div.form-group
        [:select#issuerselect.form-control
         {:name "selectissuer"
          :style {:max-width "300px"}
          :on-change #(reset! (cursor state [:selected-issuer]) (.-target.value %))
          :default-value ""}
         [:option {:value "" } "All"]
         (for [option (filter :enabled @(cursor state [:message_setting :issuers]))]
           ^{:key (:issuer_name option)}[:option {:value (:issuer_name option)} (:issuer_name option)])]]
       [:div.form-group
        [:label
          [:input
           {:style {:margin "0 5px"}
            :type "checkbox"
            :default-checked @(cursor state [:select-all])
            :on-change #(do
                          (reset! (cursor state [:selected-badges]) [])
                          (reset! (cursor state [:select-all]) (not @(cursor state [:select-all])))
                          (when @(cursor state [:select-all])

                            (reset! (cursor state [:selected-badges])  (filter (fn [b] (element-visible? b state)) badges))))}]
          [:b (t :extra-spaces/Selectall)]]]]

      [:div#badges (into [:div {:class "row wrap-grid"
                                :id    "grid"
                                :style {:max-height "700px" :overflow "auto"}}]
                         (for [badge badges]
                           (when (element-visible? badge state)
                            (badge-grid-element badge state))))]
      [:div.well.well-sm.text-center
       [:button.btn.btn-primary.btn-bulky
        {:data-dismiss "modal" :aria-label (t :core/Continue)}
        (t :core/Continue)]]]])))

(defn content [space-id state]
  [:div#space
   [m/modal-window]
   [:p (t :extra-spaces/Aboutmessagetool)]
   [:div.panel.panel-default
    [:div.panel-heading.weighted
     (t :extra-spaces/MessageTool)]
    [:div.panel-body
      (when @(cursor state [:message_alert])
        [:div.alert.alert-success
         (t :extra-spaces/Messagesentsuccess)])
      [:div.row
       [:div.col-md-12
        [:div.col-md-6 {:style {:margin "10px auto"}}
         [:a
          {:type "button"
           :href "#"
           :on-click #(mo/open-modal [:space :badges-mt] state {:hidden (fn []
                                                                           (reset! (cursor state [:select-all]) false)
                                                                           (reset! (cursor state [:selected-issuer]) "")
                                                                           (reset! (cursor state [:search]) "")
                                                                           (fetch-badge-earners state))})}
          [:span [:i.fa.fa-certificate.fa-lg] " " (t :extra-spaces/Addbadge)]]
         (when (seq @(cursor state [:selected-badges]))
          [:div
           [:div.row
            [:div.col-md-12 [:a.pull-right {:href "#" :on-click #(reset! (cursor state [:selected-badges]) []) :role "button"} [:b (t :social/clearall)]]]]
           [:div#admin-report {:style {:max-height "500px" :overflow "auto" :margin "10px auto"}}

               (reduce
                 #(conj %1
                   ^{:key (:id %2)}[:a.list-group-item
                                    [:div.inline
                                     [:button.close
                                      {:type "button"
                                       :aria-label (t :core/Delete)
                                       :on-click (fn []
                                                   (reset! (cursor state [:selected-badges]) (remove (fn [b] (= (:id b) (:id %2))) @(cursor state [:selected-badges])))
                                                   (fetch-badge-earners state))}
                                      [:span {:aria-hidden "true"
                                              :dangerouslySetInnerHTML {:__html "&times;"}}]]
                                     [:img.logo {:src (str "/" (:badge_image %2))}]
                                     [:span.name (:badge_name %2)]]])

                 [:div.list-group {:style {:margin "10px auto"}}]
                 @(cursor state [:selected-badges]))]
           (when (pos? (dec (count @(cursor state [:selected-badges]))))
             [:div.row
              [:div.col-md-12
                [:div.checkbox
                 [:label
                  [:input {:type "checkbox"
                           :name "allbadges"
                           :id "allbadgesinput"
                           :on-change #(do
                                         (reset! (cursor state [:allbadgesreceived]) (not @(cursor state [:allbadgesreceived])))
                                         (fetch-badge-earners state))
                           :default-checked (true? @(cursor state [:allbadgesreceived]))}]
                  [:b (t :extra-spaces/Allbadgesreceived)]]]]])])]

        (when (seq @(cursor state [:selected-badges]))
          [:div.col-md-6 {:style {:margin "10px auto"}}
           [:span [:b (count @(cursor state [:emails])) " " (t :extra-spaces/recipientsfound)]]
           [:div.row
            [:div.col-md-12
             [:b.text-muted (t :extra-spaces/Recipientlistinfo)]]]
           (when (seq @(cursor state [:emails]))
             [:div#admin-report {:style {:max-height "500px" :overflow "auto" :margin "10px auto"}}
              (reduce
               #(conj %1 ^{:key %2}[:li.list-group-item %2]) [:ul.list-group {:style {:margin "10px auto"}}] @(cursor state [:emails]))])])]]
      (when (and (seq @(cursor state [:selected-badges])) (seq @(cursor state [:emails])))
       [:div.col-md-12

        [:div.well.well-sm;.panel.panel-default
         [:div.panel-body
          [:div.form-group
           [:label {:for "languages"}
            (t :extra-spaces/Languageselect)]
           [input/radio-button-selector "language" (session/get :languages []) (cursor state [:message_language])]]
          [:div.form-group
           [:label {:for "subject"} (t :extra-spaces/Subject) [:span.form-required " *"]]
           [:input#subject.form-control
            {:style {:max-width "500px"}
             :type "text"
             :value @(cursor state [:message :subject])
             :on-change #(reset! (cursor state [:message :subject]) (.-target.value %))
             :required true}]]
          [:div.form-group
           [:label {:for "message"} (t :extra-spaces/Message) [:span.form-required " *"]]
           [:textarea#message.form-control
            {:style {:max-width "500px"}
             :on-change #(reset! (cursor state [:message :content]) (.-target.value %))
             :rows 8
             :value @(cursor state [:message :content])
             :required true}]]
          [:div.btn-toolbar
           [:div.btn-group
            [:button.btn.btn-primary.btn-bulky
             {:aria-label (t :extra-spaces/Sendmessages)
              :on-click #(do
                          (.preventDefault %)
                          (reset! (cursor state [:sending_messages]) true)
                          (ajax/POST
                            (path-for (str "/obpv1/space/message_tool/send_message/" space-id))
                            {:params {:ids (mapv :id @(cursor state [:selected-badges]))
                                      :message @(cursor state [:message])
                                      :message_language @(cursor state [:message_language])
                                      :received_all @(cursor state [:allbadgesreceived])}
                             :handler (fn [data]
                                        (when (= "success" (:status data))
                                          (reset! (cursor state [:sending_messages]) false)
                                          (reset! (cursor state [:message_alert]) true)))

                              :finally (fn [] (js/setTimeout
                                                (fn []
                                                  (reset! (cursor state [:message_alert]) false)
                                                  (reset! (cursor state [:sending_messages]) false))
                                                3000))}))

              :disabled (or (clojure.string/blank? @(cursor state [:message :subject]))
                            (clojure.string/blank? @(cursor state [:message :content])))}
             [:span (when @(cursor state [:sending_messages]) [:i.fa.fa-cog.fa-spin.fa-lg]) " " (t :extra-spaces/Sendmessage)]]
            [:button.btn.btn-warning.btn-bulky
             {:aria-label (t :core/Cancel)
              :on-click #(do
                           (.preventDefault %)
                           (reset! (cursor state [:message]) {:content "" :subject ""}))}
             (t :core/Cancel)]]]]]])
      (when (empty? @(cursor state [:selected-badges]))
       [:div.col-md-12
        [:div.well.well-sm
         (t :extra-spaces/Selectbadgefirst)]])]]])


(defn init-badges [space-id state]
  (ajax/POST
   (path-for (str "/obpv1/space/message_tool/badges/" space-id))
   {:handler (fn [data]
               (reset! (cursor state [:badges]) data))}))

(defn init-data [space-id state]
  (init-message-tool-settings space-id state)
  (init-badges space-id state))

(defn handler [site-navi]
  (let [space-id (session/get-in [:user :current-space :id] 0)
        state (atom {:space-id space-id
                     :badges []
                     :search ""
                     :selected-issuer ""
                     :selected-badges []
                     :select-all false
                     :emails []
                     :message {:content "" :subject ""}
                     :sending_messages false
                     :message_alert false
                     :allbadgesreceived false
                     :message_language (session/get-in [:user :language] "en")})]
   (init-data space-id state)
   (fn []
    (layout/default site-navi [content space-id state]))))
