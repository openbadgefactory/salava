(ns salava.extra.application.ui.modal
  (:require [reagent.core :refer [atom cursor create-class]]
            [reagent-modals.modals :refer [close-modal!]]
            [clojure.string :refer [trim blank? split]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.helper :refer [path-for unique-values current-path navigate-to not-activated?]]
            [salava.core.i18n :as i18n :refer [t]]
            [salava.core.ui.modal :as mo]
            [salava.extra.application.ui.application :as app]
            [salava.badge.ui.modal :as bm]))

(defn tag-parser [tags]
  (if tags
    (split tags #",")))

(defn init [id data-atom state]
  (ajax/GET
    (path-for (str "/obpv1/application/public_badge_advert_content/" id))
    {:handler (fn [data]
                (reset! data-atom data))}))

(defn content [id state]
  (let [data-atom (atom {})]
    (init id data-atom state)
    (fn []
      (let [{:keys [image_file name info issuer_content_name tags issuer_content_id issuer_content_name issuer_content_url issuer_contact issuer_image description criteria_url]} @data-atom
            tags (tag-parser tags)
            country (:country-selected @state)]
        [:div {:id "badge-content"}
         [:div {:id "badge-contents"}
          [:div.row
           [:div {:class "col-md-3 badge-image modal-left"}
            [:img {:src (str "/" image_file)}]]
           [:div {:class "col-md-9 badge-info"}
            [:div.rowcontent
             [:h1.uppercase-header name]
             [:div.badge-stats
              #_(bh/issuer-label-image-link issuer_content_name issuer_content_url "" issuer_contact issuer_image)
              (bm/issuer-modal-link issuer_content_id issuer_content_name)
              [:div
               description]
              (if-not (blank? criteria_url)
                [:div {:class "badge-info"}
                 [:a {:href   criteria_url
                      :target "_blank"} (t :badge/Opencriteriapage)]])]
             [:div {:class " badge-info"}
              [:h2.uppercase-header (t :extra-application/Howtogetthisbadge)]
              [:div {:dangerouslySetInnerHTML {:__html info}}]]
             [:div
              (if (not (empty? tags))
                (into [:div]
                      (for [tag tags]
                        [:a {:href         "#"
                             :id           "tag"
                             :on-click     #(do
                                              (swap! state assoc :advanced-search true)
                                              (app/set-to-autocomplete state tag))
                             :data-dismiss "modal"}
                         (str "#" tag )])))]]]]]
         [:div.modal-footer
          [:div {:class "badge-advert-footer"}
           [:div {:class "badge-contents col-xs-12"}
            [:div.col-md-3 [:div]]
            [:div {:class "col-md-9 badge-info"}
             [:div
              [:div.pull-left
               [:a  {:href (:application_url @data-atom) :target "_"} [:i.apply-now-icon {:class "fa fa-angle-double-right"}] (if (or (= "application" (:kind @data-atom)) (blank? (:application_url_label @data-atom))) (str " " (t :extra-application/Getthisbadge))  (str " " (:application_url_label @data-atom)))]
               ;[:a  " >> Apply now"]
               ]
              (if-not (not-activated?)
                (if (pos? (:followed @data-atom))
                  [:div.pull-right [:a {:href "#" :on-click #(app/remove-from-followed (:id @data-atom) data-atom state)} [:i {:class "fa fa-bookmark"}] (str " " (t :extra-application/Removefromfavourites))]]
                  [:div.pull-right [:a {:href "#" :on-click #(app/add-to-followed (:id @data-atom) data-atom state)} [:i {:class "fa fa-bookmark-o"}] (str " " (t :extra-application/Addtofavourites))]]))]]]]]]))))

(defn badge-content-modal [id state]
  (create-class {:reagent-render (fn []
                                   (content id state))
                 :component-will-unmount (fn [] (do (close-modal!)))}))


(defn handler [params]
  (let [id (:id params)
        state (:state params)]
    (fn [] (badge-content-modal id state))))

(def ^:export modalroutes
  {:app {:advert handler}}
  )
