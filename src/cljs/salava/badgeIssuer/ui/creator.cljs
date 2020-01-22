(ns salava.badgeIssuer.ui.creator
  (:require
   [clojure.string :refer [blank?]]
   [reagent.core :refer [atom create-class cursor]]
   [reagent.session :as session]
   [salava.core.ui.ajax-utils :as ajax]
   [salava.core.ui.helper :refer [path-for]]
   [salava.core.i18n :refer [t]]
   [salava.core.ui.layout :as layout]))


(defn init-data [state]
  (ajax/GET
    (path-for "/obpv1/issuer")
    {:handler (fn [data]
                (swap! state assoc :badge data
                                   :generating-image false))}))


(defn generate-image [state]
  (reset! (cursor state [:generating-image]) true)
  (ajax/GET
    (path-for "/obpv1/issuer/generate_image")
    {:handler (fn [{:keys [status url message]}]
                (when (= "success" status)
                  (reset! (cursor state [:badge :data-url]) url)
                  (reset! (cursor state [:generating-image]) false)))}))

(defn upload-image [])
(defn save-badge [])

(defn content [state]
  (let [{:keys [badge generating-image]} @state
        {:keys [uploaded-image data-url name]} badge]
    [:div#badge-creator
     [:h1.sr-only (t :badgeIssuer/badge-creator)]
     [:div.panel.thumbnail
      [:div.panel-heading
       [:span (t :badgeIssuer/badge-creator)]]
      [:div.panel-body
       [:h4 (t :badgeIssuer/Addbadgeimage)]
       [:div "Generate random image or upload badge image"]
       [:div.image-container
        (if-not @(cursor state [:generating-image])
          (if-not (blank? data-url)
           [:img {:src data-url :alt "generated image"}]
           [:img {:src (str "/" (:path uploaded-image)) :alt (or (:name uploaded-image) "File upload")}])
          [:span.fa.fa-spin.fa-cog.fa-2x])]]]]))


(defn handler [site-navi]
  (let [state (atom {:badge {:uploaded-image {:name nil :path nil}
                             :data-url nil
                             :name nil}
                     :generating-image true})]

    (init-data state)
    (fn []
      (layout/default site-navi (content state)))))
