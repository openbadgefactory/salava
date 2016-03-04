(ns salava.badge.ui.upload
  (:require [reagent.core :refer [atom]]
            [reagent-modals.modals :as m]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.layout :as layout]
            [salava.core.ui.helper :refer [navigate-to]]
            [salava.core.i18n :refer [t]]))

(defn upload-modal [{:keys [status message reason]}]
  [:div
   [:div.modal-header
    [:button {:type "button"
              :class "close"
              :data-dismiss "modal"
              :aria-label "OK"}
     [:span {:aria-hidden "true"
             :dangerouslySetInnerHTML {:__html "&times;"}}]]
    [:h4.modal-title message]]
   [:div.modal-body
    [:div {:class (str "alert " (if (= status "error")
                                  "alert-warning"
                                  "alert-success"))}
     reason]]
   [:div.modal-footer
    [:button {:type "button"
              :class "btn btn-primary"
              :data-dismiss "modal"}
     "OK"]]])

(defn send-file []
  (let [file (-> (.querySelector js/document "input")
                 .-files
                 (.item 0))
        form-data (doto
                    (js/FormData.)
                    (.append "file" file (.-name file)))]
    (ajax/POST
      "/obpv1/badge/upload"
      {:body    form-data
       :handler (fn [data]
                  (m/modal! (upload-modal data)
                            (if (= (:status data) "success")
                              {:hide #(navigate-to "/badge")})))})))

(defn upload-info []
  [:div
   [:p
    "You can upload your own badge files from your computer:"]
   [:ol
    [:li "Click " [:b "Browse"] " and browse to your badge"]
    [:li "Click " [:b "Open"]]]
   [:p
    "Your uploaded badges will appear at "
    [:a {:href "/badge/mybadges"} "My badges"] " page. "
    ". You can delete unwanted badges at "
    [:a {:href "/badge/mybadges"} "My badges"]
    " page in badge Settings."]])

(defn content []
  [:div {:class "badge-upload"}
   [m/modal-window]
   [:h1.uppercase-header (t :badge/Uploadbadgesfrom)]
   [upload-info]
   [:form {:id "form"}
    [:input {:type "file"
             :name "file"
             :on-change #(send-file)
             :accept "image/png, image/svg+xml"}]]])

(defn init-data []
      (ajax/GET "/obpv1/user/test" {}))

(defn handler [site-navi]
  (fn []
    (init-data)
    (layout/default site-navi (content))))
