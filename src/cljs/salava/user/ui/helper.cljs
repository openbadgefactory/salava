(ns salava.user.ui.helper
  (:require [salava.core.ui.helper :refer [path-for]]))

(def default-profile-picture "/img/user_default.png")

(defn profile-picture [path]
  (if path (str "/" path) default-profile-picture))

(defn profile-link-inline [id first_name last_name picture]
  [:li.user-link-inline
   [:a {:href (path-for (str "/user/profile/" id))}
    [:img {:src (profile-picture picture)}]
    first_name " " last_name]])