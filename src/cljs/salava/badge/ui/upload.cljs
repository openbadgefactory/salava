(ns salava.badge.ui.upload
  (:require [reagent.core :refer [atom]]
            [reagent-modals.modals :as m]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.layout :as layout]
            [salava.core.ui.helper :refer [navigate-to path-for]]
            [salava.core.i18n :refer [t translate-text]]))

(defn upload-modal [{:keys [status message reason]}]
  [:div
   [:div.modal-header
    [:button {:type "button"
              :class "close"
              :data-dismiss "modal"
              :aria-label "OK"}
     [:span {:aria-hidden "true"
             :dangerouslySetInnerHTML {:__html "&times;"}}]]
    [:h4.modal-title (translate-text message)]]
   [:div.modal-body
    [:div {:class (str "alert " (if (= status "error")
                                  "alert-warning"
                                  "alert-success"))}
     (translate-text reason)]]
   [:div.modal-footer
    [:button {:type "button"
              :class "btn btn-primary"
              :data-dismiss "modal"}
     "OK"]]])

(defn send-file [state]
  (let [file (-> (.querySelector js/document "input")
                 .-files
                 (.item 0))
        form-data (doto
                    (js/FormData.)
                    (.append "file" file (.-name file)))]
    (swap! state assoc :status "loading")
    (ajax/POST
      (path-for "/obpv1/badge/upload")
      {:body    form-data
       :handler (fn [data]
                  (do
                    (swap! state assoc :status "form")
                    (m/modal! (upload-modal data)
                              (if (= (:status data) "success")
                                {:hide #(navigate-to "/badge")}))))})))

(defn upload-info []
  [:div
   [:p
    (t :badge/Uploadbadgesfrominfo1) ":"]
   [:ol
    [:li {:dangerouslySetInnerHTML
         {:__html (t :badge/Uploadbadgesfrominfo2)}}]
    [:li {:dangerouslySetInnerHTML
         {:__html (t :badge/Uploadbadgesfrominfo3)}}]]
   [:p
    (t :badge/Uploadbagesfromresult1) " "
    [:a {:href (path-for "/badge/mybadges")} (t :badge/Mybadges)] " " (t :badge/page) ". "
     (t :badge/Uploadbagesfromresult2) "  "
    [:a {:href (path-for "/badge/mybadges")} (t :badge/Mybadges)]
    " " (t :badge/Uploadbagesfromresult3) "."]])

(defn content [state]
  (let [status  (:status @state)]
    [:div {:class "badge-upload"}
     [m/modal-window]
     [:h1.uppercase-header (t :badge/Uploadbadgesfrom)]
     [upload-info]
     (cond
       (= "loading" status) [:div.ajax-message
                             [:i {:class "fa fa-cog fa-spin fa-2x "}]
                             [:span (str (t :core/Loading) "...")]]
       :else [:form {:id "form"}
              [:input {:type       "file"
                       :aria-label "Choose file"
                       :name       "file"
                       :on-change  #(send-file state)
                       :accept     "image/png, image/svg+xml"}]])]))

(defn init-data []
      (ajax/GET (path-for "/obpv1/user/test") {}))

(defn handler [site-navi]
  (let [state (atom {:status "form"})]
    (fn []
      (init-data)
      (layout/default site-navi (content state)))))
