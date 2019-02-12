(ns salava.badge.ui.endorsement
  (:require [salava.core.i18n :refer [t]]
            [reagent.core :refer [atom cursor create-class]]
            [salava.core.time :refer [unix-time date-from-unix-time]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.helper :refer [path-for private?]]
            [salava.core.ui.modal :as mo]
            [clojure.string :refer [blank?]]
            [reagent.session :as session]
            [salava.user.ui.helper :refer [profile-picture]]))


(defn endorsement-row [endorsement]
  (let [{:keys [issuer content issued_on]} endorsement]

    [:div {:style {:margin-bottom "20px"}}
     [:h5
      [:a {:href "#"
           :on-click #(do (.preventDefault %) (mo/set-new-view [:badge :issuer] (:id issuer)))
           } (:name issuer)]
      " "
      [:small (date-from-unix-time (* 1000 issued_on))]]
     [:div {:dangerouslySetInnerHTML {:__html content}}]]))


(defn init-badge-endorsements [state badge-id]
  (ajax/GET
    (path-for (str "/obpv1/badge/endorsement/" badge-id))
    {:handler (fn [data] (reset! state data))}))

(defn badge-endorsement-content [badge-id]
  (let [endorsements (atom [])]
    (init-badge-endorsements endorsements badge-id)
    (fn []
      [:div.row {:id "badge-contents"}
       [:div.col-xs-12
        [:h4 {:style {:margin-bottom "20px"}} (t :badge/BadgeEndorsedBy)]
        (into [:div]
              (for [endorsement @endorsements]
                (endorsement-row endorsement)))]])))

;; User Endorsements

(defn init-user-badge-endorsement [state]
  (ajax/GET
    (path-for (str "/obpv1/badge/user/endorsement/" (:id @state)))
    {:handler (fn [data]
                (swap! state assoc :user-badge-endorsements data)
                (when (some #(= (:endorser_id %) (:endorser-id @state)) data)
                  (swap! state assoc :show-link "none"
                         :show-content "none"
                         :show-endorsement-status "block")))}))


(defn init-pending-endorsements [state]
  (ajax/GET
    (path-for "/obpv1/badge/user/pending_endorsement/")
    {:handler (fn [data]
                (swap! state assoc :pending data)
                )}
    )
  )

(defn save-endorsement [state]
  (ajax/POST
    (path-for (str "/obpv1/badge/endorsement/" (:id @state)))
    {:params {:content @(cursor state [:endorsement-comment]) }
     :handler (fn [data]
                (when (= (:status data) "success")
                  (swap! state assoc :show-link "none"
                         :show-content "none"
                         :show-endorsement-status "block")
                  ))}))

(defn update-status [id status user_badge_id state reload-fn]
  (ajax/POST
    (path-for (str "/obpv1/badge/endorsement/update_status/" id))
    {:params {:user_badge_id user_badge_id
              :status status}
     :handler (fn [data]
                (when (= "success" (:status data))
                  (reload-fn state)))}))

(defn delete-endorsement [id user_badge_id state]
  (ajax/DELETE
    (path-for (str "/obpv1/badge/endorsement/" user_badge_id "/" id))
    {:handler (fn [data]
                (when (= "success" (:status data))
                  (init-user-badge-endorsement state)))}))


(defn process-text [s state]
  (let [text (-> js/document
                 (.getElementById "claim")
                 (.-innerHTML))
        endorsement-claim (str text (if (blank? text) "" "\n\n") "* " s)]
    (reset! (cursor state [:endorsement-comment]) endorsement-claim)))

(defn endorse-badge-content [state]
  (fn []
    [:div {:style {:display @(cursor state [:show-content])}}
     [:hr]
     [:div.row
      [:div.col-xs-12 [:a.cancel.pull-right {:href "#" :on-click #(do
                                                                    (.preventDefault %)
                                                                    (swap! state assoc :show-link "block"
                                                                           :show-content "none"))} [:i.fa.fa-remove]]]]

     [:div.endorse {:style {:margin "5px"}} (t :badge/Endorsehelptext)]

     [:div.row
      [:div.col-xs-12
       [:div.list-group
        [:a.list-group-item {:href "#" :on-click #(do
                                                    (.preventDefault %)
                                                    (process-text (t :badge/Endorsephrase1) state)
                                                    )} [:i.fa.fa-plus-circle](t :badge/Endorsephrase1)]
        [:a.list-group-item {:href "#" :on-click #(do
                                                    (.preventDefault %)
                                                    (process-text (t :badge/Endorsephrase2) state)
                                                    )} [:i.fa.fa-plus-circle](t :badge/Endorsephrase2)]
        [:a.list-group-item {:href "#" :on-click #(do
                                                    (.preventDefault %)
                                                    (process-text (t :badge/Endorsephrase3) state)
                                                    ) } [:i.fa.fa-plus-circle](t :badge/Endorsephrase3)]
        [:a.list-group-item {:href "#" :on-click #(do
                                                    (.preventDefault %)
                                                    (process-text (t :badge/Endorsephrase4) state)
                                                    ) } [:i.fa.fa-plus-circle](t :badge/Endorsephrase4)]
        [:a.list-group-item {:href "#" :on-click #(do
                                                    (.preventDefault %)
                                                    (process-text (t :badge/Endorsephrase5) state)
                                                    )  } [:i.fa.fa-plus-circle](t :badge/Endorsephrase5)]]]]

     [:div.form-group
      [:label {:for "claim"} (t :badge/Composeyourendorsement)  ]
      [:textarea.form-control {:id "claim"
                               :rows "5"
                               :cols "60"
                               :max-length "1500"
                               :type "text"
                               :value @(cursor state [:endorsement-comment])
                               :on-change #(do
                                             (reset! (cursor state [:endorsement-comment]) (.-target.value %))
                                             )
                               #_:on-key-down #_(if (= (.-which %) 13)
                                               #_(add-tag tags-atom new-tag-atom))}]]
     [:div
      [:button.btn.btn-primary {:on-click #(do
                                             (.preventDefault %)
                                             (save-endorsement state)
                                             ;;save endorsement
                                             )
                                :disabled (blank? @(cursor state [:endorsement-comment]))

                                } (t :badge/endorse)]]
     [:hr]]))

