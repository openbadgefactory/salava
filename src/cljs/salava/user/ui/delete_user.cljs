(ns salava.user.ui.delete-user
  (:require
    [salava.core.i18n :refer [t]]
    [reagent.core :refer [atom cursor]]
    [salava.core.ui.layout :as layout]))

(defn content [state]
  [:div
   (t :user/Userdeletemsg)
   ]
  )
(defn handler [site-navi]
  (let [state (atom {})]
    (fn []
      (layout/landing-page site-navi (content state)))))
