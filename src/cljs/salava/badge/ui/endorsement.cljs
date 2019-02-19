(ns salava.badge.ui.endorsement
  (:require [salava.core.i18n :refer [t]]
            [reagent.core :refer [atom cursor create-class]]
            [salava.core.time :refer [unix-time date-from-unix-time]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.helper :refer [path-for private?]]
            [salava.core.ui.modal :as mo]
            [clojure.string :refer [blank?]]
            [reagent.session :as session]
            [salava.user.ui.helper :refer [profile-picture]]
            [reagent-modals.modals :as m]
            [salava.core.ui.layout :as layout]
            [salava.core.ui.error :as err]))


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

(defn init-user-badge-endorsement [state]
  (ajax/GET
    (path-for (str "/obpv1/badge/user/endorsement/" (:id @state)))
    {:handler (fn [data]
                (swap! state assoc :user-badge-endorsements data)
                (when (some #(= (:endorser_id %) (:endorser-id @state)) data)
                  (swap! state assoc :show-link "none"
                         :show-content "none"
                         :show-endorsement-status "block")))}))

(defn user-badge-endorsement-content [badge-id]
  (let [state (atom {:id badge-id})]
    (init-user-badge-endorsement state)
    (fn []
      (let [endorsements (:user-badge-endorsements @state)]
        (when (seq endorsements)
          [:div;.row {:id "badge-contents"}
           ;[:div.col-xs-12
           ;[:h4 {:style {:margin-bottom "20px"}} (t :badge/BadgeEndorsedBy)]
           (reduce (fn [r endorsement]
                     (let [{:keys [id user_badge_id image_file name content first_name last_name profile_picture endorser_id mtime]} endorsement]
                       (conj r [:div {:style {:margin-bottom "20px"}}
                                [:h5
                                 [:a {:href "#"
                                      :on-click #(do (.preventDefault %) (mo/set-new-view [:user :profile] {:user-id endorser_id}))
                                      } (str first_name " " last_name)]
                                 " "
                                 [:small (date-from-unix-time (* 1000 mtime))]]
                                [:div {:dangerouslySetInnerHTML {:__html content}}]]
                             ))) [:div] endorsements)]
          ;]
          )))))

#_(defn user-badge-endorsement-modal-link [id endorsement-count]
    (when (pos? endorsement-count)
      [:div.endorsement-link
       [:span [:i {:class "fa fa-users"}]]
       [:a {:href "#"
            :on-click #(do (.preventDefault %)
                         (mo/set-new-view [:badge :userbadgeendorsement] id))}
        (if (== endorsement-count 1)
          (str  endorsement-count " " (t :badge/endorsement))
          (str  endorsement-count " " (t :badge/endorsements)))]]))

(defn badge-endorsement-content [param]
  (let [endorsements (atom [])
        badge-id (if (map? param) (:badge-id param) param)
        user-badge-id (:id param)]
    (init-badge-endorsements endorsements badge-id)
    (fn []
      [:div.row {:id "badge-contents"}
       [:div.col-xs-12
        [:h4 {:style {:margin-bottom "20px"}} (t :badge/BadgeEndorsedBy)]
        (into [:div]
              (for [endorsement @endorsements]
                (endorsement-row endorsement)))]
       (when user-badge-id
         (if (seq @endorsements) [:hr])
         [:div.col-xs-12
          [user-badge-endorsement-content user-badge-id]]
         )])))

;; User Badge Endorsements

(defn init-user-endorsements [state]
  (ajax/GET
    (path-for (str "/obpv1/badge/user/endorsements"))
    {:handler (fn [data]
                (reset! state (assoc data
                                :initializing false
                                :permission "success"))
                )}
    (fn [] (swap! state assoc :permission "error"))))



(defn init-pending-endorsements [state]
  (ajax/GET
    (path-for "/obpv1/badge/user/pending_endorsement/")
    {:handler (fn [data]
                (swap! state assoc :pending data)
                )}))

(defn edit-endorsement [id badge-id content]
  (ajax/POST
    (path-for (str "/obpv1/badge/endorsement/edit/" id))
    {:params {:content content
              :user_badge_id badge-id}
     :handler (fn [data]
                (when (= "success" (:status data))
                  (prn data)
                  ))}))

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
                  (when reload-fn (reload-fn state))))}))

