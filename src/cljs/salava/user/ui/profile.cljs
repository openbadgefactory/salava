(ns salava.user.ui.profile
  (:require [reagent.core :refer [atom cursor]]
            [reagent.session :as session]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.layout :as layout]
            [salava.core.ui.share :as s]
            [salava.core.ui.helper :refer [path-for hyperlink private? not-activated? plugin-fun]]
            [salava.user.schemas :refer [contact-fields]]
            [salava.user.ui.helper :refer [profile-picture]]
            [salava.core.i18n :refer [t]]
            [salava.core.time :refer [date-from-unix-time]]
            [salava.core.helper :refer [dump]]
            [salava.core.ui.modal :as mo]
            [reagent-modals.modals :as m]
            [salava.core.ui.error :as err]
            [salava.admin.ui.admintool :refer [admintool]]
            [salava.admin.ui.reporttool :refer [reporttool1]]
            ))



(defn toggle-visibility [visibility-atom]
  (ajax/POST
     (path-for "/obpv1/user/profile/set_visibility")
     {:params  {:visibility (if (= "internal" @visibility-atom) "public" "internal")}
      :handler (fn [new-value]
                 (reset! visibility-atom new-value)
                 )}))

(defn profile-visibility-input [visibility-atom]
  [:div.col-xs-12
   [:div.checkbox
    [:label
     [:input {:name      "visibility"
              :type      "checkbox"
              :on-change #(toggle-visibility visibility-atom)
              :checked   (= "public" @visibility-atom)}]
     (t :user/Publishandshare)]]])

(defn badge-grid-element [element-data]
  (let [{:keys [id image_file name description issuer_content_name issuer_content_url]} element-data]
    ;[:div {:class "col-xs-12 col-sm-6 col-md-4" :key id}
     [:div {:class "media grid-container"}
      [:div.media-content
       (if image_file
         [:div.media-left
          [:a{:href "#"
              :on-click #(do
                           (mo/open-modal [:badge :info] {:badge-id id})
                                        ;(b/open-modal id false init-data state)
                           (.preventDefault %)) }
            [:img {:src (str "/" image_file)
                 :alt name}]]])
       [:div.media-body
        [:div.media-heading
         [:a.heading-link {:href "#"
                           :on-click #(do
                                        (mo/open-modal [:badge :info] {:badge-id id})
                                        ;(b/open-modal id false init-data state)
                                        (.preventDefault %)) }
          name]]
        [:div.media-issuer
         [:p issuer_content_name]]]]]
              ;]
              ))

