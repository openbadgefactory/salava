(ns salava.extra.spaces.ui.helper
 (:require
  [reagent.core :refer [atom cursor]]
  [reagent-modals.modals :as m]
  [salava.core.ui.ajax-utils :as ajax]
  [salava.core.i18n :refer [t translate-text]]
  [salava.core.time :refer [date-from-unix-time]]
  [salava.core.ui.helper :refer [input-valid? path-for]]
  [salava.core.ui.modal :as mo]
  [salava.user.ui.helper :refer [profile-picture]]
  [salava.extra.spaces.schemas :as schemas]))

(defn validate-inputs [s space]
  (doall
    [(input-valid? (:name s) (:name space))
     (input-valid? (:description s) (:description space))
     (input-valid? (:alias s) (:alias space))]))
     ;(input-valid? (:banner s) (:banner space))
     ;(input-valid? (:logo s) (:logo space))
     ;(input-valid? (:properties s) (:properties space))]))
     ;(input-valid? (:admins s) (:admins space))]))

(defn error-msg [state]
  [:div
   [:div.modal-header
    [:button {:type "button"
              :class "close"
              :data-dismiss "modal"
              :aria-label "OK"}
     [:span {:aria-hidden "true"
             :dangerouslySetInnerHTML {:__html "&times;"}}]]]
    ;[:h4.modal-title (translate-text message)]]
   [:div.modal-body
    [:div.alert.alert-warning
     @(cursor state [:error-message])]]
   [:div.modal-footer
    [:button {:type "button"
              :class "btn btn-primary"
              :data-dismiss "modal"}
     "OK"]]])


(defn create-space [state]
  (reset! (cursor state [:error-message]) nil)
  (let [validate-info (validate-inputs schemas/create-space @(cursor state [:space]))]
    (if (some false? validate-info)
      (do
        (reset! (cursor state [:error-message])
          (case (.indexOf validate-info false)
            0 (t :extra-spaces/Namefieldempty)
            1 (t :extra-spaces/Descriptionfieldempty)
            2 (t :extra-spaces/Aliasfieldempty)
            (t :extra-spaces/Errormsg)))
        (m/modal! (error-msg state) {}))


      (ajax/POST
       (path-for "/obpv1/spaces/create")
       {:params (-> @(cursor state [:space])
                    (assoc :admins (mapv :id @(cursor state [:space :admins]))))
        :handler (fn [data]
                  (when (= (:status data) "error")
                    (m/modal! (upload-modal data) {}))
                  (when (= (:status data) "success")
                    (navigate-to "admin/spaces")))}))))

(defn profile-link-inline-modal [id first_name last_name picture]
  (let [name (str first_name " " last_name)]
    [:div.user-link-inline
     [:a {:href "#"
          :on-click #(mo/open-modal [:profile :view] {:user-id id})}
      [:img {:src (profile-picture picture) :alt (str name (t :user/Profilepicture))}]
      name]]))

(defn space-card [info]
 (let [{:keys [name logo valid_until visibility status ctime]} info]
   [:div {:class "col-xs-12 col-sm-6 col-md-4"}
          ;:key id}
    [:div {:class "media grid-container"}
     [:a {:href "#"} ;:on-click #(mo/open-modal [:profile :view] {:user-id id}) :style {:text-decoration "none"}}
      [:div.media-content
       [:div.media-left
        (if logo
          [:img {:src (profile-picture logo)
                 :alt "" #_(str first_name " " last_name)}]
          [:i.fa.fa-building-o {:style {:font-size "40px" :margin-top "10px"}}])]
       [:div.media-body
        [:div {:class "media-heading profile-heading"}
         name]
        [:div.media-profile
         [:div.join-date
          (t :gallery/Joined) ": " (date-from-unix-time (* 1000 ctime))]]]]
      #_[:div.common-badges
         (if (= id current-user)
           (t :gallery/ownprofile)
           [:span common_badge_count " " (if (= common_badge_count 1)
                                           (t :gallery/commonbadge) (t :gallery/commonbadges))])]]]]))
(defn upload-modal [{:keys [status message reason]}]
  [:div
   [:div.modal-header
    [:button {:type "button"
              :class "close"
              :data-dismiss "modal"
              :aria-label "OK"}
     [:span {:aria-hidden "true"
             :dangerouslySetInnerHTML {:__html "&times;"}}]]
    [:h4.modal-title (translate-text message)]]
   [:div.modal-body
    [:div {:class (str "alert " (if (= status "error")
                                  "alert-warning"
                                  "alert-success"))}
     (translate-text message)]]
   [:div.modal-footer
    [:button {:type "button"
              :class "btn btn-primary"
              :data-dismiss "modal"}
     "OK"]]])
