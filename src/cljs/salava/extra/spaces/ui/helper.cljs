(ns salava.extra.spaces.ui.helper
 (:require
  [clojure.string :refer [blank? join split replace lower-case]]
  [reagent.core :refer [atom cursor]]
  [reagent-modals.modals :as m]
  [salava.core.ui.ajax-utils :as ajax]
  [salava.core.i18n :refer [t translate-text]]
  [salava.core.time :refer [date-from-unix-time]]
  [salava.core.ui.helper :refer [input-valid? path-for navigate-to]]
  [salava.core.ui.modal :as mo]
  [salava.user.ui.helper :refer [profile-picture]]
  [salava.extra.spaces.schemas :as schemas]))

(defn init-data [state]
  (ajax/GET
   (path-for "/obpv1/spaces/")
   {:handler (fn [data]
               (swap! state assoc :spaces data))}))

(defn generate-alias [state]
  (when-let [x (not (blank?  @(cursor state [:space :name])))]
    (reset! (cursor state [:space :alias])
        (as-> @(cursor state [:space :name]) $
              (replace $ #" " "")
              (lower-case $)
              (if (> (count $) 15) (clojure.string/join (take 10 $)) $)))))



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

(defn edit-space [state]
  (reset! (cursor state [:error-message]) nil)
  (let [data (select-keys @(cursor state [:space])[:id :name :description :alias :logo :css :banner])
         validate-info (validate-inputs schemas/create-space data)]
    (if (some false? validate-info)
        (reset! (cursor state [:error-message])
          (case (.indexOf validate-info false)
            0 (t :extra-spaces/Namefieldempty)
            1 (t :extra-spaces/Descriptionfieldempty)
            2 (t :extra-spaces/Aliasfieldempty)
            (t :extra-spaces/Errormsg)))

       (ajax/POST
        (path-for (str "/obpv1/spaces/edit/" @(cursor state [:space :id])))
        {:params data #_(-> @(cursor state [:space])
                          (assoc :admins (mapv :id @(cursor state [:space :admins]))))
          :handler (fn [data]
                    (when (= (:status data) "error")
                      (reset! (cursor state [:error-message]) data))
                    (when (= (:status data) "success")
                      (swap! state assoc :tab nil :tab-no 1)))}))))


(defn profile-link-inline-modal [id first_name last_name picture]
  (let [name (str first_name " " last_name)]
    [:div.user-link-inline
     [:a {:href "#"
          :on-click #(mo/open-modal [:profile :view] {:user-id id})}
      [:img {:src (profile-picture picture) :alt (str name (t :user/Profilepicture))}]
      name]]))

(defn space-card [info state]
 (let [{:keys [id name logo valid_until visibility status ctime banner member_count]} info
       bg (case status
            "active" "green"
            "suspended" "yellow"
            "red")]
   [:div {:class "col-xs-12 col-sm-6 col-md-4"}
          ;:key id}
    [:div {:class "media grid-container"}
     [:a {:href "#" :on-click #(mo/open-modal [:space :info] {:id id :member_count member_count} {:hidden (fn [] (init-data state))}) :style {:text-decoration "none"}}
      #_(when banner
          [:div.banner
           [:img {:src (str "/" banner)}]
           [:hr.line]])
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
         [:div.status.join-date
          (t :extra-spaces/Createdon) ": " (date-from-unix-time (* 1000 ctime))]
        ;[:div.status
         ; [:i.fa.fa-users.fa-fw member_count]]

         [:div.status
           [:div.blob {:class status}]
          [:b (t (keyword (str "extra-spaces/"status)) (when (and valid_until (= status "active")) (str " " (t :extra-spaces/until) (date-from-unix-time (* 1000 valid_until)))))]]]]]]]]))


