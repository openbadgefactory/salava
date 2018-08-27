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
            [clojure.string :refer [capitalize lower-case upper-case blank?]]))

(defn export-data-to-pdf [state]
  (let [user-id (:user-id @state)]
    (ajax/GET
      (path-for (str "obpv1/user/export-to-pdf/" user-id))
      {:handler (js-navigate-to (str "obpv1/user/export-to-pdf/" user-id))})))

(defn email-address-table [email]
  [:table.table.hidden-xs
   [:thead
    [:tr
     [:th (t :user/Email)]
     [:th (t :user/verified)]
     [:th (t :user/Loginaddress)]
     [:th (t :user/BackpackID)]]]
   [:tbody
    (doall
      (for [e email
            :let [{:keys [email verified primary_address backpack_id]} e]]

        ^{:key e}[:tr
                  [:td email]
                  [:td.text-center (if verified [:i {:class "fa fa-check"}])]
                  [:td (if primary_address [:i {:class "fa fa-check"}])]
                  [:td backpack_id]]
        ))]])

(defn email-address-table-mobile [email]
  [:table.table.visible-xs
   [:thead
    [:tr
     [:th (t :user/Email)]]]
   [:tbody
    (doall
      (for [e email
            :let [{:keys [email verified primary_address backpack_id]} e]]
        ^{:key e}[:tr
                  [:td email
                   [:p (t :user/verified) ": " (if verified [:i {:class "fa fa-check"}])]
                   (if backpack_id
                     [:p (t :user/BackpackID) ": "  [:i backpack_id ]])
                   (if primary_address
                     [:p (t :user/Loginaddress) ": " [:i {:class "fa fa-check"}]])]]
        ))]])

