(ns salava.badge.ui.upload
  (:require [reagent.core :refer [atom cursor]]
            [reagent-modals.modals :as m]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.layout :as layout]
            [salava.core.ui.helper :refer [navigate-to path-for not-activated?]]
            [salava.core.ui.notactivated :refer [not-activated-banner]]
            [salava.core.ui.error :as err]
            [salava.core.ui.helper :refer [url?]]
            [salava.core.i18n :refer [t translate-text]]
            [salava.user.ui.input :as input]
            [salava.core.helper :refer [dump]]
            [clojure.string :as s]))


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
                                {:hidden #(navigate-to "/badge")}))))})))
(defn import-badge [state]
  (let [assertion-url (:assertion-url @state)]
    (ajax/POST
      (path-for "/obpv1/badge/import_badge_with_assertion")
      {:params {:assertion (s/trim assertion-url)}
       :handler (fn [data]
                  (do
                    (swap! state assoc :status "form")
                    (m/modal! (upload-modal data)
                              (if (= (:status data) "success")
                                {:hidden #(navigate-to "/badge")}))))})))

(defn upload-info []
  [:div
   [:p
    (t :badge/Uploadbadgesfrominfo1) ":"]
   [:ul
    [:li {:dangerouslySetInnerHTML
          {:__html (str (t :badge/Uploadbadgesfrominfo2) ". " (t :badge/Uploadbadgesfrominfo3))}}]]

   [:p
    (t :badge/Uploadbagesfromresult1) " "
    [:a {:href (path-for "/badge/mybadges")} (t :badge/Mybadges)] " " (t :badge/page) ". "
    (t :badge/Uploadbagesfromresult2) "  "
    [:a {:href (path-for "/badge/mybadges")} (t :badge/Mybadges)]
    " " (t :badge/Uploadbagesfromresult3) "."]])

(defn assertion-upload-info []
  [:div
   [:p (t :badge/Importbadgeswithassertioninfo1)]
   [:ul
    [:li {:dangerouslySetInnerHTML
          {:__html (str (t :badge/Importbadgeswithassertioninfo2) " " (t :badge/Importbadgeswithassertioninfo3))}}]]
   [:p
    (t :badge/Uploadbagesfromresult1) " "
    [:a {:href (path-for "/badge/mybadges")} (t :badge/Mybadges)] " " (t :badge/page) ". "
    (t :badge/Uploadbagesfromresult2) "  "
    [:a {:href (path-for "/badge/mybadges")} (t :badge/Mybadges)]
    " " (t :badge/Uploadbagesfromresult3) "."]])

(defn badge-file-upload-content [state]
  (let [status  (:status @state)]
    [:div
     [:h2.uppercase-header (t :badge/Uploadbadgefrom)]
     [upload-info]
     (cond
       (= "loading" status) [:div.ajax-message
                             [:i {:class "fa fa-cog fa-spin fa-2x "}]
                             [:span (str (t :core/Loading) "...")]]
       :else                [:span {:class "btn btn-primary btn-file"}
                             [:input {:type       "file"
                                      :name       "file"
                                      :on-change  #(send-file state)
                                      :accept     "image/png, image/svg+xml"}] (t :badge/Browse)])
     [:br]]))

(defn assertion-url-upload-content [state]
  (let [assertion-url (cursor state [:assertion-url])
        status  (:status @state)]
    [:div
     [:h2.uppercase-header (t :badge/Importbadgeswithassertion)]
     [assertion-upload-info]
     (cond
       (= "importing" status) [:div.ajax-message
                               [:i {:class "fa fa-cog fa-spin fa-2x "}]
                               [:span (str (t :core/Loading) "...")]]
       :else                [:div {:id "assertion-textfield" :class "form-group"}
                             [:div {:style {:margin-top "15px"}}
                              [input/text-field {:name "input-assertion-url" :atom assertion-url :password? false :placeholder "http://"}]
                              [:button {:class "btn btn-primary"
                                        :on-click #(do
                                                     (swap! state assoc :status "importing")
                                                     (import-badge state))
                                        :disabled (not (url? @assertion-url))}
                                       (t :badge/ImportBadge)]]])
     [:br]]))
