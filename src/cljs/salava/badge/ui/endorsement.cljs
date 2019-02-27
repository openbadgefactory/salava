(ns salava.badge.ui.endorsement
  (:require [salava.core.i18n :refer [t]]
            [reagent.core :refer [atom cursor create-class dom-node]]
            [salava.core.time :refer [unix-time date-from-unix-time]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.helper :refer [path-for private?]]
            [salava.core.ui.modal :as mo]
            [clojure.string :refer [blank?]]
            [reagent.session :as session]
            [salava.user.ui.helper :refer [profile-picture]]
            [reagent-modals.modals :as m]
            [salava.core.ui.layout :as layout]
            [salava.core.ui.error :as err]
            [salava.core.ui.grid :as g]))


(defn endorsement-row [endorsement]
  (let [{:keys [issuer content issued_on]} endorsement]

    [:div {:style {:margin-bottom "20px"}}
     [:h5
      (when (:image_file issuer) [:img {:src (str "/" (:image_file issuer)) :style {:width "65px" :height "auto" :padding "7px"}}])
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
                (when (some #(= (:issuer_id %) (:endorser-id @state)) data)
                  (swap! state assoc :show-link "none"
                         :show-content "none"
                         :show-endorsement-status "block")))}))

(defn user-badge-endorsement-content [badge-id badge-endorsements]
  (let [state (atom {:id badge-id})]
    (init-user-badge-endorsement state)
    (fn []
      (let [endorsements (filter #(= (:status %) "accepted") (:user-badge-endorsements @state))
            badge-endorsements? (pos? (count @badge-endorsements))]
        (when (seq endorsements)
          [:div;.row {:id "badge-contents"}
           ;[:div.col-xs-12
           ;[:h4 {:style {:margin-bottom "20px"}} (t :badge/BadgeEndorsedBy)]
           (if badge-endorsements? [:hr.line])
           [:h4 {:style {:margin-bottom "20px"}} (t :badge/BadgeEndorsedByIndividuals)]
           (reduce (fn [r endorsement]
                     (let [{:keys [id user_badge_id image_file name content first_name last_name profile_picture endorser_id mtime]} endorsement]
                       (conj r [:div {:style {:margin-bottom "20px"}}

                                [:h5
                                 [:img {:src (profile-picture profile_picture) :style {:width "55px" :height "auto" :padding "7px"}}]
                                 [:a {:href "#"
                                      :on-click #(do (.preventDefault %) (mo/set-new-view [:user :profile] {:user-id endorser_id}))
                                      } (str first_name " " last_name)]
                                 " "
                                 [:small (date-from-unix-time (* 1000 mtime))]]
                                [:div {:dangerouslySetInnerHTML {:__html content}}]]
                             ))) [:div] endorsements)]
          ;]
          )))))

(defn badge-endorsement-content [param]
  (let [endorsements (atom [])
        badge-id (if (map? param) (:badge-id param) param)
        user-badge-id (:id param)]
    (init-badge-endorsements endorsements badge-id)
    (fn []
      (let [endorsement-count (count @endorsements)]
        [:div.row {:id "badge-contents"}
         (when (seq @endorsements)
           [:div.col-xs-12
            [:h4 {:style {:margin-bottom "20px"}} (t :badge/BadgeEndorsedByOrganizations)]
            (into [:div]
                  (for [endorsement @endorsements]
                    (endorsement-row endorsement)))])
         (when user-badge-id
           ;(if (seq @endorsements) [:hr])
           [:div.col-xs-12
            [user-badge-endorsement-content user-badge-id endorsements]]
           )]))))



;; User Badge Endorsements

(defn init-user-endorsements [state]
  (ajax/GET
    (path-for (str "/obpv1/badge/user/endorsements"))
    {:handler (fn [data]
                (reset! state (assoc data
                                :initializing false
                                :permission "success"
                                :show "all"
                                :search ""
                                :order "mtime"))
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




(def simplemde-toolbar (array "bold" "italic" "heading-3"
                              "quote" "unordered-list" "ordered-list"
                              "link" "horizontal-rule"
                              "preview"))

(defn init-editor [atom element-id]
  (let [editor (js/SimpleMDE. (js-obj "element" (.getElementById js/document element-id)
                                      "toolbar" simplemde-toolbar
                                      "autosave" "true"
                                      "forceSync" "true"))]

    (js/setTimeout (fn [] (.value editor @atom)) 200) ;;delay for editor to load
    (.codemirror.on editor "change" (fn [] (reset! atom (.value editor))))))

(defn markdown-editor [value]
  (create-class {:component-did-mount #(init-editor value (str "editor" (-> (session/get :user) :id)))
                 :reagent-render (fn []
                                   [:div.form-group
                                    [:textarea {:class "form-control"
                                                :id (str "editor" (-> (session/get :user) :id))
                                                ;:value @atom
                                                :on-change #(reset! value (.-target.value %))
                                                }]]) }))

(defn test-editor [value]
  (let [instance (atom nil)]
    (create-class {:component-did-mount (fn []
                                          (let [editor (js/SimpleMDE. (js-obj "element" (.getElementById js/document (str "editor" (-> (session/get :user) :id)))
                                                                              "toolbar" simplemde-toolbar
                                                                              "autosave" "true"
                                                                              "forceSync" "true"))]
                                            (-> editor
                                                .-codemirror
                                                (.refresh)
                                                #_(.setValue @value))
                                            (.value editor @value)

                                            (.codemirror.on editor "scroll" (fn [] (.value editor @value)))
                                            (.codemirror.on editor "mousedown" (fn [] (.value editor @value)))
                                            (.codemirror.on editor "change" (fn [] (reset!  value (.value editor))))
                                            )

                                          )
                   :component-did-update (fn [this editor]
                                           (prn "TEST"))
                   :reagent-render (fn []
                                     @value
                                     [:div.form-group
                                      [:textarea {:class "form-control"
                                                  :id (str "editor" (-> (session/get :user) :id))
                                                  :defaultValue @value
                                                  :on-change #(reset! value (.-target.value %))
                                                  }]])})))

(defn process-text [s state]
  (let [text (-> js/document
                 (.getElementById (str "editor" (-> (session/get :user) :id)))
                 (.-innerHTML))
        endorsement-claim (str text (if (blank? text) "" "\n\n") "* " s)]
    ; (prn text)
    (reset! (cursor state [:endorsement-comment]) endorsement-claim))
  #_(init-editor (cursor state [:endorsement-comment] (-> (session/get :user) :id))))


#_(defn editor [value-atom]
    (let [cm (atom nil)]
      (create-class
        {:component-did-mount
         (fn [this]
           (let [el (dom-node this)
                 editor (js/SimpleMDE.
                          (clj->js
                            {:toolbar simplemde-toolbar
                             :autofocus    true
                             :spellChecker true
                             :placeholder  ""
                             :forceSync    true
                             :element      @cm
                             :initialValue @value-atom
                             :value        @value-atom
                             }))]

             (reset! cm editor)
             (.codemirror.on editor "change" (fn [] (reset! value-atom (.value editor))))
             #_(.on editor "change"
                    (fn []
                      (let [value (.value editor)]
                        (when-not (= value @value-atom)
                          (reset! value-atom value)))))
             ))

         :component-did-update
         (fn [this old-argv]
           (when-not (= @value-atom (.value cm))
             (.setValue @cm @value-atom)
             ;; reset the cursor to the end of the text, if the text was changed externally
             (let [last-line (.lastLine @cm)
                   last-ch (count (.getLine @cm last-line))]
               (.setCursor @cm last-line last-ch))))

         :reagent-render
         (fn [_ _ _]
           ;@atom
           [:textarea {:id (str "editor" (-> (session/get :user) :id)) :defaultValue @value-atom}])})))



(defn endorse-badge-content [state]

  (fn []
    [:div {:style {:display @(cursor state [:show-content])}}
     [:hr.border]
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
      [:div.editor #_[editor (cursor state [:endorsement-comment])] [test-editor (cursor state [:endorsement-comment])]]
      #_[:textarea.form-control {:id "claim"
                                 :rows "10"
                                 :cols "50"
                                 :max-length "1500"
                                 :type "text"
                                 :value @(cursor state [:endorsement-comment])
                                 :on-change #(do
                                               (reset! (cursor state [:endorsement-comment]) (.-target.value %)))}]]
     [:div
      [:button.btn.btn-primary {:on-click #(do
                                             (.preventDefault %)
                                             (save-endorsement state))
                                :disabled (blank? @(cursor state [:endorsement-comment]))

                                } (t :badge/Endorsebadge)]]
     [:hr.border]]))

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

(defn profile-link-inline [id issuer_name picture]
  [:div [:a {:href "#"
             :on-click #(mo/open-modal [:user :profile] {:user-id id})}
         [:img {:src (profile-picture picture)}]
         (str issuer_name " ")]  (t :badge/Hasendorsedyou)])

(defn pending-endorsements []
  (let [state (atom {:user-id (-> (session/get :user) :id)})]
    (init-pending-endorsements state)
    (fn []
      [:div#endorsebadge
       (reduce (fn [r endorsement]
                 (let [{:keys [id user_badge_id image_file name content profile_picture issuer_id description issuer_name]} endorsement]
                   (conj r
                         [:div
                          [:div.col-md-12
                           [:div.thumbnail
                            [:div.endorser.col-md-12
                             [profile-link-inline issuer_id issuer_name profile_picture id]
                             [:hr.line]
                             ]
                            [:div.caption.row.flip
                             [:div.position-relative.badge-image.col-md-3
                              [:img {:src (str "/" image_file) :style {:padding "15px"}}]]

                             [:div.col-md-9 [:h4.media-heading name]
                              [:div.thumbnail-description.smaller {:dangerouslySetInnerHTML {:__html content}}]]
                             ]

                            [:div.caption.card-footer.text-center
                             [:hr.line]
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
                                [:li {:style {:margin "10px"}} [:a.button {:href "#"
                                                                           :on-click #(do
                                                                                        (.preventDefault %)
                                                                                        (update-status id "accepted" user_badge_id state init-pending-endorsements)
                                                                                        )} [:i.fa.fa-check] (t :badge/Acceptendorsement)]]
                                [:li.cancel {:style {:margin "10px"}}[:a.button {:href "#"
                                                                                 :on-click #(do
                                                                                              (.preventDefault %)
                                                                                              (update-status id "declined" user_badge_id state init-pending-endorsements))} [:i.fa.fa-remove] (t :badge/Declineendorsement)]]]]]]]))) [:div.row] @(cursor state [:pending]))
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
                    (let [{:keys [id user_badge_id image_file name content issuer_name first_name last_name profile_picture endorser_id status]} endorsement]
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
                                   [:hr.line]
                                   [:div.text-center
                                    [:ul.list-inline.buttons.buttons
                                     [:a.button {:href "#"
                                                 :on-click #(do
                                                              (.preventDefault %)
                                                              (update-status id "accepted" user_badge_id state init-user-badge-endorsement)
                                                              )} [:li [:i.fa.fa-check {:title (t :badge/Acceptendorsement)}] ]]
                                     [:a.button {:href "#"
                                                 :on-click #(do
                                                              (.preventDefault %)
                                                              (update-status id "declined" user_badge_id state init-user-badge-endorsement))}[:li.cancel [:i.fa.fa-remove {:title (t :badge/Declineendorsement)}]]]]]])
                                ]]))

                    ) [:div] @(cursor state [:user-badge-endorsements]))]]))))