(defn endorsement-text [state]
  (let [user-endorsement (->> @(cursor state [:user-badge-endorsements])
                              (filter #(= (:endorser-id @state) (:endorser_id %))))]
    (if (seq user-endorsement)
      (case  (->> user-endorsement first :status)
        "accepted" [:span.label.label-success (t :badge/Youendorsebadge)]
        "declined" [:span.label.label-danger (t :badge/Declinedendorsement)]
        [:span.label.label-info (t :badge/Pendingendorsement)]
        )
      [:span.label.label-info (t :badge/Pendingendorsement)])))

(defn endorse-badge-link [state]
  (fn []
    [:div
     [:a {:href "#"
          :style {:display @(cursor state [:show-link])}
          :on-click #(do
                       (.preventDefault %)
                       (swap! state assoc :show-link "none"
                              :show-content "block")
                       )}[:i.fa.fa-handshake-o] (t :badge/Endorsethisbadge)]
     [:div {:style {:display @(cursor state [:show-endorsement-status])}} [:i.fa.fa-thumbs-up] (endorsement-text state)]]))

(defn profile-link-inline [id first_name last_name picture]
  [:div [:a {:href "#"
             :on-click #(mo/open-modal [:user :profile] {:user-id id})}
         [:img {:src (profile-picture picture)}]
         (str first_name " " last_name " ")]  (t :badge/Hasendorsedyou)])

(defn pending-endorsements []
  (let [state (atom {:user-id (-> (session/get :user) :id)})]
    (init-pending-endorsements state)
    (fn []
      [:div#endorsebadge
       (reduce (fn [r endorsement]
                 (let [{:keys [id user_badge_id image_file name content first_name last_name profile_picture endorser_id]} endorsement]
                   (conj r
                         [:div
                          [:div.col-md-12
                           [:div.thumbnail
                            [:div.endorser.col-md-12
                             [profile-link-inline endorser_id first_name last_name profile_picture id]
                             [:hr.border]
                             ]
                            [:div.caption.row
                             [:div.position-relative.text-center.col-md-3
                              [:img{:src (str "/" image_file)}]]

                             [:div.col-md-9 [:h4.media-heading name]
                              [:div.thumbnail-description.smaller {:dangerouslySetInnerHTML {:__html content}}]]
                             ]

                            [:div.caption.card-footer.text-center
                             [:hr]
                             [:ul.list-inline.buttons
                              [:a.button {:href "#"
                                          :on-click #(do
                                                       (.preventDefault %)
                                                       (update-status id "accepted" user_badge_id state init-pending-endorsements)
                                                       )} [:li [:i.fa.fa-check] ]]
                              [:a.button {:href "#"
                                          :on-click #(do
                                                       (.preventDefault %)
                                                       (update-status id "declined" user_badge_id state init-pending-endorsements))}[:li.cancel [:i.fa.fa-remove]]]]]]]]))) [:div.row] @(cursor state [:pending]))
       ])))

(defn endorse-badge [badge-id]
  (let [state (atom {:id badge-id
                     :show-link "block"
                     :show-content "none"
                     :endorsement-comment ""
                     :endorser-id (-> (session/get :user) :id)
                     :show-endorsement-status "none"})]
    (init-user-badge-endorsement state)
    (fn []
      [:div#endorsebadge {:style {:margin-top "10px"}}
       [endorse-badge-link state]
       [endorse-badge-content state]])))

(defn endorsement-list [badge-id]
  (let [state (atom {:id badge-id})]
    (init-user-badge-endorsement state)

    (fn []
      (when (seq @(cursor state [:user-badge-endorsements]))
        [:div
         [:div.row
          [:label.col-md-12.sub-heading (t :badge/Endorsements)]]
         [:div#endorsebadge

          (reduce (fn [r endorsement]
                    (let [{:keys [id user_badge_id image_file name content first_name last_name profile_picture endorser_id status]} endorsement]
                      (conj r [:div.panel.panel-default.endorsement
                               [:div.panel-heading {:id (str "heading" id)}
                                [:div.panel-title
                                 (case status
                                   "accepted" [:span.label.label-success (t :core/Accepted)]
                                   "declined" [:span.label.label-danger (t :core/Declined)]
                                   "pending" [:span.label.label-info (t :core/Pending)]
                                   [:span.label.label-info (t :core/Pending)])
                                 [:div.row.flip.settings-endorsement
                                  [:div.col-md-9
                                   [:a {:href "#"
                                        :on-click #(mo/open-modal [:user :profile] {:user-id endorser_id})}
                                    [:img.small-image {:src (profile-picture profile_picture)}]
                                    (str first_name " " last_name " ")] ]]]

                                [:div [:button {:type "button"
                                                :aria-label "OK"
                                                :class "close"
                                                :on-click #(do (.preventDefault %)
                                                             (delete-endorsement id user_badge_id state))
                                                }
                                       [:i.fa.fa-trash.trash]]]]
                               [:div.panel-body
                                [:div {:dangerouslySetInnerHTML {:__html content}}]
                                (when (= "pending" status)
                                  [:div.caption
                                   [:hr]
                                   [:div.text-center
                                    [:ul.list-inline.buttons.buttons
                                     [:a.button {:href "#"
                                                 :on-click #(do
                                                              (.preventDefault %)
                                                              (update-status id "accepted" user_badge_id state init-user-badge-endorsement)
                                                              )} [:li [:i.fa.fa-check] ]]
                                     [:a.button {:href "#"
                                                 :on-click #(do
                                                              (.preventDefault %)
                                                              (update-status id "declined" user_badge_id state init-user-badge-endorsement))}[:li.cancel [:i.fa.fa-remove]]]]]])
                                ]]))

                    ) [:div] @(cursor state [:user-badge-endorsements]))]]))))

(defn request-endorsement [])