(defn delete-endorsement [id user_badge_id state reload-fn]
  (ajax/DELETE
    (path-for (str "/obpv1/badge/endorsement/" user_badge_id "/" id))
    {:handler (fn [data]
                (when (= "success" (:status data))
                  (when reload-fn (reload-fn state))))}))


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
      [:div.col-xs-12 {:style {:margin-bottom "10px"}} [:a.cancel.pull-right {:href "#" :on-click #(do
                                                                                                     (.preventDefault %)
                                                                                                     (swap! state assoc :show-link "block"
                                                                                                            :show-content "none"))} [:i.fa.fa-remove {:title (t :core/Cancel)}]]]]

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
      [:label {:for "claim"} (str (t :badge/Composeyourendorsement) ":") ]
      [:textarea.form-control {:id "claim"
                               :rows "10"
                               :cols "50"
                               :max-length "1500"
                               :type "text"
                               :value @(cursor state [:endorsement-comment])
                               :on-change #(do
                                             (reset! (cursor state [:endorsement-comment]) (.-target.value %))
                                             )
                               #_:on-key-down #_(when (= (.-which %) 13)
                                                  (prn (clojure.string/split-lines @(cursor state [:endorsement-comment])))
                                                  ;(process-text "test" state)
                                                  ;(reset! (cursor state [:endorsement-comment]) (str (.-target.value %) "\n\n* ") )
                                                  #_(add-tag tags-atom new-tag-atom))}]]
     [:div
      [:button.btn.btn-primary {:on-click #(do
                                             (.preventDefault %)
                                             (save-endorsement state))
                                :disabled (blank? @(cursor state [:endorsement-comment]))

                                } (t :badge/Endorsebadge)]]
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
                       )}[:i.fa.fa-thumbs-o-up {:style {:vertical-align "unset"}}] (t :badge/Endorsethisbadge)]
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
                 (let [{:keys [id user_badge_id image_file name content first_name last_name profile_picture endorser_id description]} endorsement]
                   (conj r
                         [:div
                          [:div.col-md-12
                           [:div.thumbnail
                            [:div.endorser.col-md-12
                             [profile-link-inline endorser_id first_name last_name profile_picture id]
                             [:hr.border]
                             ]
                            [:div.caption.row.flip
                             [:div.position-relative.badge-image.col-md-3
                              [:img {:src (str "/" image_file) :style {:padding "15px"}}]]

                             [:div.col-md-9 [:h4.media-heading name]
                              [:div.thumbnail-description.smaller {:dangerouslySetInnerHTML {:__html content}}]]
                             ]

                            [:div.caption.card-footer.text-center
                             [:hr]
                             [:button.btn.btn-primary {:href "#"
                                                       :on-click #(do
                                                                    (.preventDefault %)
                                                                    (update-status id "accepted" user_badge_id state init-pending-endorsements)
                                                                    )} (t :badge/Acceptendorsement)]
                             [:button.btn.btn-warning.cancel {:href "#"
                                                              :on-click #(do
                                                                           (.preventDefault %)
                                                                           (update-status id "declined" user_badge_id state init-pending-endorsements))} (t :badge/Declineendorsement)]
                             #_[:ul.list-inline.buttons
                                [:a.button {:href "#"
                                            :on-click #(do
                                                         (.preventDefault %)
                                                         (update-status id "accepted" user_badge_id state init-pending-endorsements)
                                                         )} [:li [:i.fa.fa-check] (t :badge/Acceptendorsement)]]
                                [:a.button {:href "#"
                                            :on-click #(do
                                                         (.preventDefault %)
                                                         (update-status id "declined" user_badge_id state init-pending-endorsements))}[:li.cancel [:i.fa.fa-remove] (t :badge/Declineendorsement)]]]]]]]))) [:div.row] @(cursor state [:pending]))
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
                                 (if (= "pending" status)  [:span.label.label-info (t :social/Pending)])
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
                                                             (delete-endorsement id user_badge_id state init-user-badge-endorsement))
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

