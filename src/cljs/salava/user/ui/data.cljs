(ns salava.user.ui.data
  (:require [salava.core.ui.layout :as layout]
            [salava.core.ui.ajax-utils :as ajax]
            [reagent.session :as session]
            [salava.core.ui.helper :refer [path-for navigate-to js-navigate-to hyperlink]]
            [salava.core.time :refer [unix-time date-from-unix-time]]
            [salava.core.helper :refer [dump]]
            [reagent-modals.modals :as m]
            [salava.user.ui.helper :refer [profile-picture]]
            [salava.core.i18n :refer [t]]
            [salava.user.schemas :refer [contact-fields]]
            [reagent.core :refer [atom cursor]]
            [salava.user.ui.profile :as profile]
            [salava.core.ui.modal :as mo]
            [salava.core.i18n :refer [t translate-text]]
            [salava.user.ui.cancel :as c]))

(defn export-data-to-pdf [state]
  (let [user-id (:user-id @state)]
    (ajax/GET
      (path-for (str "obpv1/user/export-to-pdf/" user-id))
      {:handler (js-navigate-to (str "obpv1/user/export-to-pdf/" user-id))})))

(defn delete-data-modal [state]
  (let [password-atom (cursor state [:password])]
    [:div
     [:div.modal-body
      [:div.row
       [:div.col-md-12
        [:button {:type  "button"
                  :class  "close"
                  :data-dismiss "modal"
                  :aria-label   "OK"
                  }
         [:span {:aria-hidden  "true"
                 :dangerouslySetInnerHTML {:__html "&times;"}}]]]]
      [:div
            (if (:error-message @state)
       [:div {:class "alert alert-warning" :role "alert"}
        (translate-text (:error-message @state))])
       [:p "All your data will be erased and you will be automatically logged out of the system!"]
       [:form {:class "form-horizontal"}
        [:div.form-group
         [:div.col-md-12
          [:label {
                   :for "input-password"}
           (t :user/Password)
           [:span.form-required " *"]]]
         [:div.col-md-12
          [:input {:class       "form-control"
                   :id          "input-password"
                   :type        "password"
                   :name        "password"
                   :read-only   true
                   :on-change   #(reset! password-atom (.-target.value %))
                   :on-focus    #(.removeAttribute (.-target %) "readonly")
                   :placeholder "To delete data input password" #_(t :user/Tocancelaccountenterpassword)
                   :value       @password-atom}]]

         [:fieldset {:id "export-pdf" :class "col-md-12 checkbox"}
          [:div.col-md-12 [:label
                           [:input {:type     "checkbox"
                                    :on-change (fn [e]
                                                 (if (.. e -target -checked)
                                                   (swap! state assoc :confirm-delete "true")(swap! state assoc :confirm-delete "false")
                                                   ))}]
                           "Are you sure you want to delete data?" #_(t :badge/ExportAllLang)]]]]]]]
     [:div.modal-footer
      [:button {:type         "button"
                :class        "btn btn-primary"
                :data-dismiss "modal"}
       (t :core/Close)]
      [:button {:type         "button"
                :class        "btn btn-warning"
                :disabled (if-not (and (c/password-valid? @password-atom) (= "true" (:confirm-delete @state)))
                            "disabled")
                :on-click   #(do
                               (swap! state assoc :password @password-atom)
                               (if (= "true" (:confirm-delete @state))
                                 (c/cancel-account state)))
                }
       "Delete"]]]))

(defn content [state]
  (let [{user_followers :user_followers user_following :user_following pending_badges :pending_badges connections :connections events :events user_files :user_files user_badges :user_badges
         user_pages :user_pages owner? :owner? {id :id first_name :first_name last_name :last_name profile_picture :profile_picture about :about role :role language :language
                                                 private :private activated? :activated country :country
                                                 email_notifications :email_notifications
                                                 profile_visibility :profile_visibility} :user email :emails

         profile :profile user-id :user-id} @state
         fullname (str first_name " " last_name)
         site-url (session/get :site-url)
         ]
    [:div {:id "cancel-account"}
     [:h1.uppercase-header "My DATA"]
     [:div
      [:p "Below is a summary of your personal data. For a more extensive data archive,
       click export below. A pdf document containing your data will downloaded to your computer "]]

     [:div.panel {:id "my-data"}
      [m/modal-window]
      [:div.panel-body
       [:div {:id "page-buttons-share"}
        [:div {:id "buttons"
               :class "text-right"}
         [:button {:class "btn btn-warning"
                   :on-click #(do
                                (.preventDefault %)
                                (swap! state assoc :confirm-delete "false")
                                (m/modal![delete-data-modal state] {:size :lg}))}
          "Delete data!" #_(t :badge/Export)]
         [:button {:class "btn btn-primary"
                   :on-click #(export-data-to-pdf state)}
          (t :badge/Export)]]]

       [:div.row
        (when profile_picture [:div {:class "col-md-3 col-sm-3 col-xs-12"}
                               [:div.profile-picture-wrapper
                                [:img.profile-picture {:src (profile-picture profile_picture)
                                                       :alt fullname}]]])
        [:div {:class "col-md-9 col-sm-9 col-xs-12"}
         [:div.row
          [:div.col-xs-12 [:b "ID: "] id]
          [:div.col-xs-12 [:b "Role: "] role]
          [:div.col-xs-12 [:b "FirstName: "] first_name]
          [:div.col-xs-12 {:style {:margin-bottom "20px"}} [:b "LastName: "] last_name]
          (if (not-empty about)
            [:div
             [:div.col-xs-12 [:b (t :user/Aboutme) ":"]]
             [:div.col-xs-12 {:style {:margin-bottom "20px"}} about]])
          (if (not-empty profile)
            [:div {:style {:margin-top "20px"}}
             [:div.col-xs-12 [:b (t :user/Contactinfo) ":"]]
             [:div.col-xs-12
              [:table.table
               (into [:tbody]
                     (for [profile-field (sort-by :order profile)
                           :let [{:keys [field value]} profile-field
                                 key (->> contact-fields
                                          (filter #(= (:type %) field))
                                          first
                                          :key)]]
                       [:tr
                        [:td.profile-field (t key) ":"]
                        [:td (cond
                               (or (re-find #"www." (str value)) (re-find #"^https?://" (str value)) (re-find #"^http?://" (str value))) (hyperlink value)
                               (and (re-find #"@" (str value)) (= "twitter" field)) [:a {:href (str "https://twitter.com/" value) :target "_blank" } (t value)]
                               (and (re-find #"@" (str value)) (= "email" field)) [:a {:href (str "mailto:" value)} (t value)]
                               (and  (empty? (re-find #" " (str value))) (= "facebook" field)) [:a {:href (str "https://www.facebook.com/" value) :target "_blank" } (t value)]
                               (= "twitter" field) [:a {:href (str "https://twitter.com/" value) :target "_blank" } (t value)]
                               (and  (empty? (re-find #" " (str value))) (= "pinterest" field)) [:a {:href (str "https://www.pinterest.com/" value) :target "_blank" } (t value)]
                               (and  (empty? (re-find #" " (str value))) (= "instagram" field)) [:a {:href (str "https://www.instagram.com/" value) :target "_blank" } (t value)]
                               (= "blog" field) (hyperlink value)
                               :else (t value))]]))]]])

          [:div.col-xs-12 [:b "Language: "] language]
          [:div.col-xs-12 [:b "Country: "] country]
          [:div.col-xs-12 [:b "Email addresses:  "] (count email)]
          (for [e email]
            ^{:key e}[:div
             [:div.col-xs-12 [:b "Email: "] (:email e)]
             [:div.col-xs-12 [:b "Verified: "] (str (:verified e))]
             (if (true? (:primary_address e)) [:div.col-xs-12 [:b "Primary address: "] (str (:primary_address e))])
             (if-not (empty? (:backpack_id e)) [:div.col-xs-12 [:b "Backpack ID: " ](str (:backpack_id e))])
             ]
            )
          [:div.col-xs-12 [:b "Email Notifications: "] (str email_notifications)]
          [:div.col-xs-12 [:b "Private profile?: "] (str private)]
          [:div.col-xs-12 [:b "Activated? : "] (str activated?)]
          [:div.col-xs-12 [:b "Profile Visibility : "] profile_visibility]]

         [:div {:style {:margin-bottom "20px"}}
          [:h2 {:class "uppercase-header"} "Badges:" (count user_badges)]
          (if (not-empty user_badges)
            (for [b user_badges]
              ^{:key b}[:div.col-xs-12 [:a {:href "#"
                                   :on-click #(do
                                                (.preventDefault %)
                                                (mo/open-modal [:gallery :badges] {:badge-id (:badge_id b)})
                                                )} (:name b)]]
              )
            )]
         [:div
          (if-not (empty? pending_badges)
            [:div
             [:h3  "Pending Badges: "]
             (for [p pending_badges]
               ^{:key p}[:div
                [:div.col-xs-12 [:b "Badge ID: "] (str (:badge_id p))]
                [:div.col-xs-12 [:b "Name: "] (:name p)]
                [:div.col-xs-12 [:b "Description: "] (:description p)]
                [:div.col-xs-12 [:b "Image file: "] (str site-url "/" (:image_file p))]
                [:div.col-xs-12 [:b "Assertion url: "] (:assertion_url p)]
                [:div.col-xs-12 [:b "Visibility: "] (str (:visibility p))]
                [:div.col-xs-12 [:b "Issued on: "] (date-from-unix-time (* 1000 (:issued_on p)))]
                (when (:expires_on p) [:div.col-xs-12 [:b "Expires on: "] (date-from-unix-time (* 1000 (:expires_on p)))])
                ]
               )
             ])]
         [:div {:class "col-md-12 col-sm-9 col-xs-12"}
          [:h2 {:class "uppercase-header"} "Pages:" (count user_pages)]
          (if (not-empty user_pages)
            (for [p user_pages]
              ^{:key p}[:div.col-xs-12 [:a {:href "#"
                                   :on-click #(do
                                                (.preventDefault %)
                                                (mo/open-modal [:page :view] {:page-id (:id p)})
                                                )} (:name p)]]
              )
            )]

         [:div {:class "col-md-12 col-sm-9 col-xs-12"}
          [:h2 {:class "uppercase-header"} "Files:" (count user_files)]
          (if (not-empty user_files)
            (for [f user_files]
              ^{:key f}[:div.col-xs-12 [:a {:href  (str "/" (:path f)) :target "_blank"}
                               (:name f)]]))]


         [:div {:class "col-md-12"}
          [:h1 {:class "uppercase-header"} "Activity:" ]
          (if (not-empty connections)
            [:div
             [:h2 {:class "uppercase-header"} "Badge Connections:" (count connections)]
             (for [c connections]
               ^{:key c}[:div {:style {:margin-top "20px"}}
                ;;            [:div.col-xs-12 [:b "ID: "] (:id c)]
                [:div.col-xs-12 [:b "Name: "] (:name c)]
                [:div.col-xs-12 {:style {:margin-bottom "20px"}} [:b "Description: "] (:description c)]
                ])])
          (if (or (not-empty user_following) (not-empty user_followers))
            [:div
             [:h2 {:class "uppercase-header"} "Social Connections: " (+ (count user_followers) (count user_following))]
             (when-not (empty? user_followers)
               [:div
                [:h3 "Followers: "]
                (for [follower user_followers
                      :let [id (:owner_id follower)
                            fname (:first_name follower)
                            lname (:last_name follower)
                            status (:status follower)]]
                  ^{:key follower}[:div
                   [:div.col-xs-12 [:b "Name: "] [:a {:href "#" :on-click #(mo/open-modal [:user :profile] {:user-id id})} (str fname " " lname )]]
                   [:div.col-xs-12 {:style {:margin-bottom "20px"}} [:b "Status: "] status]
                   ])
                [:br]])
             (when-not (empty? user_following)
               [:div
                [:h3 "Following: "]
                (for [f user_following
                      :let [fid (:user_id f)
                            fname (:first_name f)
                            lname (:last_name f)
                            status (:status f)]]
                  ^{:key f}[:div
                   [:div.col-xs-12 [:b "Name: "] [:a {:href "#" :on-click #(mo/open-modal [:user :profile] {:user-id fid})} (str fname " " lname )]]
                   [:div.col-xs-12 [:b "Status: "] status]
                   ]
                  )])])

          (if (not-empty events)
            [:div
             [:h2 {:class "uppercase-header"} "Events:" (count events)]
             (for [e (reverse events)]
               ^{:key e}[:div {:style {:margin-top "20px"}}
                ;;            (if ())
                [:div.col-xs-12 [:b "Event id: "] (:id e)]
                [:div.col-xs-12 [:b "Action: "] (:verb e)]
                [:div.col-xs-12 [:b "Object Type: "] (or (:type e) (:report_type e))]
                #_(case (:verb e)
                    "message" [:div.col-xs-12 [:b "Message: "] (get-in e [:message :message])]
                    "follow"  (if (= "badge" (:type e))
                                [:div.col-xs-12 [:b "Name: "] (:name e) ]
                                [:div.col-xs-12 [:b "Name: "] [:a {:href "#" :on-click #(mo/open-modal [:user :profile] {:user-id (:object e)})} (str (:o_first_name e) " " (:o_last_name e))]]
                                )
                    "badge" [:div.col-xs-12 [:b "Name: "] (:name e)]
                    "ticket" [:div
                              [:div.col-xs-12 [:b "Reported by: "] (:item_name e)]
                              ]
                    "publish" [:div
                               [:div.col-xs-12 [:b "Name: " (:name e)] ]
                               [:div.col-xs-12 [:b "Image file: "] (str (:image_file e))]
                               ])


                #_(if (= (:type e) "user") [:div.col-xs-12 [:b "Name: "] [:a {:href "#" :on-click #(mo/open-modal [:user :profile] {:user-id (:object e)})} (str (:o_first_name e) " " (:o_last_name e))]] [:div.col-xs-12 [:b "Name: "] (:name e)])
                #_(if (= (:verb e) "message")
                    [:div.col-xs-12 [:b "Message: "] (get-in e [:message :message])]
                    ;;              [:div.col-xs-12 [:b "Message: "] (get-in e [:message :message])]
                    )
                (when (> (:last_checked e) (:ctime e))
                  [:div.col-xs-12 [:b "Last Checked: "] (date-from-unix-time (* 1000 (:last_checked e)))])

                [:div.col-xs-12  {:style {:margin-bottom "20px"}}[:b "Created: "] (date-from-unix-time (* 1000 (:ctime e)))]

                ]
               )])

          ]]

        #_[:div {:class "col-md-12 col-sm-9 col-xs-12"}
           (if (not-empty user_badges)
             [:div
              [:h2 {:class "uppercase-header"} "Badges:" (count user_badges)]
              [:div {:id "user-badges"}

               [profile/badge-grid user_badges @badge-small-view]
               (if (< 6 (count user_badges))
                 [:div [:a {:href "#" :on-click #(reset! badge-small-view (if @badge-small-view false true))}  (if @badge-small-view (t :admin/Showless) (t :user/Showmore))]])]])
           ]

        #_[:div {:class "col-md-12 col-sm-9 col-xs-12"}
           (if (not-empty user_pages)
             [:div {:id "user-pages"}
              [:h2 {:class "uppercase-header user-profile-header"} "Pages:" (count user_pages)#_(t :user/Recentpages)]
              [profile/page-grid user_pages profile_picture @page-small-view]
              (if (< 6 (count user_pages))
                [:div [:a {:href "#" :on-click #(reset! page-small-view (if @page-small-view false true))}  (if @page-small-view (t :admin/Showless) (t :user/Showmore))]])])
           ]

        ]]

      ]]))

(defn init-data [user-id state]
  (ajax/GET
    (path-for (str "obpv1/user/data/" user-id) true)
    {:handler (fn [data]
                (reset! state (assoc data
                                :user-id user-id)))}
    (fn [] (swap! state assoc :permission "error"))))

(defn handler [site-navi params]
  (let [user-id (:user-id params)
        state (atom {:user-id user-id
                     :password ""
                     :confirm-delete "false"
                     :error-message nil})]
    (init-data user-id state)

    (fn []
      (layout/default site-navi (content state)))))
