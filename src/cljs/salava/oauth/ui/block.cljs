(ns salava.oauth.ui.block
  (:require [salava.oauth.ui.helper :refer [linkedin-link google-link facebook-link]]
            [reagent.session :as session]
            [salava.core.i18n :refer [t]]))

(defn ^:export oauth-login-form []
  (fn []
   [:div {:class "row"}
    (if (some #(= % "oauth") (session/get :plugins))
     [:div.col-md-12 [:h2.or [:span (t :user/or)]]])
    [:div.col-md-12.oauth-buttons
     [:div (facebook-link false nil)]
     [:div (linkedin-link nil nil)]
     [:div [google-link false nil]]]]))


(defn ^:export oauth-registration-form []
 (fn []
  [:div {:class "row"}
   (if (some #(= % "oauth") (session/get :plugins))
     [:div.col-md-12 [:h2.or [:span (t :user/or)]]])
   [:div.col-md-12.oauth-buttons
    [:div (facebook-link false true)]
    [:div (linkedin-link nil "register")]
    [:div [google-link false true]]]]))
