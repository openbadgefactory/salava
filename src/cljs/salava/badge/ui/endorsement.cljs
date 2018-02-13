(ns salava.badge.ui.endorsement
    (:require [salava.core.i18n :refer [t]]
              [reagent.core :refer [atom cursor create-class]]
              [reagent-modals.modals :as m :refer [close-modal!]]
              [salava.badge.ui.helper :as bh]
              [salava.core.time :refer [unix-time date-from-unix-time]]
              [reagent-modals.modals :as m]
              [salava.core.ui.modal :as mo]))

(def views (atom []))

(defn set-new-view [modal]
  (reset! views (conj @views [modal])))


(defn modal-content []
  [:div {:id "badge-content"}
   [:div.modal-body
    [:div
     [:div.col-md-12
      (if (< 1 (count @views))
        [:div {:class "pull-left"}
         [:button {:type  "button"
                   :class "close"
                   :aria-label "OK"
                   :on-click #(do
                                (reset! views (pop @views))
                                (.preventDefault %))}
          [:i {:class "fa fa-arrow-circle-left" :aria-hidden "true"}]]])
      [:div {:class "text-right"}
       [:button {:type         "button"
                 :class        "close"
                 :data-dismiss "modal"
                 :aria-label   "OK"}
        [:span {:aria-hidden             "true"
                :dangerouslySetInnerHTML {:__html "&times;"}}]]]]]
    [:div (last @views)]]
   [:div.modal-footer ]])


(defn endorser-content [endorsement]
  (let [{:keys [endorser_name endorser_image endorser_description endorser_url endorser_email endorser_endorsements]} endorsement]
   (create-class {:reagent-render
                  (fn []
                    (bh/endorser-info-displayer endorser_name endorser_url endorser_description endorser_email endorser_image endorser_endorsements))})))


(defn inline-endorsement-modal [endorsement]
  (let [{:keys [endorser_name endorser_image endorsement_comment endorsement_issuedon]} endorsement]
   [:div {:class "media endorsement-content-item"}
     [:div.endcontent
        [:div [:i {:class "fa fa-thumbs-o-up" :style {:font-size "20px"}}]]
        [:div {:class "endorsement-body-container"}
         [:div.namedate
          [:div.name [:h4 {:class "media-heading endorser-body"}
          [:a {:href "#"
                :on-click #(do (.preventDefault %) (set-new-view (endorser-content endorsement)))
               } endorser_name]]]
          [:div.date [:span (date-from-unix-time (* 1000 endorsement_issuedon))]]
          ]
          [:div.commentbox
            [:span {:style {:font-style "italic"} } endorsement_comment]]
        ]]]))


(defn endorsement-content [endorsements]
  (let [sorted-endorsements (sort-by :endorsement_issuedon endorsements)]
    [:div.row
      [:h1.uppercase-header.endorsement-heading  (t :badge/endorsements)]
       (into [:div]
             (for [endorsement sorted-endorsements]
                  (inline-endorsement-modal endorsement)))]))

(defn modal-init [view]
  (create-class {:component-will-mount   (fn [] (reset! views [view]))
                 :reagent-render         (fn [] (modal-content))
                 :component-will-unmount (fn [] (do (close-modal!)
                                                    (reset! views [])))}))


(defn open-modal [endorsements]
  (m/modal! [modal-init (endorsement-content endorsements)] {:size :lg}))

(defn endorsement-modal-link [endorsement-count endorsements]
  (fn []
    [:div.row
        [:div.col.xs-12
         [:hr.endorsementhr]
            [:a.endorsementlink {
                       :class "endorsement-link"
                       :href "#"
                       :on-click #(do
                                      (.preventDefault %)
                                      (open-modal endorsements))}
                (if (== endorsement-count 1)
                  (str  endorsement-count " " (t :badge/endorsement))
                  (str  endorsement-count " " (t :badge/endorsements)))]]]))


