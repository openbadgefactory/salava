(ns salava.badge.ui.verify
  (:require [reagent.core :refer [atom]]
            [salava.core.i18n :refer [t]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.helper :refer [path-for]]
            [salava.core.helper :refer [dump]]
            [salava.badge.ui.helper :as bh]
            [salava.badge.ui.assertion :as a]
            [clojure.string :refer [blank? upper-case]]
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

(defn bottom-links [state]
  [:div
   [:div
    [:a.link {:style {:float "right" :padding-bottom "5px"}
              :href     "#"
              :on-click #(do (.preventDefault %)
                           (if (= (:display @state) "none") (swap! state assoc :display "block") (swap! state assoc :display "none"))
                           )} (if (= (:display @state) "none") (str (t :badge/Openassertion) "...") (str (t :badge/Hideassertion) "..."))]
    #_[:a {:style {:float "right"} :href (str "https://badgecheck.io/?url="(:asr @state)) :target "_blank" :rel "nofollow noopener"} "use external validator"]]
   [:div {:style {:display (:display @state) :padding-top "30px"}}
    [a/assertion-content (dissoc (:assertion @state) :evidence)]]
   [:br]])

(defn verify-badge-content [badge-id state]
  (fn []
    (let [{:keys [assertion-status badge-image-status revoked? expired? assertion badge-issuer-status badge-criteria-status asr revocation_reason message]} @state]
      [:div {:id "verify-badge" :style {:display (:show-result @state)}}
       (if (= true (:verifying @state))
         [:div.ajax-message {:style {:padding-top "20px"}}
          [:i {:class "fa fa-cog fa-spin fa-2x "}]
          [:span (str (t :core/Loading) "...")]]
         [:div {:style {:padding-top "20px"}}

          (case assertion-status
            404  [:div
                  [:div {:class "alert alert-danger"} (t :badge/Badgecheckfailed)]
                  [:p [:i "404 not found"]]]
            410  [:div
                  [:div {:class "alert alert-danger"} (str (t :badge/Badge) " " (t :badge/Revoked))]
                  #_[:a {:target "_blank" :rel "nofollow noopener" :href (str "https://badgecheck.io/?url="asr) :style {:float "right"}} "use external validator"]]
            500 [:div
                 [:div {:class "alert alert-danger"}
                  (t :badge/Badgecheckfailed)]
                 [:br]
                 [:p [:i message]]
                 #_[:a {:target "_blank" :rel "nofollow noopener" :href (str "https://badgecheck.io/?url="asr) :style {:float "right"}} "use external validator"]]
            [:div
             (cond
               revoked? [:div [:div {:class "alert alert-danger"} (str (t :badge/Badge) " " (t :badge/Revoked))] [:p revocation_reason]]
               expired? [:div [:div {:class "alert alert-danger"} (t :badge/Badgeisexpired)] [bottom-links state]]
               :else [:div
                      [:p.validation-header (t :badge/Badgevaliditycheck)]

                      [:table
                       [:tbody
                        [:tr
                         [:td.validation-result  (t :badge/Retrievefromassertionurl)]
                         [:td [:i {:class "fa fa-check-circle fa-lg"}]]]
                        (if (= 200 badge-image-status)
                          [:tr
                           [:td.validation-result  (t :badge/Retrievefromimageurl)]
                           [:td [:i {:class "fa fa-check-circle fa-lg"}]]])
                        (if (= 200 badge-criteria-status)
                          [:tr
                           [:td.validation-result  (t :badge/Retrievefromcriteriaurl)]
                           [:td [:i {:class "fa fa-check-circle fa-lg"}]]])
                        (if (= 200 badge-issuer-status)
                          [:tr
                           [:td.validation-result  (t :badge/Retrievefromissuerurl)]
                           [:td [:i {:class "fa fa-check-circle fa-lg"}]]])]]

                      [:div  {:class "alert alert-success "} [:i {:class "fa fa-check-circle fa-2x"}] (t :badge/Validbadge)]
                      [bottom-links state]])])])])))

(defn verify-badge [badgeid]
  #_(let [state (atom {:verifying true})]
      (init-badge-info badgeid state)
      (verify-badge-content state)
      (fn []
        (let [{:keys [assertion-status badge-image-status revoked? expired? assertion badge-issuer-status badge-criteria-status asr revocation_reason message]} @state]
          (if (= true (:verifying @state))
            [:div.ajax-message {:style {:padding-top "20px"}}
             [:i {:class "fa fa-cog fa-spin fa-2x "}]
             [:span (str (t :core/Loading) "...")]]
            [:div {:style {:padding-top "20px"}}

             (case assertion-status
               404  [:div {:id "verify-badge"}
                     [:div {:class "alert alert-danger"} (t :badge/Badgecheckfailed)]
                     [:p "404 not found"]]
               410  [:div {:id "verify-badge"}
                     [:div {:class "alert alert-danger"} (str (t :badge/Badge) " " (t :badge/Revoked))]
                     #_[:a {:target "_blank" :rel "nofollow noopener" :href (str "https://badgecheck.io/?url="asr) :style {:float "right"}} "use external validator"]]
               500 [:div {:id "verify-badge"}
                    [:div {:class "alert alert-danger"}
                     (t :badge/Badgecheckfailed)]
                    [:br]
                    [:p [:i message]]
                    #_[:a {:target "_blank" :rel "nofollow noopener" :href (str "https://badgecheck.io/?url="asr) :style {:float "right"}} "use external validator"]]
               [:div
                (cond
                  revoked? [:div {:id "verify-badge"} [:div {:class "alert alert-danger"} (str (t :badge/Badge) " " (t :badge/Revoked))] [:p revocation_reason]]
                  expired? [:div {:id "verify-badge"} [:div {:class "alert alert-danger"} (t :badge/Badgeisexpired)] [bottom-links state]]
                  :else [:div {:id "verify-badge"}
                         [:p.validation-header (t :badge/Badgevaliditycheck)]

                         [:table
                          [:tr
                           [:td.validation-result  (t :badge/Retrievefromassertionurl)]
                           [:td [:i {:class "fa fa-check-circle fa-lg"}]]]
                          (if (= 200 badge-image-status)
                            [:tr
                             [:td.validation-result  (t :badge/Retrievefromimageurl)]
                             [:td [:i {:class "fa fa-check-circle fa-lg"}]]])
                          (if (= 200 badge-criteria-status)
                            [:tr
                             [:td.validation-result  (t :badge/Retrievefromcriteriaurl)]
                             [:td [:i {:class "fa fa-check-circle fa-lg"}]]])
                          (if (= 200 badge-issuer-status)
                            [:tr
                             [:td.validation-result  (t :badge/Retrievefromissuerurl)]
                             [:td [:i {:class "fa fa-check-circle fa-lg"}]]])]

                         [:div  {:class "alert alert-success "} [:i {:class "fa fa-check-circle fa-2x"}] (t :badge/Validbadge)]
                         [bottom-links state]])])])))))

(defn verify-badge-link [badgeid]
  (let [state (atom {:verifying true
                     :show-result "none"})]
    [:div {:id "verify-link"}
     [:br]
     [:a {:href "#"
          :on-click #(do
                       (.preventDefault %)
                       (swap! state assoc :show-result "block")
                       (init-badge-info badgeid state))} (str (t :badge/Verifybadge) "...")]
     [verify-badge-content badgeid state]]))

