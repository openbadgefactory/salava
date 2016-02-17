(ns salava.user.ui.profile
  (:require [reagent.core :refer [atom cursor]]
            [reagent.session :as session]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.layout :as layout]
            [salava.core.ui.share :as s]
            [salava.core.i18n :refer [t]]
            [salava.core.time :refer [date-from-unix-time]]))

(defn toggle-visibility [visibility-atom]
  (ajax/POST
    "/obpv1/user/profile/set_visibility"
    {:params  {:visibility (if (= "internal" @visibility-atom) "public" "internal")}
     :handler (fn [new-value]
                (reset! visibility-atom new-value))}))

(defn profile-visibility-input [visibility-atom]
  [:div.col-xs-12
   [:input {:id        "input-visibility"
            :name      "visibility"
            :type      "checkbox"
            :on-change #(toggle-visibility visibility-atom)
            :checked   (= "public" @visibility-atom)}]
   [:label {:for "input-visibility"} (t :user/Publishandshare)]])

(defn badge-grid-element [element-data]
  (let [{:keys [id image_file name description]} element-data]
    [:div {:class "col-xs-12 col-sm-6 col-md-4"
           :key id}
     [:div {:class "media grid-container"}
      [:div.media-content
       (if image_file
         [:div.media-left
          [:img {:src (str "/" image_file)}]])
       [:div.media-body
        [:div.media-heading
         [:a.heading-link {:href (str "/badge/info/" id)}
          name]]
        [:div.media-description description]]]]]))

(defn page-grid-element [element-data]
  (let [{:keys [id name first_name last_name badges mtime]} element-data]
    [:div {:class "col-xs-12 col-sm-6 col-md-4"
           :key id}
     [:div {:class "media grid-container"}
      [:div.media-content
       [:div.media-body
        [:div.media-heading
         [:a.heading-link {:href (str "/page/view/" id)} name]]
        [:div.media-content
         [:div.page-owner
          [:a {:href "#"} first_name " " last_name]]
         [:div.page-create-date
          (date-from-unix-time (* 1000 mtime) "minutes")]
         (into [:div.page-badges]
               (for [badge badges]
                 [:img {:title (:name badge)
                        :src (str "/" (:image_file badge))}]))]]
       [:div {:class "media-right"}
        [:img {:src "/img/user_default.png"}]]]]]))

(defn badge-grid [badges]
  (into [:div {:class "row" :id "grid"}]
        (for [element-data (sort-by :mtime > badges)]
          (badge-grid-element element-data))))

(defn page-grid [pages]
  (into [:div {:class "row" :id "grid"}]
        (for [element-data (sort-by :mtime > pages)]
          (page-grid-element element-data))))

(defn content [state]
  (let [visibility-atom (cursor state [:user :profile_visibility])
        link-or-embed-atom (cursor state [:user :show-link-or-embed-code])
        {badges :badges pages :pages owner? :owner? {first_name :first_name last_name :last_name profile_picture :profile_picture about :about} :user user-id :user-id} @state
        fullname (str first_name " " last_name)]
    [:div.panel {:id "profile"}
     [:div.panel-body
      (if owner?
        [:div.row
         (profile-visibility-input visibility-atom)
         [:div.col-xs-12
          [s/share-buttons (str (session/get :base-url) "/user/profile/" user-id) fullname (= "public" @visibility-atom) false link-or-embed-atom]]
         [:div.col-xs-12
          [:a {:href "/user/profile/edit"} (t :user/Editprofile)]]])
      [:h2.uppercase-header fullname]
      [:div.row
       [:div {:class "col-md-3 col-xs-12"}
        [:img.profile-picture {:src (or profile_picture "/img/user_default.png")}]]
       [:div {:class "col-md-9 col-xs-12"}
        (if about
          [:div.row
           [:div.col-xs-12 [:b (t :user/Aboutme) ":"]]
           [:div.col-xs-12 about]])]]
      (if (not-empty badges)
        [:div {:id "user-badges"}
         [:h2 {:class "uppercase-header user-profile-header"} (t :user/Recentbadges)]
         [badge-grid badges]
         [:div [:a {:href (str "/gallery/badges/" user-id)} (t :user/Showmore)]]])
      (if (not-empty pages)
        [:div {:id "user-pages"}
         [:h2 {:class "uppercase-header user-profile-header"} (t :user/Recentpages)]
         [page-grid pages]
         [:div [:a {:href (str "/gallery/pages/" user-id)} (t :user/Showmore)]]])]]))

(defn init-data [user-id state]
  (ajax/GET
    (str "/obpv1/user/profile/" user-id)
    {:handler (fn [data]
                (reset! state (assoc data :user-id user-id
                                          :show-link-or-embed-code nil)))}))

(defn handler [site-navi params]
  (let [user-id (:user-id params)
        state (atom {:user-id user-id})]
    (init-data user-id state)
    (fn []
      (layout/default site-navi (content state)))))
