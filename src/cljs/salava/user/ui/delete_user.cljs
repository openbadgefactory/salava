(ns salava.user.ui.delete-user
  (:require
    [salava.core.i18n :refer [t]]
    [reagent.core :refer [atom cursor]]
    [salava.core.ui.layout :as layout]
    [salava.translator.ui.helper :refer [translate]]))

(defn content [state]
  [:div.alert.alert-success
   [:b (translate (:lang @state) :user/Userdeletemsg)]])


(defn handler [site-navi params]
  (let [lang (or (:lang params) (-> (or js/window.navigator.userLanguage js/window.navigator.language) (clojure.string/split #"-") first))
        state (atom {:lang lang})]
    (fn []
      (layout/landing-page site-navi (content state)))))