(defn profile [element-data]
  (let [{:keys [id first_name last_name profile_picture status label issuer_name]} element-data
        current-user (session/get-in [:user :id])]
    [:div.endorsement-profile.panel-default
     [:a {:href "#" :on-click #(mo/open-modal [:user :profile] {:user-id id})}
      [:div.panel-body.flip
       [:div.col-md-4
        [:div.profile-image
         [:img.img-responsive.img-thumbnail
          {:src (profile-picture profile_picture)
           :alt (or issuer_name (str first_name " " last_name))}]]]
       [:div.col-md-8
        [:h4 (or issuer_name (str first_name " " last_name))]
        (when (= status "pending") [:p [:span.label.label-info label]])]]]]))

(defn user-endorsement-content [params]
  (fn []
    (let [{:keys [endorsement state]} @params
          {:keys [id profile_picture name first_name last_name image_file content user_badge_id issuer_id issuer_name endorsee_id status]} endorsement]
      [:div.row.flip {:id "badge-info"}
       [:div.col-md-3
        [:div.badge-image [:img.badge-image {:src (str "/" image_file)}]]]
       [:div.col-md-9
        [:div
         [:h1.uppercase-header name]
         [:div (if endorsee_id (t :badge/Manageendorsementtext1) (t :badge/Manageendorsementtext2))]
         [:hr.line]
         [:div.row
          [:div.col-md-4.col-md-push-8  " "#_(when-not (and endorser_id (= "pending" status))
                                               [:div.row
                                                [:div.col-xs-12.delete-btn {:style {:margin "5px 0px" :cursor "pointer" :width "100%"}}
                                                 [:a {:style {:line-height "4"}
                                                      :on-click #(do
                                                                   (.preventDefault %)
                                                                   (delete-endorsement id user_badge_id nil nil))
                                                      :data-dismiss "modal"} [:i.fa.fa-trash] (t :badge/Deleteendorsement)]
                                                 ]])]
          [:div.col-md-8.col-md-pull-4 [profile {:id (or endorsee_id issuer_id)
                                                 :profile_picture profile_picture
                                                 :first_name first_name
                                                 :last_name last_name
                                                 :issuer_name issuer_name
                                                 :status status
                                                 :label (if issuer_id
                                                          (t :badge/pendingreceived)
                                                          (t :badge/pendinggiven)
                                                          )}]]]

         (if endorsee_id
           [:div {:style {:margin-top "15px"}}
            [:div;.form-group
             [:label {:for "claim"} (str (t :badge/Composeyourendorsement) ":")]
             [:div.editor [markdown-editor (cursor params [:endorsement :content])]]
             #_[:textarea.form-control {:id "claim"
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
             [:button.btn.btn-warning.cancel {:data-dismiss "modal"} (t :core/Cancel)]
             ;[:div.col-xs-12.delete-btn {:style {:margin "10px 0px 10px 0px" :cursor "pointer" :width "100%"}}
             [:a.delete-btn.pull-right {:style {:line-height "4" :cursor "pointer"}
                                        :on-click #(do
                                                     (.preventDefault %)
                                                     (delete-endorsement id user_badge_id nil nil))
                                        :data-dismiss "modal"} [:i.fa.fa-trash] (t :badge/Deleteendorsement)]
             ]]
           ;]

           [:div {:style {:margin-top "15px"}}
            [:div {:dangerouslySetInnerHTML {:__html content}}]
            (when (= "pending" status)
              [:div.caption
               [:hr.line]
               [:div.buttons
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
                ;[:ul.list-inline.buttons
                #_[:ul.list-inline.buttons
                   [:li [:a.button {:href "#"
                                    :on-click #(do
                                                 (.preventDefault %)
                                                 (update-status id "accepted" user_badge_id nil nil)
                                                 )
                                    :data-dismiss "modal"}  [:i.fa.fa-check] (t :badge/Acceptendorsement)]]
                   [:li.cancel [:a.button {:href "#"
                                           :on-click #(do
                                                        (.preventDefault %)
                                                        (update-status id "declined" user_badge_id nil nil ))
                                           :data-dismiss "modal"} [:i.fa.fa-remove] (t :badge/Declineendorsement)]]]]
               ]
              )
            ])]]])))