(defn profile [element-data]
  (let [{:keys [id first_name last_name profile_picture status header]} element-data
        current-user (session/get-in [:user :id])]
    [:div.endorsement-profile.panel-default
     [:a {:href "#" :on-click #(mo/open-modal [:user :profile] {:user-id id})}
      [:div.panel-body.flip
       [:div.col-md-4
        [:div.profile-image
         [:img.img-responsive.img-thumbnail
          {:src (profile-picture profile_picture)
           :alt (str first_name " " last_name)}]]]
       [:div.col-md-8
        [:h4 (str first_name " " last_name)]
        (when (= status "pending") [:p [:span.label.label-info (t :social/Pending)]])]]]]))

(defn user-endorsement-content [params]
  (fn []
    (let [{:keys [endorsement state]} @params
          {:keys [id profile_picture name first_name last_name image_file content user_badge_id endorser_id endorsee_id status]} endorsement]
      [:div.row.flip {:id "badge-info"}
       [:div.col-md-3
        [:div.badge-image [:img.badge-image {:src (str "/" image_file)}]]]
       [:div.col-md-9
        [:div
         [:h1.uppercase-header name]
         [:div (if endorsee_id (t :badge/Manageendorsementtext1) (t :badge/Manageendorsementtext2))]
         [:hr]
         [:div.row
          [:div.col-md-4.col-md-push-8 (when-not (and endorser_id (= "pending" status))
                                         [:div.row
                                          [:div.col-xs-12.delete-btn {:style {:margin "5px 0px" :cursor "pointer" :width "100%"}}
                                           [:a {:style {:line-height "4"}
                                                :on-click #(do
                                                             (.preventDefault %)
                                                             (delete-endorsement id user_badge_id nil nil))
                                                :data-dismiss "modal"} [:i.fa.fa-trash] (t :badge/Deleteendorsement)]
                                           ]])]
          [:div.col-md-8.col-md-pull-4 [profile {:id (or endorsee_id endorser_id) :profile_picture profile_picture :first_name first_name :last_name last_name :status status}]]]

         (if endorsee_id
           [:div {:style {:margin-top "15px"}}
            [:div;.form-group
             [:label {:for "claim"} (str (t :badge/Composeyourendorsement) ":")]
             [:textarea.form-control {:id "claim"
                                      :rows "10"
                                      :cols "50"
                                      :max-length "1500"
                                      :type "text"
                                      :value @(cursor params [:endorsement :content])
                                      :on-change #(do
                                                    (reset! (cursor params [:endorsement :content]) (.-target.value %)))}]]
            [:div
             [:button.btn.btn-primary {:on-click #(do
                                                    (.preventDefault %)
                                                    (edit-endorsement id user_badge_id @(cursor params [:endorsement :content])))
                                       :disabled (blank? @(cursor params [:endorsement :content]))
                                       :data-dismiss "modal"

                                       } (t :core/Save)]

             #_[:a.pull-right {:style {:margin "10px 0px" :cursor "pointer"}
                               :on-click #(do
                                            (.preventDefault %)
                                            (delete-endorsement id user_badge_id nil nil)
                                            )
                               :data-dismiss "modal"} [:i.fa.fa-trash] (t :badge/DeleteEndorsement)]]
            #_[:div.pull-right.delete-btn
               [:a {:style {:margin "10px 0px" :cursor "pointer"}
                    :on-click #(do
                                 (.preventDefault %)
                                 (delete-endorsement id user_badge_id nil nil)
                                 )
                    :data-dismiss "modal"} [:i.fa.fa-trash] (t :badge/DeleteEndorsement)]
               ]]

           [:div {:style {:margin-top "15px"}}
            [:div {:dangerouslySetInnerHTML {:__html content}}]
            (when (= "pending" status)
              [:div.caption
               [:hr]
               [:div.text-center
                [:button.btn.btn-primary {:href "#"
                                          :on-click #(do
                                                       (.preventDefault %)
                                                       (update-status id "accepted" user_badge_id state init-pending-endorsements)
                                                       )
                                          :data-dismiss "modal"}  (t :badge/Acceptendorsement)]
                [:button.btn.btn-warning.cancel {:href "#"
                                                 :on-click #(do
                                                              (.preventDefault %)
                                                              (update-status id "declined" user_badge_id state init-pending-endorsements))
                                                 :data-dismiss "modal"} (t :badge/Declineendorsement)]
                #_[:ul.list-inline.buttons
                   [:a.button {:href "#"
                               :on-click #(do
                                            (.preventDefault %)
                                            (update-status id "accepted" user_badge_id nil nil)
                                            )
                               :data-dismiss "modal"} [:li [:i.fa.fa-check] ]]
                   [:a.button {:href "#"
                               :on-click #(do
                                            (.preventDefault %)
                                            (update-status id "declined" user_badge_id nil nil ))
                               :data-dismiss "modal"}[:li.cancel [:i.fa.fa-remove]]]]]])
            ])]]])))


