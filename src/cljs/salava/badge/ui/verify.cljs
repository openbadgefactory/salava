(ns salava.badge.ui.verify
  (:require [reagent.core :refer [atom cursor]]
            [salava.core.i18n :refer [t]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.helper :refer [path-for]]
            [salava.core.helper :refer [dump]]
            [salava.badge.ui.helper :as bh]
            [salava.badge.ui.assertion :as a]
            [clojure.string :refer [blank? upper-case]]
            [salava.core.time :refer [date-from-unix-time unix-time unix-time]]
            ))

(defn init-verify-info [state]
  (ajax/GET
    (path-for (str "/obpv1/badge/verify/" (:id @state)))
    {:handler (fn [data]
                (swap! state assoc :result data
                                   :verifying false
                                   :display "none"))}))

(defn bottom-links [state]
  (let [assertion (get-in @state [:result :assertion])]
    [:div
     [:div
      [:a.link {:style {:float "right" :padding-bottom "5px"}
                :href     "#"
                :on-click #(do (.preventDefault %)
                             (if (= (:display @state) "none") (swap! state assoc :display "block") (swap! state assoc :display "none"))
                             )} (if (= (:display @state) "none") (str (t :badge/Openassertion) "...") (str (t :badge/Hideassertion) "..."))]
      #_[:a {:style {:float "right"} :href (str "https://badgecheck.io/?url="(:asr @state)) :target "_blank" :rel "nofollow noopener"} "use external validator"]]
     [:div {:style {:display (:display @state) :padding-top "30px"}}
      [a/assertion-content (dissoc assertion :evidence :endorsement)]]
     [:br]]))

(defn verify-badge-content [state]
  (fn []
    (let [data (:result @state)
          {:keys [assertion-status badge-image-status revoked? expired? assertion badge-issuer-status badge-criteria-status asr revocation_reason message]} data]
      [:div {:id "verify-badge" :style {:display (:show-result @state)}}
       (if (= true (:verifying @state))
         [:div.ajax-message {:style {:padding-top "20px"}}
          [:i {:class "fa fa-cog fa-spin fa-2x "}]
          [:span (str (t :core/Loading) "...")]]
         [:div;.panel {:style {:padding-top "20px"}}
          [:hr.border]
          [:div;.panel-body
           [:div.close {:style {:opacity 1}};.close-button
            [:a {
                      :aria-label "OK"
                      :on-click   #(do
                                     (.preventDefault %)
                                     (swap! state assoc :show-result "none"
                                                        :show-link "block"
                                                        :display "none"))}
             [:i.fa.fa-remove {:title (t :core/Cancel)}]
             #_[:span {:aria-hidden "true"
                     :dangerouslySetInnerHTML {:__html "&times;"}}]]]

           (case assertion-status
             404  [:div
                   [:div {:class "alert alert-danger"} (t :badge/Badgecheckfailed)]
                   [:p [:i "404 not found"]]
                   [:hr.border]]
             410  [:div
                   [:div {:class "alert alert-danger"} (t :badge/Badgerevoked) #_(str (t :badge/Badge) " " (t :badge/Revoked))]
                   #_[:a {:target "_blank" :rel "nofollow noopener" :href (str "https://badgecheck.io/?url="asr) :style {:float "right"}} "use external validator"]
                   [:hr.border]]
             500 [:div
                  [:div {:class "alert alert-danger"}
                   (t :badge/Badgecheckfailed)]
                  [:br]
                  [:p [:i message]]
                  #_[:a {:target "_blank" :rel "nofollow noopener" :href (str "https://badgecheck.io/?url="asr) :style {:float "right"}} "use external validator"]
                  [:hr.border]]
             [:div
              (cond
                revoked? [:div [:div {:class "alert alert-danger"}  (t :badge/Badgerevoked) #_(str (t :badge/Badge) " " (t :badge/Revoked))] [:p revocation_reason]]
                expired? [:div [:div {:class "alert alert-danger"} (t :badge/Badgeisexpired)] [bottom-links state]]
                :else [:div
                       #_[:p.validation-header (t :badge/Badgevaliditycheck)]
                       #_[:h2.uppercase-header.validation-header (t :badge/Badgevaliditycheck)]

                       [:table
                        [:tbody
                         [:tr
                          [:td.validation-result  (t :badge/Gotfromassertionurl)]
                          [:td [:i {:class "fa fa-check-circle fa-lg"}]]]
                         (if (= 200 badge-image-status)
                           [:tr
                            [:td.validation-result  (t :badge/Gotfromimageurl)]
                            [:td [:i {:class "fa fa-check-circle fa-lg"}]]])
                         (if (= 200 badge-criteria-status)
                           [:tr
                            [:td.validation-result  (t :badge/Gotfromcriteriaurl)]
                            [:td [:i {:class "fa fa-check-circle fa-lg"}]]])
                         (if (= 200 badge-issuer-status)
                           [:tr
                            [:td.validation-result  (t :badge/Gotfromissuerurl)]
                            [:td [:i {:class "fa fa-check-circle fa-lg"}]]])]]

                       [:div  {:class "alert alert-success "} [:i {:class "fa fa-check-circle fa-2x"}] (t :badge/Validbadge)]
                       [bottom-links state]
                       [:hr.border]])])]])])))

(defn check-badge-link [state]
  (fn []
    [:div
     [:a {:href "#"
          :style {:display (:show-link @state)}
          :on-click #(do
                       (swap! state assoc :show-result "block"
                                          :show-link "none")
                       (when (empty? (:result @state)) (init-verify-info state))
                       (.preventDefault %))}[:i.fa.fa-search] (str (t :badge/Verifybadge) "...")]]))


(defn check-badge [badgeid]
  (let [state (atom {:id badgeid
                     :verifying true
                     :show-result "none"
                     :show-link "block"
                     :result {}})]
    [:div {:id "verify-link"}
     [:br]
     [check-badge-link state]
     [verify-badge-content state]]))