(defn content [state]
  (let [{user_followers :user_followers user_following :user_following pending_badges :pending_badges connections :connections events :events user_files :user_files user_badges :user_badges
         user_pages :user_pages owner? :owner? {id :id first_name :first_name last_name :last_name profile_picture :profile_picture about :about role :role language :language
                                                private :private activated? :activated country :country
                                                email_notifications :email_notifications
                                                profile_visibility :profile_visibility} :user email :emails

         profile :profile user-id :user-id} @state
        fullname (str first_name " " last_name)
        site-url (session/get :site-url)]

    [:div {:id "my-data"}
     [:h1.uppercase-header (t :user/Mydata)]
     [:div
      [:p (str (t :user/Deleteinstruction) " " (t :user/Todeletedata) " ") [:a {:href (path-for "/user/cancel")} (str (t :user/Removeaccount) ".")]]
      ]
     (if (:initializing @state)
       [:div.ajax-message
        [:i {:class "fa fa-cog fa-spin fa-2x "}]
        [:span (str (t :core/Loading) "...")]]
       [:div.panel {:id "profile"}
        [m/modal-window]

        [:div.panel-body
         [:div {:id "page-buttons-share"}
          [:div {:id "buttons"
                 :class "text-right"}

           [:button {:class "btn btn-primary"
                     :on-click #(export-data-to-pdf state)}
            (t :user/Exportdata)]]]

         [:div.row.flip
          (when-not (= "-" profile_picture) [:div {:class "col-md-3 col-sm-3 col-xs-12 "}
                                             [:div {:class "profile-picture-wrapper"}
                                              [:img.profile-picture {:src (profile-picture profile_picture)
                                                                     :alt fullname}]]])
          [:div {:class "col-md-9 col-sm-9 col-xs-12"}
           [:div {:id "profile-data" :class "row"}
            [:div.col-xs-12 [:b (t :user/UserID)": "] id]
            [:div.col-xs-12 [:b (str (t :user/Role) ": ")] (if (= "user" role) (t :social/User) (t :admin/Admin))]
            [:div.col-xs-12 [:b (str (t :user/Firstname)": ")] (capitalize first_name)]
            [:div.col-xs-12 {:style {:margin-bottom "20px"}} [:b (str (t :user/Lastname)": ")] (capitalize last_name)]
            [:div
             [:div.col-xs-12 [:b (t :user/Aboutme) ":"]]
             [:div.col-xs-12 {:style {:margin-bottom "20px"}} about]]
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

            [:div.col-xs-12 [:b (t :user/Language)": "] (upper-case language)]
            [:div.col-xs-12 [:b (t :user/Country)": "] country]
            [:div.col-xs-12 [:b (str (t :user/Emailnotifications) ": ")]  (if (true? email_notifications) (t :core/Yes) (t :core/No))]
            [:div.col-xs-12 [:b (str (t :user/Privateprofile) ": ")] (if (true? private) (t :core/Yes) (t :core/No))]
            [:div.col-xs-12 [:b (str (t :user/Activated) ": ")] (if (true? activated?) (t :core/Yes) (t :core/No))]
            [:div.col-xs-12 {:style {:margin-bottom "20px"}} [:b (str (t :user/Profilevisibility) ": ")] (t (keyword (str "core/"(capitalize profile_visibility))))]]]]

         [:div {:class "row col-md-12 col-sm-12 col-xs-12 th-align" :style {:margin-bottom "10px"}}
          [:h2 {:class "uppercase-header"} [:a {:href (path-for "/user/edit/email-addresses")} (str (if (empty? (rest email)) (t :user/Email) (t :user/Emailaddresses)))]]
          (email-address-table email)
          (email-address-table-mobile email)]

         [:div {:class "row col-md-12 col-sm-12 col-xs-12" :style {:margin-bottom "20px"}}
          [:h2 {:class "uppercase-header"} [:a {:href (path-for "/badge")}(str (t :badge/Mybadges) ": ")(count user_badges)]]
          (if (not-empty user_badges)
            (doall
              (for [b user_badges]
                ^{:key b}[:div.col-xs-12 [:a {:href "#"
                                              :on-click #(do
                                                           (.preventDefault %)
                                                           (mo/open-modal [:gallery :badges] {:badge-id (:badge_id b)})
                                                           )} (:name b)]])))]
         [:div {:class "row col-md-12 col-sm-12 col-xs-12"}
          (if-not (empty? pending_badges)
            [:div
             [:h3 [:a {:href (path-for "/social/stream")} (str (t :badge/Pendingbadges) ": ") (count pending_badges)]]
             (doall
               (for [p pending_badges]
                 ^{:key p}[:div
                           [:div.col-xs-12 [:b (str (t :badge/Name) ": ")] (:name p)]
                           [:div.col-xs-12 [:b (str (t :page/Description) ": ")] (:description p)]
                           ]))])]

         [:div {:class "row col-md-12 col-sm-12 col-xs-12"}
          [:h2 {:class "uppercase-header"} [:a {:href (path-for "/page")} (str (t :page/Mypages) ": ") (count user_pages)]]
          (if (not-empty user_pages)
            (doall
              (for [p user_pages]
                ^{:key p}[:div.col-xs-12 [:a {:href "#"
                                              :on-click #(do
                                                           (.preventDefault %)
                                                           (mo/open-modal [:page :view] {:page-id (:id p)})
                                                           )} (:name p)]])))]

         [:div {:class "row col-md-12 col-sm-12 col-xs-12"}
          [:h2 {:class "uppercase-header"} [:a {:href (path-for "/page/files")} (str (t :file/Files) ": ") (count user_files)]]
          (if (not-empty user_files)
            (doall
              (for [f user_files]
                ^{:key f}[:div.col-xs-12 [:a {:href  (str "/" (:path f)) :target "_blank"}
                                          (:name f)]])))]


         [:div {:class "row col-md-12 col-sm-12 col-xs-12"}
          [:h1 {:class "uppercase-header" :style {:text-align "center"}} (t :user/Activity) ]
          (if (> connections 0)
            [:div
             [:h2 {:class "uppercase-header"} [:a {:href (path-for "/social/connections")} (str (t :user/Badgeconnections) ": ")  connections]]])
          (if (or (not-empty user_following) (not-empty user_followers))
            [:div
             [:h2 {:class "uppercase-header"} (str (t :user/Socialconnections) ": ") (+ (count user_followers) (count user_following))]
             (if (not-empty user_followers)
               [:div
                [:h3 (str (t :social/Followersusers) ": ")]
                (for [follower user_followers
                      :let [id (:owner_id follower)
                            fname (:first_name follower)
                            lname (:last_name follower)
                            status (:status follower)]]
                  ^{:key follower}[:div
                                   [:div.col-xs-12 {:style {:margin-bottom "20px"}} [:div {:id "inline"} [:b (str (t :badge/Name) ": ")] [:a {:href "#" :on-click #(mo/open-modal [:user :profile] {:user-id (:owner_id follower)})} (str (:first_name follower) " " (:last_name follower) )] ", "[:b (str (t :user/Status) ": ")] (t (keyword (str "social/"(:status follower))))]]
                                   #_[:div.col-xs-12 {:style {:margin-bottom "20px"}} [:b (str (t :user/Status) ": ")] (t (keyword (str "social/"(:status follower))))]
                                   ])
                [:br]])

             (when-not (empty? user_following)
               [:div
                [:h3 (str (t :social/Followedusers) ": ")]
                (for [f user_following
                      :let [fid (:user_id f)
                            fname (:first_name f)
                            lname (:last_name f)
                            status (:status f)]]
                  ^{:key f}[:div
                            [:div.col-xs-12 {:style {:margin-bottom "20px"}} [:div {:id "inline"}  [:b (str (t :badge/Name) ": ")] [:a {:href "#" :on-click #(mo/open-modal [:user :profile] {:user-id (:user_id f)})} (str (:first_name f) " " (:last_name f) )] ", " [:b (str (t :user/Status) ": ")] (t (keyword (str "social/"(:status f))))]]
                            ])]
               )])

          (if (not-empty events)
            [:div.th-align
             [:h2 {:class "uppercase-header"} (str (t :user/Activityhistory) ": ") (count events)]
             [:table.table
              [:thead
               [:tr
                [:th (t :social/Action)]
                [:th (t :social/Object)]
                [:th (t :badge/Name)]
                [:th (t :social/Created)]]]
              [:tbody
               (doall
                 (for [e (reverse events)]
                   ^{:key e}[:tr
                             [:td [:div (t (keyword (str "social/"(:verb e))))]]
                             [:td [:div (case (:type e)
                                          "page" (t :social/Emailpage)
                                          "badge" (t :social/Emailbadge)
                                          "user" (lower-case (t :social/User))
                                          "admin" (lower-case (t :admin/Admin))
                                          "-")]]
                             [:td [:div (case (str (:verb e)(:type e))
                                          "publishpage"  (or (get-in e [:info :object_name]) "-")
                                          "unpublishpage" (or (get-in e [:info :object_name]) "-")
                                          "publishbadge" (or (get-in e [:info :object_name]) "-")
                                          "unpublishbadge" (or (get-in e [:info :object_name]) "-")
                                          "messagebadge" [:div {:style {:max-width "550px"}}
                                                          (or (get-in e [:info :object_name]) "-")"\n"
                                                          [:br][:br]
                                                          [:p [:i (or (get-in e [:info :message :message]) "comment-removed")]]]
                                          "congratulatebadge" (or (get-in e [:info :object_name]) "-")
                                          "followbadge" (or (get-in e [:info :object_name]) "-")
                                          "followuser" (or (get-in e [:info :object_name]) "-")
                                          "ticketadmin" (or (get-in e [:info :object_name]) "-")
                                          "-")]]
                             [:td [:div (date-from-unix-time (* 1000 (:ctime e)))]]]
                   ))]]])
          ]]])]))

(defn init-data [user-id state]
  (ajax/GET
    (path-for (str "obpv1/user/data/" user-id) true)
    {:handler (fn [data]
                (reset! state (assoc data
                                :user-id user-id))
                (swap! state assoc :initializing false))}
    (fn [] (swap! state assoc :permission "error"))))

(defn handler [site-navi params]
  (let [user-id (:user-id params)
        state (atom {:user-id user-id
                     :password ""
                     :confirm-delete "false"
                     :error-message nil
                     :initializing true})]
    (init-data user-id state)

    (fn []
      (layout/default site-navi (content state)))))