(defn page-grid-element [element-data profile_picture]
  (let [{:keys [id name first_name last_name badges mtime]} element-data
        badges (take 4 badges)]
    [:div {:class "col-xs-12 col-sm-6 col-md-4" :key id}
     [:div {:class "media grid-container"}
      [:div.media-content
       [:div.media-body
        [:div.media-heading
         [:a.heading-link  {:href "#" :on-click #(mo/open-modal [:page :view] {:page-id id})}  name]]
        [:div.media-content
         [:div.page-owner
          [:a {:href "#"} first_name " " last_name]]
         [:div.page-create-date
          (date-from-unix-time (* 1000 mtime) "minutes")]
         (into [:div.page-badges]
               (for [badge badges]
                 [:img {:title (:name badge)
                        :alt (:name badge)
                        :src (str "/" (:image_file badge))}]))]]
       [:div {:class "media-right"}
        [:img {:src (profile-picture profile_picture)
               :alt (str first_name " " last_name)}]]]]]))

(defn badge-grid [badges badge-small-view]
  (into [:div {:class "row" :id "grid"}]
        (for [element-data (if badge-small-view (sort-by :mtime > badges) (take 6 (sort-by :mtime > badges)))]
          (badge-grid-element element-data))))

(defn page-grid [pages profile_picture page-small-view]
  (into [:div {:class "row" :id "grid"}]
        (for [element-data (if page-small-view (sort-by :mtime > pages) (take 6 (sort-by :mtime > pages))) ]
          (page-grid-element element-data profile_picture))))

(defn connect-user [user-id]
  (let [connectuser (first (plugin-fun (session/get :plugins) "block" "connectuser"))]
    (if connectuser
      [connectuser user-id]
      [:div ""])))


(defn content [state]
  (let [visibility-atom (cursor state [:user :profile_visibility])
        badge-small-view (cursor state [:badge-small-view])
        page-small-view (cursor state [:page-small-view])
        link-or-embed-atom (cursor state [:user :show-link-or-embed-code])
        {badges :badges pages :pages owner? :owner? {first_name :first_name last_name :last_name profile_picture :profile_picture about :about} :user profile :profile user-id :user-id} @state
        fullname (str first_name " " last_name)]

    [:div.panel {:id "profile"}
     [m/modal-window]
     [:div.panel-body
      (if owner?
        [:div.row
         (if-not (or (not-activated?) (private?))
           (profile-visibility-input visibility-atom))
         [:div.col-xs-12
          [s/share-buttons (str (session/get :site-url) (path-for "/user/profile/") user-id) fullname (= "public" @visibility-atom) false link-or-embed-atom]]
         [:div.col-xs-12
          (if-not (not-activated?)
            [:a {:href (path-for "/user/edit/profile")} (t :user/Editprofile)])]]
        [:div
         (connect-user user-id)
         (admintool user-id "user")])
      [:h1.uppercase-header fullname]

      [:div {:class "row row_reverse"}
       [:div {:class "col-md-3 col-sm-3 col-xs-12"}
        [:div.profile-picture-wrapper
        [:img.profile-picture {:src (profile-picture profile_picture)
                               :alt fullname}]]]
       [:div {:class "col-md-9 col-sm-9 col-xs-12"}
        (if (not-empty about)
          [:div {:class "row about"}
           [:div.col-xs-12 [:b (t :user/Aboutme) ":"]]
           [:div.col-xs-12 about]])
        (if (not-empty profile)
          [:div.row
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
                             :else (t value))]]))]]]
          )]]
      (if (not-empty badges)
        [:div {:id "user-badges"}
         [:h2 {:class "uppercase-header user-profile-header"} (t :user/Recentbadges)]
         [badge-grid badges @badge-small-view]
         (if (< 6 (count badges))
           [:div [:a {:href "#" :on-click #(reset! badge-small-view (if @badge-small-view false true))}  (if @badge-small-view (t :admin/Showless) (t :user/Showmore))]])])
      (if (not-empty pages)
        [:div {:id "user-pages"}
         [:h2 {:class "uppercase-header user-profile-header"} (t :user/Recentpages)]
         [page-grid pages profile_picture @page-small-view]
         (if (< 6 (count pages))
           [:div [:a {:href "#" :on-click #(reset! page-small-view (if @page-small-view false true))}  (if @page-small-view (t :admin/Showless) (t :user/Showmore))]])])
      (reporttool1 user-id fullname "user")
      ]]))

(defn init-data [user-id state]
  (let [reporttool-init {:description ""
                         :report-type "bug"
                         :item-id ""
                         :item-content-id ""
                         :item-url   ""
                         :item-name "" ;
                         :item-type "" ;badge/user/page/badges
                         :reporter-id ""
                         :status "false"}]

    (ajax/GET
     (path-for (str "/obpv1/user/profile/" user-id) true)
     {:handler (fn [data]
                 (reset! state (assoc data :user-id user-id
                                      :show-link-or-embed-code nil
                                      :permission "success"
                                      :badge-small-view false
                                      :reporttool reporttool-init)))}
     (fn [] (swap! state assoc :permission "error")))))

(defn handler [site-navi params]
  (let [user-id (:user-id params)
        state (atom {:user-id user-id
                     :permission "initial"
                     :badge-small-view false
                     :pages-small-view true})
        user (session/get :user)]
    (init-data user-id state)

    (fn []
      (cond
        (= "initial" (:permission @state)) (layout/default site-navi [:div])
        (and user (= "error" (:permission @state)))(layout/default-no-sidebar site-navi (err/error-content))
        (= "error" (:permission @state)) (layout/landing-page site-navi (err/error-content))
        (= (:id user) (js/parseInt user-id)) (layout/default site-navi (content state))
        (and (= "success" (:permission @state)) user) (layout/default-no-sidebar site-navi (content state))
        :else (layout/landing-page site-navi (content state))))))
