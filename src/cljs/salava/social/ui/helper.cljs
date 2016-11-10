(ns salava.social.ui.helper
  (:require
   [reagent.core :refer [atom cursor create-class]]
   [reagent.session :as session]
   [salava.core.i18n :refer [t]]))

(defn social-plugin? []
  (some #(= "social" %) (session/get :plugins)))

(defn system-image []
  (let [site-name (session/get :site-name)]
    [:div {:class      "logo-image system-image-url"
           :title      (str site-name " logo")
           :aria-label  (str site-name " logo")}]))
