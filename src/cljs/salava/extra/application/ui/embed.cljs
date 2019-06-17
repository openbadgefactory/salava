(ns salava.extra.application.ui.embed
 (:require [reagent.core :refer [atom cursor create-class]]
           [reagent.session :as session]
           [reagent-modals.modals :as m :refer [close-modal!]]
           [salava.core.ui.ajax-utils :as ajax]
           [salava.extra.application.ui.helper :refer [query-params str-cat]]
           [salava.core.ui.layout :as layout]
           [salava.core.ui.helper :refer [path-for unique-values current-path navigate-to not-activated? disable-background-image]]
           [cemerick.url :as url]
           [salava.core.i18n :as i18n :refer [t]]
           [clojure.walk :refer [keywordize-keys]]
           [salava.core.ui.modal :as mo]))

(defn open-modal [id state]
  (ajax/GET
   (path-for (str "/obpv1/application/public_badge_advert_content/" id))
   {:handler (fn [data]
               (do
                 (mo/open-modal [:application :badge] {:id (:init-id @state) :state state :data data})))}))

(defn badge-grid-element [element-data state]
  (let [{:keys [id image_file name  issuer_content_name issuer_content_url recipients badge_content_id followed issuer_tier]} element-data
        badge-id (or badge_content_id id)]
    [:div {:class "media grid-container"}
     [:div.media-content
      [:a {:href "#" :on-click #(do (.preventDefault %) (mo/open-modal [:application :badge] {:id id  :state state}))}
       (if image_file
         [:div.media-left
          [:img {:src (str "/" image_file)
                 :alt name}]])
       [:div.media-body
        [:div {:class "media-heading"}
         [:span name]]
        [:div.media-issuer
         [:p issuer_content_name]]
        [:div.media-getthis
         [:span [:i.apply-now-icon {:class "fa fa-angle-double-right"}] (str " " (t :extra-application/Getthisbadge))]]]]
      [:div.media-bottom]]]))

(defn gallery-grid [state]
  (let [badges (:applications @state)
        tags (cursor state [:params :tags])
        show-issuer-info-atom (cursor state [:show-issuer-info])]
    [:div
     (into [:div {:class "row wrap-grid"
                  :id    "grid"}]
           (for [element-data badges]
             (badge-grid-element element-data state)))]))

(defn content [state]
  (create-class {:reagent-render (fn []
                                   [:div {:id "badge-advert"}
                                    [m/modal-window]
                                    [gallery-grid state]])
                 :component-did-mount (fn []
                                        (disable-background-image)
                                        (if (:init-id @state) (open-modal (:init-id @state) state)))}))

(defn init-data [user_id params state]
 (let [{:keys [order tags name issuer country followed]} params]
  (ajax/GET
   (path-for (str "/obpv1/application/" user_id "/embed"))
   {:params params
    :handler (fn [data]
              (swap! state assoc :applications (:applications data)))})))


(defn handler [site-navi params]
 (let [query (as-> (-> js/window .-location .-href url/url :query keywordize-keys) $
                   (if (:country $)
                     $
                     (assoc $ :country (session/get-in [:filter-options :country]
                                                       (session/get-in [:user :country] "all")))))
       {:keys [badge_content_id user-id]} params
       params (query-params query)
       state (atom {:params params
                    :init-id (:id params)
                    :user-id user-id
                    :applications []
                    :timer nil
                    :ajax-message nil
                    :initial-query nil
                    :show-link-or-embed-code nil})]
  (init-data user-id params state)
  (fn []
   (layout/embed-page [content state badge_content_id]))))
