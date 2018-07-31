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

(defn init-badge-info [assertion state]
  (ajax/GET
    (path-for (str "/obpv1/badge/verify"))
    { :params {:assertion_url (:id assertion)}
      :handler (fn [data]
                 (reset! state (assoc data
                                 ;:verifying false
                                 :display "none"
                                 :style "success")))}))
(defn code-helper [v]
  (case v
    200 "ok"
    800 "empty"
    v
    ))

(defn verify-badge [assertion]
  (let [state (atom {:verifying true})]
    (init-badge-info assertion state)
    (fn []
      (let [{:keys [assertion-status badge-image-status revoked? expired? assertion badge-issuer-status badge-criteria-status]} @state
            ]
        (js/setTimeout (fn [] (swap! state assoc :verifying false)) 2000)
        (if (= true (:verifying @state))
          [:div.ajax-message {:style {:padding-top "20px"}}
           [:i {:class "fa fa-cog fa-spin fa-2x "}]
           [:span (str (t :core/Loading) "...")]]
          [:div {:style {:padding-top "20px"}}
           (case assertion-status
             410  [:div {:class "revoked"}
                   (str (t :badge/Badge) (t :badge/Revoked))
                   ]
             500 [:div {:class "expired"}
                  (:badge-status @state)
                  ]
             [:div
              #_(if (and verified_by_obf issued_by_obf) [:p (t :badge/Issuedandverifiedbyobf)])
              [:p [:b "Assertion url"]" - " (code-helper assertion-status)]
              [:p [:b "Badge Image url"]" - " (code-helper badge-image-status)]
              [:p [:b "Badge Criteria url"]" - " (code-helper badge-criteria-status)]
              [:p [:b "Badge Issuer url"]" - " (code-helper badge-issuer-status)]
              [:p [:b (str (t :badge/Revoked))] " - " (if revoked? (t :core/Yes) (t :core/No)) ]
              [:p [:b (str (t :badge/Expired))]" - " (if expired? (t :core/Yes) (t :core/No))]



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
               ]])])))))


