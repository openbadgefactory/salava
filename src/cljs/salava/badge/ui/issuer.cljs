(ns salava.badge.ui.issuer
  (:require [reagent.core :refer [atom]]
            [salava.core.i18n :refer [t]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.helper :refer [path-for private?]]
            [salava.badge.ui.endorsement :as endr]))



(defn init-issuer-content [state issuer-id]
  (ajax/GET
    (path-for (str "/obpv1/badge/issuer/" issuer-id))
    {:handler (fn [data] (reset! state data))}))

(defn- issuer-image [path]
  (when (not-empty path)
    [:img.profile-picture
     {:src (if (re-find #"^file/" path) (str "/" path) path)
      :style {:width "50px"}}]))

(defn content [issuer-id]
  (let [state (atom {:issuer nil :endorsement []})]
    (init-issuer-content state issuer-id)
    (fn []
      (let [{:keys [name description email url image_file]} @state]
        [:div.row {:id "badge-contents"}
         [:div.col-xs-12
          [:h2.uppercase-header
           (issuer-image image_file)
           " "
           name]

          [:div.row
           [:div {:class "col-md-9 col-sm-9 col-xs-12"}
            (if (not-empty url)
              [:div {:class "row"}
               [:div.col-xs-12
                [:a {:target "_blank" :rel "noopener noreferrer" :href url} url]]])

            (if (not-empty email)
              [:div {:class "row"}
               [:div.col-xs-12 {:style {:margin-bottom "20px"}}
                [:span [:a {:href (str "mailto:" email)} email]]]])

            (if (not-empty description)
              [:div {:class "row about"}
               [:div.col-xs-12 {:dangerouslySetInnerHTML {:__html description}}]])]]

          (when-not (empty? (:endorsement @state))
            [:div.row
             [:div.col-xs-12
              [:hr]
              [:h4 {:style {:margin-bottom "20px"}} (t :badge/IssuerEndorsedBy)]
              (into [:div]
                    (for [endorsement (:endorsement @state)]
                      (endr/endorsement-row endorsement)))]])]]))))

;;TODO
(defn creator-content [creator-id]
  [:div ""]

  )