(defn endorsements [state]
  (let [endorsements (case @(cursor state [:show])
                       "all" @(cursor state [:all-endorsements])
                       "given" @(cursor state [:given])
                       "received" @(cursor state [:received])
                       @(cursor state [:all-endorsements]))
        processed-endorsements (if (blank? @(cursor state [:search]))
                                 endorsements
                                 (filter #(or (re-find (re-pattern (str "(?i)" @(cursor state [:search]))) (:name %))
                                              (re-find (re-pattern (str "(?i)" @(cursor state [:search]))) (:first_name %))
                                              (re-find (re-pattern (str "(?i)" @(cursor state [:search]))) (:last_name %))
                                              (re-find (re-pattern (str "(?i)" @(cursor state [:search]))) (str (:first_name %) " " (:last_name %))))
                                         endorsements))
        order (keyword  @(cursor state [:order]))
        endorsements (case order
                       (:mtime) (sort-by order > processed-endorsements)
                       (:name) (sort-by (comp clojure.string/upper-case str order) processed-endorsements)
                       (:user) (sort-by #(str (:first_name %) " " (:last_name %)) processed-endorsements)
                       processed-endorsements
                       )]

    [:div.panel
     [:div.panel-heading
      [:h3
       (str (t :badge/Endorsements) ) #_[:span.badge {:style {:vertical-align "text-top"}}  (count processed-endorsements)]]
      [:br]
      [:div (case @(cursor state [:show])
              "all" (t :badge/Allendorsementstext)
              "given" (t :badge/Givenendorsementstext)
              "received" (t :badge/Receivedendorsementstext)
              (t :badge/Allendorsementstext))]
      ]
     [:div.panel-body
      [:div.table  {:summary (t :badge/Endorsements)}
       (reduce (fn [r endorsement]
                 (let [{:keys [id endorsee_id issuer_id profile_picture issuer_name first_name last_name name image_file content status user_badge_id mtime]} endorsement
                       endorser (or issuer_name (str first_name " " last_name))]
                   (conj r [:div.list-item.row.flip
                            [:a {:href "#" :on-click #(do
                                                        (.preventDefault %)
                                                        (mo/open-modal [:badge :userendorsement] (atom {:endorsement endorsement :state state}) {:hidden (fn [] (init-user-endorsements state))} ))}
                             [:div.col-md-4.col-md-push-8 [:small.pull-right [:i (date-from-unix-time (* 1000 mtime) "days")]]]
                             [:div.col-md-8.col-md-pull-4 [:div.media
                                                           [:div;.row
                                                            [:div.labels
                                                             (if issuer_id
                                                               [:span.label.label-success (t :badge/Endorsedyou)]
                                                               [:span.label.label-primary (t :badge/Youendorsed)])
                                                             (if (= "pending" status)
                                                               [:span.label.label-info
                                                                (if issuer_id
                                                                  (t :badge/pendingreceived)
                                                                  (t :badge/pendinggiven)
                                                                  )])]
                                                            ]
                                                           [:div.media-left.media-top.list-item-body
                                                            [:img.main-img.media-object {:src (str "/" image_file)}]
                                                            ]
                                                           [:div.media-body
                                                            [:h4.media-heading.badge-name  name]
                                                            [:div.media
                                                             [:div.child-profile [:div.media-left.media-top
                                                                                  [:img.media-object.small-img {:src (profile-picture profile_picture)}]]
                                                              [:div.media-body
                                                               [:p endorser]]
                                                              ]]]]]]]))) [:div] endorsements)]]]))
