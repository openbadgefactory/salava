(ns salava.user.ui.helper
  (:require [salava.core.ui.helper :refer [path-for]]
            [salava.core.i18n :refer [t]]
            [salava.core.ui.modal :refer [set-new-view]]))

(def default-profile-picture "/img/user_default.png")

(defn profile-picture [path]
  (if path (str "/" path) default-profile-picture))

(defn profile-link-inline [id first_name last_name picture]
  [:li.user-link-inline
   [:a {:href (path-for (str "/user/profile/" id))}
    [:img {:src (profile-picture picture)}]
    first_name " " last_name]])

(defn profile-link-inline-modal [id first_name last_name picture]
  [:li.user-link-inline
   [:a {:href "#"
        :on-click #(set-new-view [:profile :view] {:user-id id})}
    [:img {:src (profile-picture picture)}]
    first_name " " last_name]])