(defn endorsements [state]
  (let [endorsements (->> (list* @(cursor state [:received]) @(cursor state [:given]))
                          flatten
                          (sort-by :mtime >))]
    [:div.panel
     [:div.panel-heading
      [:h3
       (str (t :badge/Endorsements) ) [:span.badge {:style {:vertical-align "text-top"}}  (count endorsements)]]]
     [:div.panel-body
      [:div.table  {:summary (t :badge/Endorsements)}
       (reduce (fn [r endorsement]
                 (let [{:keys [id endorsee_id endorser_id profile_picture first_name last_name name image_file content status user_badge_id mtime]} endorsement
                       endorser (str first_name " " last_name)]
                   (conj r [:div.list-item
                            [:a {:href "#" :on-click #(do
                                                        (.preventDefault %)
                                                        (mo/open-modal [:badge :userendorsement] (atom {:endorsement endorsement :state state}) {:hidden (fn [] (init-user-endorsements state))} ))}
                             [:div.media
                              [:div.col-md-12 [:small [:i (date-from-unix-time (* 1000 mtime) "days")]]]
                              #_[:div.row
                                 [:div.labels
                                  [:span.label.label-success (if endorser_id (t :badge/Endorsedyou) (t :badge/Youendorsed))]
                                  (if (= "pending" status) [:span.label.label-info (t :social/Pending)])]
                                 ]

                              [:div.media-left.media-top.list-item-body
                               [:img.main-img.media-object {:src (str "/" image_file)}]
                               ]
                              [:div.media-body
                               [:h4.media-heading.badge-name  name]
                               [:div.media
                                [:div.media-left.media-top
                                 [:img.media-object.small-img {:src (profile-picture profile_picture)}]]
                                [:div.media-body
                                 [:p endorser]
                                 [:div.labels
                                  [:span.label.label-success (if endorser_id (t :badge/Endorsedyou) (t :badge/Youendorsed))]
                                  (if (= "pending" status) [:span.label.label-info (t :social/Pending)])]]
                                ]]
                              ]

                             #_[:div.media
                                [:div.media-left.media-top.list-item-body
                                 [:img.main-img.media-object {:src (profile-picture profile_picture)}]
                                 ]
                                [:div.media-body
                                 [:h4.media-heading  endorser]
                                 [:div.media
                                  [:div.media-left.media-top
                                   [:img.media-object.small-img {:src (str "/" image_file)}]]
                                  [:div.media-body
                                   [:p.badge-name name]
                                   ]]]
                                ]]]))) [:div] endorsements)]]]))

(defn user-endorsements-content [state]
  [:div
   [m/modal-window]
   [:div#badge-stats
    [:h1.uppercase-header (t :badge/Myendorsements)]
    [:div {:style {:margin-bottom "15px"}} (t :badge/Endorsementpageinfo)]
    [:div.manage]

    (if (or (seq @(cursor state [:received]) ) (seq @(cursor state [:given]))) (endorsements state) [:div (t :badge/Youhavenoendorsements)])
    ]])

(defn request-endorsement [])


(defn handler [site-navi]
  (let [state (atom {:initializing true
                     :permission "initial"})
        user (session/get :user)]
    (init-user-endorsements state)
    (fn []
      (cond
        (= "initial" (:permission @state)) [:div]
        (and user (= "error" (:permission @state))) (layout/default-no-sidebar site-navi (err/error-content))
        (= "error" (:permission @state)) (layout/landing-page site-navi (err/error-content))
        :else (layout/default site-navi (user-endorsements-content state)))
      )
    )
  )
