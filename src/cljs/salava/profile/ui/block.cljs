(ns salava.profile.ui.block
  (:require [salava.core.ui.helper :refer [base-path path-for]]
            [reagent.core :refer [atom]]
            [salava.core.ui.ajax-utils :as ajax]
            [reagent.session :as session]))

(defn init-user-profile [user-id state]
  (ajax/GET
    (path-for (str "/obpv1/profile/" user-id) true)
    {:handler (fn [data] (reset! state data))}
    ))


(defn ^:export userprofileinfo []
  (let [state (atom {})
        id (session/get-in [:user :id])]
    (init-user-profile id state)
    (fn []
      [:div "TEST"
        #_(:user @state)])))
