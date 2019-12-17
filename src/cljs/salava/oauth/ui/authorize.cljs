(ns salava.oauth.ui.authorize
  (:require [reagent.session :as session]
            [salava.core.ui.helper :refer [base-path navigate-to path-for current-route-path]]
            [salava.core.ui.layout :as layout]
            [salava.core.i18n :refer [t]]))

(defn content [form]
  [:div
   [:h1 "Authorize application"]
   [:p "TODO describe app access"]
   [:form {:method "post" :action (path-for "/user/oauth2/authorize")}
    [:input {:type "hidden" :name "client_id" :value (.get form "client_id")}]
    [:input {:type "hidden" :name "state" :value (.get form "state")}]
    [:input {:type "hidden" :name "code_challenge" :value (.get form "code_challenge")}]
    [:input.btn.btn-primary {:type "submit" :value "Authorize app"}]]])

(defn handler [site-navi params]
  (let [query-str js/window.location.search
        query (js/URLSearchParams. query-str)]
    (fn []
      (if (session/get :user)
        (layout/landing-page {:no-login true} (content query))
        (do
          (navigate-to "/user/login")
          nil)))))