(defn order-opts []
  [{:value "mtime" :id "radio-date" :label (t :badge/bydate)}
   {:value "name" :id "radio-name" :label (t :core/byname)}
   {:value "user" :id "radio-issuer" :label (t :badge/byuser)}])

(defn user-endorsements-content [state]
  [:div
   [m/modal-window]
   [:div#badge-stats
    #_[:h1.uppercase-header (t :badge/Myendorsements)]
    #_[:div {:style {:margin-bottom "15px"}} (t :badge/Endorsementpageinfo)]

    (if (or (seq @(cursor state [:received]) ) (seq @(cursor state [:given])))
      [:div
       [:div.form-horizontal {:id "grid-filter"}
        [g/grid-search-field (t :core/Search ":")  "endorsementsearch" (t :badge/Filterbybadgenameoruser) :search state]
        [:div.form-group.wishlist-buttons
         [:legend {:class "control-label col-sm-2"} (str (t :core/Show) ":")]
         [:div.col-md-10
          [:div.buttons
           [:button {:class (str "btn btn-default " (when (= "all" @(cursor state [:show])) "btn-active"))
                     :id "btn-all"
                     :on-click #(do (swap! state assoc :show "all"))}
            (t :core/All)]
           [:button {:class (str "btn btn-default " (when (= "received" @(cursor state [:show])) "btn-active"))
                     :id "btn-all"
                     :on-click #(do (swap! state assoc :show "received"))}
            (t :badge/Endorsedme)]
           [:button {:class (str "btn btn-default " (when (= "given" @(cursor state [:show])) "btn-active"))
                     :id "btn-all"
                     :on-click #(do (swap! state assoc :show "given"))}
            (t :badge/Iendorsed)]]] ]
        [g/grid-radio-buttons (t :core/Order ":") "order" (order-opts) :order state]]

       (endorsements state)]
      [:div (t :badge/Youhavenoendorsements)])]])

(defn request-endorsement [])


(defn handler [site-navi]
  (let [state (atom {:initializing true
                     :permission "initial"
                     :order :mtime})
        user (session/get :user)]
    (init-user-endorsements state)
    (fn []
      (cond
        (= "initial" (:permission @state)) [:div]
        (and user (= "error" (:permission @state))) (layout/default-no-sidebar site-navi (err/error-content))
        (= "error" (:permission @state)) (layout/landing-page site-navi (err/error-content))
        :else (layout/default site-navi (user-endorsements-content state)))
      )))
