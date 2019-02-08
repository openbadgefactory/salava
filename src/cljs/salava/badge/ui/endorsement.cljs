(ns salava.badge.ui.endorsement
  (:require [salava.core.i18n :refer [t]]
            [reagent.core :refer [atom cursor create-class]]
            [salava.core.time :refer [unix-time date-from-unix-time]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.helper :refer [path-for private?]]
            [salava.core.ui.modal :as mo]
            [clojure.string :refer [blank?]]
            [reagent.session :as session]))


(defn endorsement-row [endorsement]
  (let [{:keys [issuer content issued_on]} endorsement]

    [:div {:style {:margin-bottom "20px"}}
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

(defn save-endorsement [state]
  )


(defn process-text [s state]
  (let [text (-> js/document
                 (.getElementById "claim")
                 (.-innerHTML))
        endorsement-claim (str text (if (blank? text) "" "\n\n") "*" s)]
    (reset! (cursor state [:endorsement-comment]) endorsement-claim)))

(defn endorse-badge-content [state]
  (fn []
    [:div {:style {:display @(cursor state [:show-content])}}
     [:hr]

     [:div.endorse {:style {:margin "5px"}} (t :badge/Endorsehelptext)]

     [:div.row
      [:div.col-xs-12
       [:div.list-group
        [:a.list-group-item {:href "#" :on-click #(do
                                                    (.preventDefault %)
                                                    (process-text (t :badge/Endorsephrase1) state)
                                                    )} [:i.fa.fa-plus-circle](t :badge/Endorsephrase1)]
        [:a.list-group-item {:href "#" :on-click #(do
                                                    (.preventDefault %)
                                                    (process-text (t :badge/Endorsephrase2) state)
                                                    ; (reset! endorsement (conj @endorsement))
                                                    )} [:i.fa.fa-plus-circle](t :badge/Endorsephrase2)]
        [:a.list-group-item {:href "#" :on-click #(do
                                                    (.preventDefault %)
                                                    (process-text (t :badge/Endorsephrase3) state)
                                                    ) } [:i.fa.fa-plus-circle](t :badge/Endorsephrase3)]
        [:a.list-group-item {:href "#" :on-click #(do
                                                    (.preventDefault %)
                                                    (process-text (t :badge/Endorsephrase4) state)
                                                    ) } [:i.fa.fa-plus-circle](t :badge/Endorsephrase4)]
        [:a.list-group-item {:href "#" :on-click #(do
                                                    (.preventDefault %)
                                                    (process-text (t :badge/Endorsephrase5) state)
                                                    )  } [:i.fa.fa-plus-circle](t :badge/Endorsephrase5)]]]]

     [:div.form-group
      [:label {:for "claim"} (t :badge/Composeyourendorsement)  ]
      [:textarea.form-control {:id "claim"
                               :rows 10
                               :cols 60
                               :type "text"
                               :value @(cursor state [:endorsement-comment])
                               :on-change #(do
                                             (reset! (cursor state [:endorsement-comment]) (.-target.value %))
                                             )}]]
     [:div
      [:button.btn.btn-primary {
                                 :on-click #(do
                                              (.preventDefault %)
                                              ;;save endorsement
                                              )
                                 :disabled (blank? @(cursor state [:endorsement-comment]))

                                 } (t :badge/endorse)]
      [:a {:href "#" :on-click #(do
                                  (.preventDefault %)
                                  (swap! state assoc :show-link "block"
                                         :show-content "none"))} (t :core/Cancel)]]
     [:hr]]))

;; User Endorsements
(defn endorse-badge-link [state]
  (fn []
    [:div
     [:a {:href "#"
          :style {:display @(cursor state [:show-link])}
          :on-click #(do
                       (.preventDefault %)
                       (swap! state assoc :show-link "none"
                              :show-content "block")
                       ;(mo/open-modal [:badge :endorse] badge-id)
                       )}[:i.fa.fa-handshake-o] (t :badge/Endorsethisbadge)]]))

(defn endorse-badge [badge-id]
  (let [state (atom {:id badge-id
                     :show-link "block"
                     :show-content "none"
                     :endorsement-comment ""
                     :endorser-id (-> (session/get :user) :id)})]
    [:div#endorsebadge {:style {:margin-top "10px"}}
     [endorse-badge-link state]
     [endorse-badge-content state]
     ]
    )
  )
(defn request-endorsement [])
(defn pending-endorsement [])

