(ns salava.registerlink.ui.register-token
  (:require [reagent.core :refer [atom cursor]]
            [reagent.session :as session]
            [reagent-modals.modals :as m]
            [clojure.string :refer [trim]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.layout :as layout]
            [salava.user.ui.register :as r]
            [salava.core.ui.helper :refer [path-for]]
            [salava.core.i18n :refer [t]]
            [salava.core.helper :refer [dump]]))


(defn content [state]
  [:div "moimi"])

(defn init-data [state token]
  (ajax/GET
   (path-for (str "/obpv1/registerlink/register/" token) true)
    {:handler (fn [data]
                (let [{:keys [languages]} data]
                  (swap! state assoc :languages languages)))}))

(defn handler [site-navi params]
  (let [state (atom {:email ""
                     :first-name ""
                     :last-name ""
                     :language ""
                     :country ""
                     :languages []
                     :error-message nil
                     :registration-sent nil})
        lang (:lang params)]
    (when (and lang (some #(= lang %) (session/get :languages)))
      (session/assoc-in! [:user :language] lang)
      (swap! state assoc :language lang))
    (dump params)
    (init-data state (:token params))
    
    (fn []
      (layout/landing-page site-navi (r/content state)))))
