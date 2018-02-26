(ns salava.badge.ui.endorsement
    (:require [salava.core.i18n :refer [t]]
              [reagent.core :refer [atom cursor create-class]]
              [salava.core.time :refer [unix-time date-from-unix-time]]
              [salava.core.ui.ajax-utils :as ajax]
              [salava.core.ui.helper :refer [path-for private?]]
              [salava.core.ui.modal :as mo]))


(defn endorsement-row [endorsement]
  (let [{:keys [issuer content issued_on]} endorsement]
   [:div
    [:h5
     [:a {:href "#"
          :on-click #(do (.preventDefault %) (mo/set-new-view [:badge :issuer] (:id issuer)))
          } (:name issuer)]
     " "
     [:small (date-from-unix-time (* 1000 issued_on))]]
    [:div {:dangerouslySetInnerHTML {:__html content}}]]))


(defn init-badge-endorsements [state badge-id]
  (ajax/GET
    (path-for (str "/obpv1/badge/endorsement/" badge-id))
    {:handler (fn [data] (reset! state data))}))

(defn badge-endorsement-content [badge-id]
  (let [endorsements (atom [])]
    (init-badge-endorsements endorsements badge-id)
    (fn []
      [:div.row {:id "badge-contents"}
       [:div.col-xs-12
        [:h4 {:style {:margin-bottom "20px"}} (t :badge/BadgeEndorsedBy)]
        (into [:div]
              (for [endorsement @endorsements]
                (endorsement-row endorsement)))]])))
