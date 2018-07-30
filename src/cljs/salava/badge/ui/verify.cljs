(ns salava.badge.ui.verify
  (:require [reagent.core :refer [atom]]
            [salava.core.i18n :refer [t]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.helper :refer [path-for]]
            [salava.core.helper :refer [dump]]
            [salava.badge.ui.helper :as bh]
            [salava.badge.ui.assertion :as a]
            [salava.core.time :refer [date-from-unix-time unix-time unix-time]]
            ))

(defn init-badge-info [state badge-id]
  #_(ajax/GET
    (path-for (str "/obpv1/badge/info/" badge-id))
    {:handler (fn [data] (reset! state (assoc data
                                         :verifying true
                                         :display "none"
                                         :style "success")))})
  (ajax/GET
    (path-for (str "/obpv1/badge/verify/" badge-id))
        {:handler (fn [data]
                    (dump data)
                    (reset! state (assoc data
                                         :verifying true
                                         :display "none"
                                         :style "success")))}
    ))
(defn verify [state]
  (let [assertion (:assertion-url @state)]
    (dump assertion)
  #_(ajax/POST
    (path-for (str "/obpv1/badge/verify"))
    {:params {:assertion-url assertion}})))

(defn verify-badge [badge-id]
  (let [state (atom {})]
    (init-badge-info state badge-id)
    (fn []
      (let [{:keys [revoked verified_by_obf issued_by_obf assertion issuer_verified expires_on]} @state
            revoked? (pos? revoked)
            expired? (bh/badge-expired? expires_on)]
        (js/setTimeout (fn [] (swap! state assoc :verifying false)) 2000)
        (if (= true (:verifying @state))
          [:div.ajax-message {:style {:padding-top "20px"}}
           [:i {:class "fa fa-cog fa-spin fa-2x "}]
           [:span (str (t :core/Loading) "...")]]
          [:div {:style {:padding-top "20px"}}
           (if (and verified_by_obf issued_by_obf) [:p (t :badge/Issuedandverifiedbyobf)])
           [:p [:b (str (t :badge/Revoked)"?")] " - " (if revoked? (t :core/Yes) (t :core/No)) ]
           [:p [:b (str (t :badge/Expired)"?")]" - " (if expired? (t :core/Yes) (t :core/No))]

           [:div {:class (:style @state)}
            (cond
              revoked? (do (swap! state assoc :style "revoked") (str (t :badge/Badge) (t :badge/Revoked)))
              expired? (do (swap! state assoc :style "expired") (t :badge/Badgeisexpired))
              :else (do (swap! state assoc :style "success")  (t :badge/Validbadge)))]
           [:br]
           [:p [:i "last checked on " (date-from-unix-time (* 1000 (unix-time)))]]
           [:a.link {:href     "#"
                     :on-click #(do (.preventDefault %)
                                  (if (= (:display @state) "none") (swap! state assoc :display "block") (swap! state assoc :display "none"))
                                  )} (if (= (:display @state) "none") (str (t :badge/Openassertion) "...") (str (t :badge/Hideassertion) "..."))]
           [:div {:style {:display (:display @state) :padding-top "30px"}}
            [a/assertion-content (dissoc assertion :evidence)]
            ]])))))


