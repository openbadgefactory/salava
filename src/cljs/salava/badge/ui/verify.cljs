(ns salava.badge.ui.verify
  (:require [reagent.core :refer [atom]]
            [salava.core.i18n :refer [t]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.helper :refer [path-for]]
            [salava.core.helper :refer [dump]]
            [salava.badge.ui.helper :as bh]
            [salava.badge.ui.assertion :as a]
            [clojure.string :refer [blank?]]
            [salava.core.time :refer [date-from-unix-time unix-time unix-time]]
            ))

(defn init-badge-info [badgeid state]
  (ajax/GET
    (path-for (str "/obpv1/badge/verify/" badgeid))
    {:handler (fn [data]
                (reset! state (assoc data
                                :verifying false
                                :display "none"
                                :style "success")))}))
(defn code-helper [v]
  (case v
    200 "ok"
    800 "not provided"
    v
    ))

(defn bottom-links [state]
  [:div
   [:div
    [:a.link {:href     "#"
              :on-click #(do (.preventDefault %)
                           (if (= (:display @state) "none") (swap! state assoc :display "block") (swap! state assoc :display "none"))
                           )} (if (= (:display @state) "none") (str (t :badge/Openassertion) "...") (str (t :badge/Hideassertion) "..."))]
    [:a {:style {:float "right"} :href (str "https://badgecheck.io/?url="(:asr @state)) :target "_blank" :rel "nofollow noopener"} "use external validator"]]
   [:div {:style {:display (:display @state) :padding-top "30px"}}
    [a/assertion-content (dissoc (:assertion @state) :evidence)]]])

(defn verify-badge [badgeid]
  (let [state (atom {:verifying true})]
    (init-badge-info badgeid state)
    (fn []
      (let [{:keys [assertion-status badge-image-status revoked? expired? assertion badge-issuer-status badge-criteria-status asr revocation_reason message]} @state]
        (if (= true (:verifying @state))
          [:div.ajax-message {:style {:padding-top "20px"}}
           [:i {:class "fa fa-cog fa-spin fa-2x "}]
           [:span (str (t :core/Loading) "...")]]
          [:div {:style {:padding-top "20px"}}
           (case assertion-status
             410  [:div
                   [:div {:class "revoked"}(str (t :badge/Badge) " " (t :badge/Revoked))]
                   [:a {:target "_blank" :rel "nofollow noopener" :href (str "https://badgecheck.io/?url="asr) :style {:float "right"}} "use external validator"]
                   ]
             500 [:div
                  [:div {:class "expired"}
                   (:badge-status @state)]
                  [:br]
                  [:p [:i message]]
                  [:a {:target "_blank" :rel "nofollow noopener" :href (str "https://badgecheck.io/?url="asr) :style {:float "right"}} "use external validator"]]
             [:div
              #_(if (and verified_by_obf issued_by_obf) [:p (t :badge/Issuedandverifiedbyobf)])
              (cond
                revoked? [:div [:p {:class "revoked"} (str (t :badge/Badge) " " (t :badge/Revoked))] [:p revocation_reason]]
                expired? [:div {:class "expired"} [:p (t :badge/Badgeisexpired)]]
                :else [:div
                       [:p [:b "Assertion url"]" - " (code-helper assertion-status)]
                       [:p [:b "Badge Image url"]" - " (code-helper badge-image-status)]
                       [:p [:b "Badge Criteria url"]" - " (code-helper badge-criteria-status)]
                       [:p [:b "Badge Issuer url"]" - " (code-helper badge-issuer-status)]
                       [:p [:b (str (t :badge/Revoked))] " - " (if revoked? (t :core/Yes) (t :core/No)) ]
                       [:p {:class "success"} (t :badge/Validbadge)]])

              [:p [:i "last checked on " (date-from-unix-time (* 1000 (unix-time)))]]
              [bottom-links state]])])))))


