(ns salava.extra.spaces.ui.error
  (:require
   [salava.core.ui.layout :as layout]
   [salava.core.i18n :refer [t]]))

(defn content []
  [:div.panel
   [:div.panel-body
    [:div.alert.alert-danger
     (t :extra-spaces/Invitelinkeerror)]]])

(defn handler [site-navi]
  (fn []
    (layout/landing-page site-navi (content))))
