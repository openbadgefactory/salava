(ns salava.core.ui.badge-grid
  (:require [salava.badge.ui.helper :as bh]
            [salava.core.ui.helper :as h :refer [unique-values navigate-to path-for js-navigate-to plugin-fun current-path current-route-path]]
            [salava.core.i18n :as i18n :refer [t]]
            [salava.admin.ui.admintool :refer [admin-gallery-badge]]
            [salava.core.ui.modal :as mo]
            [reagent-modals.modals :as m]
            [salava.admin.ui.helper :refer [admin?]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.social.ui.follow :refer [follow-badge]]
            [reagent.session :as session]
            #_[salava.metabadge.ui.metabadge :as mb]
            [reagent.core :refer [atom cursor]]
            [salava.core.ui.field :as f]))


(defn num-days-left [timestamp]
  (int (/ (- timestamp (/ (.now js/Date) 1000)) 86400)))

(defn delete-badge [id state init-data]
  (ajax/DELETE
    (path-for (str "/obpv1/badge/" id))
    {:handler
     (fn []
       (init-data state)
       (navigate-to (str "/badge")))}))

(defn delete-badge-modal [id state init-data]
  [:div#delete-modal
   [:div.modal-header
    [:button {:type "button"
              :class "close"
              :data-dismiss "modal"
              :aria-label "OK"}
     [:span {:aria-hidden "true"
             :dangerouslySetInnerHTML {:__html "&times;"}}]]]
   [:div.modal-body
    [:div {:class (str "alert alert-warning")}
     (t :badge/Confirmdelete)]]
   [:div.modal-footer
    [:button {:type "button"
              :class "btn btn-primary"
              :data-dismiss "modal"}
     (t :core/Cancel)]
    [:button {:type "button"
              :class "btn btn-warning"
              :data-dismiss "modal"
              :on-click #(delete-badge id state init-data)}
     (t :core/Delete)]]])


(defn badge-grid-element [element-data state badge-type init-data]
  (let [{:keys [id image_file name description visibility expires_on revoked issuer_content_name issuer_content_url recipients badge_id assertion_url meta_badge meta_badge_req endorsement_count user_endorsements_count]} element-data
        expired? (bh/badge-expired? expires_on)
        obf_url (session/get :factory-url)
        metabadge-icon-fn (first (plugin-fun (session/get :plugins) "metabadge" "metabadge_icon"))]
    [:div {:class "media grid-container"}
     (cond
       (= "basic" badge-type) (if (or expired? revoked)
                                [:div {:class (str "media-content " (if expired? "media-expired") (if revoked " media-revoked"))}
                                 (cond
                                   revoked [:div.icons
                                            [:div.lefticon [:i {:class "fa fa-ban"}] (t :badge/Revoked)]
                                            [:a.righticon {:class "righticon revoked" :on-click (fn [] (m/modal! [delete-badge-modal id state init-data]
                                                                                                                 {:size :lg})) :title (t :badge/Delete)} [:i {:class "fa fa-trash"}]]]
                                   expired? [:div.icons
                                             [:div.lefticon [:i {:class "fa fa-history"}] (t :badge/Expired)]
                                             [:a.righticon {:class "righticon expired" :on-click (fn [] (m/modal! [delete-badge-modal id state init-data]
                                                                                                                  {:size :lg})) :title (t :badge/Delete)} [:i {:class "fa fa-trash"}]]])
                                 [:a {:href "#" :on-click #(do (.preventDefault %)(mo/open-modal [:badge :info] {:badge-id id} {:hide (fn [] (init-data state))}))}
                                  (if image_file
                                    [:div.media-left
                                     [:img.badge-img {:src (str "/" image_file)
                                                      :alt name}]])
                                  [:div.media-body
                                   [:div.media-heading
                                    [:p.heading-link name]]
                                   [:div.media-issuer
                                    [:p issuer_content_name]]]
                                  ]]
                                [:div {:class (str "media-content " (if expired? "media-expired") (if revoked " media-revoked"))}
                                 [:a {:href "#" :on-click #(do
                                                             (.preventDefault %)
                                                             (mo/open-modal [:badge :info] {:badge-id id} {:shown (fn [] (.replaceState js/history {} "Badge modal" (path-for (str "/badge?id=" id))))
                                                                                                           :hidden (fn []
                                                                                                                     (do
                                                                                                                       (if (clojure.string/includes? (str js/window.location.href) (path-for (str "/badge?id=" id)))
                                                                                                                         (.replaceState js/history {} "Badge modal" (path-for "/badge"))
                                                                                                                         (navigate-to (current-route-path)))
                                                                                                                       (init-data state)))
                                                                                                           }))}
                                  [:div.icons
                                   [:div.visibility-icon.inline
                                    (case visibility
                                      "private" [:i {:class "fa fa-lock"}]
                                      "internal" [:i {:class "fa fa-group"}]
                                      "public" [:i {:class "fa fa-globe"}]
                                      nil)
                                    (if metabadge-icon-fn [:div.pull-right [metabadge-icon-fn id]])
                                    (when (or (pos? user_endorsements_count) (pos? endorsement_count)) [:span.badge-view [:i.fa.fa-handshake-o]])]

                                   (if expires_on
                                     [:div.righticon
                                      [:i {:title (str (t :badge/Expiresin) " " (num-days-left expires_on) " " (t :badge/days))
                                           :class "fa fa-hourglass-half"}]])]


                                  (if image_file
                                    [:div.media-left
                                     [:img.badge-img {:src (str "/" image_file)
                                                      :alt name}]])
                                  [:div.media-body

                                   [:div.media-heading
                                    [:p.heading-link name]]
                                   [:div.media-issuer
                                    [:p issuer_content_name]]]
                                  ]])

       (= "profile" badge-type) [:div
                                 [:a {:href "#" :on-click #(mo/open-modal [:badge :info] {:badge-id id})}
                                  [:div.media-content

                                   [:div.icons.col-xs-12 {:style {:min-height "15px" :padding "0px"}}
                                    [:div.visibility-icon.inline
                                     ;(if metabadge-icon-fn [:div.pull-right [metabadge-icon-fn id]])
                                     (when (or (pos? user_endorsements_count) (pos? endorsement_count)) [:span.badge-view [:i.fa.fa-handshake-o]])

                                     ]]
                                   [:div.media-left
                                    (if image_file  [:img {:src (str "/" image_file) :alt name}])
                                    [:div.media-body
                                     [:div.media-heading name]
                                     [:div.media-issuer [:p issuer_content_name]]]
                                    ]]]]

       (= "gallery" badge-type) [:div
                                 [:a {:href "#" :on-click #(mo/open-modal [:gallery :badges] {:badge-id badge_id})
                                      :title name}
                                  [:div.media-content
                                   (if image_file
                                     [:div.media-left
                                      [:img {:src (str "/" image_file)
                                             :alt name}]])
                                   [:div.media-body
                                    [:div.media-heading
                                     [:p.heading-link name]]
                                    [:div.media-issuer
                                     [:p issuer_content_name]]
                                    (if recipients
                                      [:div.media-recipients
                                       recipients " " (if (= recipients 1)
                                                        (t :gallery/recipient)
                                                        (t :gallery/recipients))])
                                    [:div.media-description description]]]]
                                 [:div.media-bottom
                                  [:div {:class "pull-left"}
                                   ]
                                  (admin-gallery-badge badge_id "badges" state init-data)]]
       (= "selectable" badge-type)  [:div
                                     [:a {:href "#" :on-click #(mo/open-modal [:badge :info] {:badge-id id})}
                                      [:div.media-content
                                       [:div.icons.col-xs-12 {:style {:min-height "15px" :padding "0px"}}
                                        [:div.visibility-icon.inline
                                         ;(if metabadge-icon-fn [:div.pull-right [metabadge-icon-fn id]])
                                         (when (or (pos? user_endorsements_count) (pos? endorsement_count)) [:span.badge-view [:i.fa.fa-handshake-o]])

                                         ]]
                                       [:div.media-left
                                        (if image_file  [:img {:src (str "/" image_file) :alt name}])
                                        [:div.media-body
                                         [:div.media-heading name]
                                         [:div.media-issuer [:p issuer_content_name]]]
                                        ]]]
                                     [:div {:class "media-bottom"}
                                      [:div.row
                                       [:div.col-xs-9
                                        (let [checked? (boolean (some #(= id %) (:badges-selected @state)))]
                                          [:div.checkbox
                                           [:label {:for (str "checkbox-" id)}
                                            [:input {:type "checkbox"
                                                     :id (str "checkbox-" id)
                                                     :on-change (fn []
                                                                  (if checked?
                                                                    (swap! state assoc :badges-selected (remove #(= % id) (:badges-selected @state)))
                                                                    (swap! state assoc :badges-selected (conj (:badges-selected @state) id))))
                                                     :checked checked?}]

                                            (t :badge/Exporttobackpack)]])]
                                       #_[:div {:class "col-xs-3 text-right"}
                                          [:a {:href (str obf_url "/c/receive/download?url="(js/encodeURIComponent assertion_url)) :class "badge-download"}
                                           [:i {:class "fa fa-download"}]]]]]]
       (= "pickable" badge-type) [:div
                                  [:a {:href "#" :on-click #(do
                                                              (.preventDefault %)

                                                              ;(swap! state assoc :selected element-data)
                                                              (swap! (cursor state [:new-field-atom]) assoc :badge {:id id :image_file image_file :type "badge"})
                                                              (f/add-field (cursor state [:block-atom])(:new-field-atom @state))
                                                             ;(prn (:function @state))
                                                              #_(if (:function @state) ((:function @state) #_{:id id :image_file image_file}))

                                                              (m/close-modal!))
                                       ;:data-dismiss "modal"
                                       }
                                   [:div.media-content

                                    [:div.icons.col-xs-12 {:style {:min-height "15px" :padding "0px"}}
                                     [:div.visibility-icon.inline
                                      ;(if metabadge-icon-fn [:div.pull-right [metabadge-icon-fn id]])
                                      (when (or (pos? user_endorsements_count) (pos? endorsement_count)) [:span.badge-view [:i.fa.fa-handshake-o]])

                                      ]]
                                    [:div.media-left
                                     (if image_file  [:img {:src (str "/" image_file) :alt name}])
                                     [:div.media-body
                                      [:div.media-heading name]
                                      [:div.media-issuer [:p issuer_content_name]]]
                                     ]]]])]))
