(ns salava.extra.application.ui.helper
  (:require
   [reagent.core :refer [atom cursor create-class]]
   [reagent.session :as session]
   [salava.core.i18n :refer [t]]))



(defn application-plugin? []
  (some #(= "extra/application" %) (session/get :plugins)))
