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
            [reagent.core :refer [atom cursor]]
            [salava.core.ui.field :as f]))


(defn num-days-left [timestamp]
  (int (/ (- timestamp (/ (.now js/Date) 1000)) 86400)))

(defn delete-badge [id state init-data]
  (ajax/DELETE
    (path-for (str "/obpv1/badge/" id))
    {:handler
     (fn []
       #_(init-data state)
       #_(navigate-to (str "/badge")))}))

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

(defn badge-icons [params]
 (let [{:keys [endorsement-count meta_badge meta_badge_req visibility expires_on]} params]
  [:div.icons.row
   [:div.col-md-12
    [:span.lefticons.pull-left {:style {:display "flex" :flex-direction "column" :width "20%"}}
     [:span.visibility-icon {:style {:padding-bottom "3px"} :title visibility}
      (case visibility
       "private" [:i.fa.fa-lock]
       "internal" [:i.fa.fa-group]
       "public" [:i.fa.fa-globe]
        nil)]
     (if expires_on
       [:span [:i.fa.fa-hourglass-half {:title (str (t :badge/Expiresin) " " (num-days-left expires_on) " " (t :badge/days))}]])]

    [:span.righticons.pull-right {:style {:display "flex" :flex-direction "column"}}
     (when (pos? endorsement-count) [:span.badge-view {:title (str endorsement-count " " (if (> endorsement-count 1) (t :badge/endorsements) (t :badge/endorsement) )) :style {:padding-bottom "5px"}} [:i.fa.fa-handshake-o]])
     (into [:span]
       (for [f (plugin-fun (session/get :plugins) "block" "meta_icon")]
         [f meta_badge meta_badge_req]))]]]))

(defn badge-grid-element [element-data state badge-type init-data]
  (let [{:keys [id image_file name description visibility expires_on revoked issuer_content_name issuer_content_url recipients badge_id gallery_id assertion_url meta_badge meta_badge_req endorsement_count user_endorsements_count pending_endorsements_count new_message_count]} element-data
        expired? (bh/badge-expired? expires_on)
        badge-link (path-for (str "/badge/info/" id))
        obf_url (session/get :factory-url)
        ;{:keys [all-messages new-messages]} message_count
        notification-count (+ new_message_count pending_endorsements_count)
        endorsementscount (+ endorsement_count user_endorsements_count)]
    [:div {:class "media grid-container" :style {:position "relative"}}

     (cond
       (= "basic" badge-type) (if (or expired? revoked)
                                [:div {:class (str "media-content " (if expired? "media-expired") (if revoked " media-revoked"))}
                                 (cond
                                   revoked [:div.icons
                                            [:div.lefticon [:i {:class "fa fa-ban"}] (t :badge/Revoked)]
                                            [:a.righticon {:class "righticon revoked" :on-click #(do (.preventDefault %)(m/modal! [delete-badge-modal id state init-data]
                                                                                                                                  {:size :lg :hidden (fn [] (init-data state))})) :title (t :badge/Delete)} [:i {:class "fa fa-trash"}]]]
                                   expired? [:div.icons
                                             [:div.lefticon [:i {:class "fa fa-history"}] (t :badge/Expired)]
                                             [:a.righticon {:class "righticon expired" :on-click #(do (.preventDefault %) (m/modal! [delete-badge-modal id state init-data]
                                                                                                                                    {:size :lg :hidden (fn [] (init-data state))})) :title (t :badge/Delete)} [:i {:class "fa fa-trash"}]]])
                                 [:a {:href "#" :on-click #(do (.preventDefault %)(mo/open-modal [:badge :info] {:badge-id id} {:hide (fn [] (init-data state))}))}
                                  (if image_file
                                    [:div.media-left
                                     [:img.badge-img {:src (str "/" image_file)
                                                      :alt (str (t :badge/Badge) " " name)}]])
                                  [:div.media-body
                                   [:div.media-heading
                                    [:p.heading-link name]]
                                   [:div.media-issuer
                                    [:p issuer_content_name]]]]]

                                [:div {:class (str "media-content " (if expired? "media-expired") (if revoked " media-revoked"))}
                                 [:a {:href "#" :on-click #(do
                                                             (.preventDefault %)
                                                             (mo/open-modal [:badge :info] {:badge-id id} {:shown (fn [] (.replaceState js/history {} "Badge modal" (path-for (str "/badge?id=" id))))
                                                                                                           :hidden (fn []
                                                                                                                     (do
                                                                                                                       (if (clojure.string/includes? (str js/window.location.href) (path-for (str "/badge?id=" id)))
                                                                                                                         (.replaceState js/history {} "Badge modal" (path-for "/badge"))
                                                                                                                         (navigate-to (current-route-path))))
                                                                                                                    (init-data state))}))}
                                    [badge-icons {:endorsement-count endorsementscount :meta_badge meta_badge :meta_badge_req meta_badge_req :visibility visibility :expires_on expires_on}]
                                  (if image_file
                                    [:div.media-left
                                     [:img.badge-img {:src (str "/" image_file)
                                                      :alt name}]])
                                  [:div.media-body {:style {:position "relative"}}
                                   (when (pos? notification-count) [:span.badge.grid-element-info notification-count])
                                   [:div.media-heading
                                    [:p.heading-link name]]

                                   [:div.media-issuer
                                    [:p issuer_content_name]]]]])


       (= "profile" badge-type) [:div
                                 [:a {:href "#" :on-click #(mo/open-modal [:badge :info] {:badge-id id})}
                                  [:div.media-content

                                   [:div.icons.col-xs-12 {:style {:min-height "15px" :padding "0px"}}
                                    [:div.visibility-icon.inline
                                     ;(if metabadge-icon-fn [:div.pull-right [metabadge-icon-fn id]])
                                     (when (or (pos? user_endorsements_count) (pos? endorsement_count)) [:span.badge-view [:i.fa.fa-handshake-o]])]]


                                   [:div.media-left
                                    (if image_file  [:img {:src (str "/" image_file) :alt (str (t :badge/Badge) " " name)}])
                                    [:div.media-body
                                     [:div.media-heading name]
                                     [:div.media-issuer [:p issuer_content_name]]]]]]]


       (= "gallery" badge-type) [:div
                                 [:a {:href "#" :on-click #(mo/open-modal [:gallery :badges] {:badge-id badge_id :gallery-id gallery_id})
                                      :title name}
                                  [:div.media-content
                                   (if image_file
                                     [:div.media-left
                                      [:img {:src (str "/" image_file)
                                             :alt (str (t :badge/Badge) " " name)}]])
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
                                  [:div {:class "pull-left"}]

                                  (admin-gallery-badge badge_id "badges" state init-data)]]
       (= "pickable" badge-type) [:div
                                  [:a {:href "#" :on-click #(do
                                                              (.preventDefault %)
                                                              (let [new-field-atom (:new-field-atom @state)
                                                                    {:keys [type badges index function]} @new-field-atom
                                                                    badge {:id id :image_file image_file :type "badge" :name name :description description :visibility visibility}]
                                                                (cond
                                                                  (= "badge" type)(do
                                                                                    (swap! new-field-atom merge {:badge badge})
                                                                                    (if (:index @state)
                                                                                     (f/add-field-atomic (:block-atom @state) (:new-field-atom @state) (:index @state))
                                                                                     (f/add-field-atomic (:block-atom @state)(:new-field-atom @state))))

                                                                  (= "showcase" type) (when-not  (some (fn [b] (= id (:id b))) badges)
                                                                                          (as-> (:function @state) f (f badge)))
                                                                  (= "swap" type) (when-not (some (fn [b] (= id (:id b))) badges)
                                                                                   (function index badge badges))
                                                                 :else nil)))

                                       :data-dismiss "modal"}

                                   [:div.media-content
                                    [badge-icons {:endorsement-count endorsementscount :meta_badge meta_badge :meta_badge_req meta_badge_req :visibility visibility :expires_on expires_on}]
                                    [:div.media-left
                                     (if image_file  [:img {:src (str "/" image_file) :alt (str (t :badge/Badge) " " name)}])
                                     [:div.media-body
                                      [:div.media-heading name]
                                      [:div.media-issuer [:p issuer_content_name]]]]]]]

       (= "showcase" badge-type) [:div
                                  [:a {:href "#" :on-click #(do
                                                              (.preventDefault %)
                                                              (mo/open-modal [:badge :info] {:badge-id id}))}

                                   [:div.media-content

                                    [:div.media-left
                                     (if image_file  [:img {:src (str "/" image_file) :alt (str (t :badge/Badge) " " name)}])
                                     [:div.media-body
                                      [:div.media-heading name]]]]]
                                  [:div.swap-button {:title (t :page/Replacebadge)}
                                   [:a {:href "#" :on-click (fn []
                                                             (let [index (.indexOf (mapv :id (:badges @state)) id)
                                                                   new-field (atom {:type "swap" :index index :badges (:badges @state) :function (:swap! init-data)})]
                                                              (mo/open-modal [:badge :my] {:type "pickable" :block-atom state :new-field-atom new-field})))
                                        :aria-label (t :page/Replacebadge)}
                                    [:i.fa.fa-exchange]]]
                                  (when-not (= id (last (mapv :id (:badges @state))))
                                   [:div.move-right {:title (t :page/Moveright)}
                                                    [:a {:href "#" :on-click (fn []
                                                                              (let [index (.indexOf (mapv :id (:badges @state)) id)]
                                                                               (f/move-field :down (cursor state [:badges]) index)))
                                                         :aria-label (t :page/Moveright)}
                                                        [:i.fa.fa-chevron-right]]])
                                  (when-not (= id (first (mapv :id (:badges @state))))
                                   [:div.move-left {:title (t :page/Moveleft)}
                                               [:a {:href "#" :on-click (fn []
                                                                         (let [index (.indexOf (mapv :id (:badges @state)) id)]
                                                                          (f/move-field :up (cursor state [:badges]) index)))
                                                    :aria-label (t :page/Moveleft)}
                                                   [:i.fa.fa-chevron-left]]])


                                  [:div.delete-button {:title (t :badge/Delete)}
                                   [:a {:href "#" :on-click #(do
                                                               (.preventDefault %)
                                                               (let [block-atom @state]
                                                                ((:delete! init-data) id (:badges block-atom))))
                                        :aria-label (t :badge/Delete)}


                                    [:i.fa.fa-trash]]]]
      (= "embed" badge-type)    [:div
                                  [:a.heading-link {:target "_blank" :href (path-for (str "/badge/info/" id))}
                                    [:div.media-content
                                     (if image_file
                                      [:div.media-left
                                       [:img {:src (str "/" image_file) :alt (str (t :badge/Badge) " " name)}]])
                                     [:div.media-body
                                      [:div.media-heading
                                       [:p.heading-link
                                        name]]
                                      [:div.media-description description]]]]])]))
