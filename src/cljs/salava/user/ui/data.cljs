(ns salava.user.ui.data
  (:require [salava.core.ui.layout :as layout]
            [salava.core.ui.ajax-utils :as ajax]
            [reagent.session :as session]
            [salava.core.ui.helper :refer [path-for]]
            [salava.core.helper :refer [dump]]
            [salava.core.ui.error :as err]
            ))

(defn content [state]
  [:div {:id "cancel-account"}
   "TEST"
   (dump @state)])

(defn init-data [state]
  (ajax/GET
    (path-for "obpv1/user/foo" true)
     {:handler (fn [data]
                 (swap! state assoc :text data))}
     (fn [] (swap! state assoc :permission "error"))))

(defn handler [site-navi]
  (let [state (atom {})]
    (init-data state)

   (fn []
      (layout/default site-navi (content state)))))
