(ns salava.badge.ui.upload
  (:require [reagent.core :refer [atom cursor]]
            [reagent-modals.modals :as m]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.layout :as layout]
            [salava.core.ui.helper :refer [navigate-to path-for not-activated?]]
            [salava.core.ui.notactivated :refer [not-activated-banner]]
            [salava.core.ui.error :as err]
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
                                {:hide #(navigate-to "/badge")}))))})))
(defn send-assertion [state]
  (let [assertion-url (:assertion-url @state)]
    (ajax/POST
      (path-for "/obpv1/badge/badge_via_assertion")
      {:params {:assertion (s/trim assertion-url)}
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

(defn assertion-upload-info []
  [:div
   [:p (t :badge/Uploadbadgesviaassertioninfo1)]
   [:ol
    [:li {:dangerouslySetInnerHTML
          {:__html (t :badge/Uploadbadgesviaassertioninfo2)}}]
    [:li {:dangerouslySetInnerHTML
          {:__html (t :badge/Uploadbadgesviaassertioninfo3)}}]]
   [:p
    (t :badge/Uploadbagesfromresult1) " "
    [:a {:href (path-for "/badge/mybadges")} (t :badge/Mybadges)] " " (t :badge/page) ". "
    (t :badge/Uploadbagesfromresult2) "  "
    [:a {:href (path-for "/badge/mybadges")} (t :badge/Mybadges)]
    " " (t :badge/Uploadbagesfromresult3) "."]])

(defn badge-file-upload-content [state]
  (let [status  (:status @state)]
    [:div
     [:h1.uppercase-header (t :badge/Uploadbadgesfrom)]
     [upload-info]
     (cond
       (= "loading" status) [:div.ajax-message
                             [:i {:class "fa fa-cog fa-spin fa-2x "}]
                             [:span (str (t :core/Loading) "...")]]
       :else                [:form {:id "form"}
                             [:input {:type       "file"
                                      :aria-label "Choose file"
                                      :name       "file"
                                      :on-change  #(send-file state)
                                      :accept     "image/png, image/svg+xml"}]])]))

(defn assertion-url-upload-content [state]
  (let [assertion-url (cursor state [:assertion-url])
        status  (:status @state)]
    [:div
     [:h1.uppercase-header (t :badge/Uploadbadgesviaassertion)]
     [assertion-upload-info]
     (cond
       (= "loading" status) [:div.ajax-message
                             [:i {:class "fa fa-cog fa-spin fa-2x "}]
                             [:span (str (t :core/Loading) "...")]]
       :else                [:div.form-group.flip
                             [:div {:style {:margin-top "15px"}}
                              [input/text-field {:name "input-assertion-url" :atom assertion-url :password? false}]
                              [:button {:class "btn btn-primary"
                                        :on-click #(do
                                                     (swap! state assoc :status "loading")
                                                     (send-assertion state))
                                        } (t :file/Upload)]]])]))

(defn content [state]
  (let [status  (:status @state)
        assertion-url (cursor state [:input-assertion-url])
        selection-atom (cursor state [:input-upload-method])]

    [:div {:class "badge-upload"}
     [m/modal-window]

     (if  (not-activated?)
       (not-activated-banner)
       [:div
        [:select {:id        "input-upload-method"
                  :class     "form-control"
                  :value     (or @selection-atom "")
                  :on-change #(reset! selection-atom (.-target.value %))}
         [:option {:value "file"
                   :key   "file"}
          (t :badge/Imagefile)]
         [:option {:value "assertion"
                   :key   "assertion"}
          (t :badge/Assertionurl)]]

        (if (= @selection-atom "assertion")
          (assertion-url-upload-content state)
          (badge-file-upload-content state))]

       #_[:div
          [upload-info]
          (cond
            (= "loading" status) [:div.ajax-message
                                  [:i {:class "fa fa-cog fa-spin fa-2x "}]
                                  [:span (str (t :core/Loading) "...")]]
            :else                [:form {:id "form"}
                                  [:input {:type       "file"
                                           :aria-label "Choose file"
                                           :name       "file"
                                           :on-change  #(send-file state)
                                           :accept     "image/png, image/svg+xml"}]])])]))

(defn init-data [state]
  (ajax/GET (path-for "/obpv1/user/public-access")
            {:handler (fn [data]
                        (swap! state assoc :permission "success"))}
            (fn [] (swap! state assoc :permission "error"))))

(defn handler [site-navi]
  (let [state (atom {:status "form"
                     :permission "initial"})]
    (init-data state)
    (fn []
      (cond
        (= "initial" (:permission @state)) (layout/default site-navi [:div])
        (= "success" (:permission @state)) (layout/default site-navi (content state))
        :else (layout/default site-navi (err/error-content))))))
