(ns salava.social.ui.helper
  (:require
   [reagent.core :refer [atom cursor create-class]]
   [reagent.session :as session]
   [salava.core.i18n :refer [t]]))

(defn social-plugin? []
  (some #(= "social" %) (session/get :plugins)))
