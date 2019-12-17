(ns salava.extra.application.ui.modal
  (:require [reagent.core :refer [atom cursor create-class]]
            [reagent-modals.modals :refer [close-modal!]]
            [clojure.string :refer [trim blank? split]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.helper :refer [path-for unique-values current-path navigate-to not-activated?]]
            [salava.core.i18n :as i18n :refer [t]]
            [salava.core.ui.modal :as mo]
            [salava.extra.application.ui.application :as app]
            [salava.badge.ui.modal :as bm]
            [salava.core.helper :refer [dump]]
            [salava.extra.application.ui.helper :refer [set-to-autocomplete add-to-followed remove-from-followed]]))

(defn tag-parser [tags]
  (if tags
    (split tags #",")))

(defn init [id data-atom state]
  (ajax/GET
   (path-for (str "/obpv1/application/public_badge_advert_content/" id))
   {:handler (fn [data]
               (reset! data-atom data))}))

(defn issuer-modal-link [issuer-id name]
  [:div {:class "issuer-data clearfix"}
   [:label {:class "advert-issuer"}  (t :extra-application/Issuer) ":"]
   [:div {:class "issuer-links pull-label-left inline"}
    [:a {:href "#"
         :on-click #(do (.preventDefault %)
                        (mo/open-modal [:badge :issuer] issuer-id))} name]]])

(defn content [id data state]
  (let [data-atom (atom data)]
    (if (empty? data) (init id data-atom state))
    (fn []
      (let [{:keys [image_file name info issuer_content_name tags issuer_content_id issuer_content_name issuer_content_url issuer_contact issuer_image description criteria_url]} @data-atom
            tags (tag-parser tags)
            country (:country-selected @state)]
        [:div {:id "badge-content"}
         [:div {:id "badge-contents"}
          [:div.row.flip
           [:div {:class "col-md-3 badge-image modal-left"}
            [:img {:src (str "/" image_file)}]
            [:div
             [:div.clip-text
              [:a  {:href (:application_url @data-atom) :target "_"} [:i.apply-now-icon {:class "fa fa-angle-double-right"}] (if (or (= "application" (:kind @data-atom)) (blank? (:application_url_label @data-atom))) (str " " (t :extra-application/Getthisbadge))  (str " " (:application_url_label @data-atom)))]]]]
           [:div {:class "col-md-9 "}
            [:div.rowcontent
             [:h1.uppercase-header name]
             [:div.badge-stats
              (issuer-modal-link issuer_content_id issuer_content_name)

              (if-not (blank? description)
                [:div {:class "issuer-data clearfix" :style {:margin-bottom "10px"}}
                 [:label {:class "advert-issuer"}  (t :admin/Description) ":"]
                 [:div {:class "issuer-links pull-label-left inline"}
                  description]])
              (if-not (blank? criteria_url)
                [:div {:class "badge-info"}
                 [:a {:href   criteria_url
                      :target "_blank"} (t :badge/Opencriteriapage)]])]
             [:div {:class " badge-info"}
              [:h2.uppercase-header (t :extra-application/Howtogetthisbadge)]
              [:div {:dangerouslySetInnerHTML {:__html info} :style {:word-wrap "break-word"}}]]
             [:div
              (if (not (empty? tags))
                (into [:div {:style {:word-wrap "break-word"}}]
                      (for [tag tags]
                        [:a {:href         "#"
                             :id           "tag"
                             :on-click     #(do
                                              (swap! state assoc :advanced-search true)
                                              (set-to-autocomplete state tag))
                             :data-dismiss "modal"}
                         (str "#" tag)])))]]]]]
         [:div.modal-footer
          [:div {:class "badge-advert-footer"}
           [:div {:class "badge-contents col-xs-12"}
            [:div.col-md-3 [:div]]
            [:div {:class "col-md-9 badge-info"}
             [:div
              (if-not (not-activated?)
                (if (pos? (:followed @data-atom))
                  [:div.pull-right [:a {:href "#" :on-click #(remove-from-followed (:id @data-atom) data-atom state)} [:i {:class "fa fa-bookmark"}] (str " " (t :extra-application/Removefromfavourites))]]
                  [:div.pull-right [:a {:href "#" :on-click #(add-to-followed (:id @data-atom) data-atom state)} [:i {:class "fa fa-bookmark-o"}] (str " " (t :extra-application/Addtofavourites))]]))]]]]]]))))

(defn handler [params]
  (let [id (:id params)
        state (:state params)
        data (or (:data params) {})]
    (fn [] (content id data state))))

(def ^:export modalroutes
  {:application {:badge handler}})
