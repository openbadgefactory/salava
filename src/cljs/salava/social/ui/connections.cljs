(ns salava.social.ui.connections
   (:require [salava.core.ui.layout :as layout]
             [salava.core.i18n :as i18n :refer [t]]
             [salava.core.ui.ajax-utils :as ajax]
             [salava.core.ui.helper :as h :refer [unique-values navigate-to path-for]]))


(defn content [state]
  [:div {:class "my-badges pages"}
   "hello world"
   ])

(defn init-data [state]
  (ajax/GET
    (path-for "/obpv1/page" true)
    {:handler (fn [data]
                (swap! state assoc :pages data))}))

(defn handler [site-navi]
  (let [state (atom "")]
    ;(init-data state)
    (fn []
      (layout/default site-navi (content state)))))
