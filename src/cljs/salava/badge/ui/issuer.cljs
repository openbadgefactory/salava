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

(defn content [issuer-id]
  (let [state (atom {:issuer nil :endorsements []})]
    (init-issuer-content state issuer-id)
    (fn []
      (let [{:keys [name description email url image]} @state]
        [:div.row
         [:div.col-xs-12
          [:div.row.endorser-header
           [:div [:h1.uppercase-header name]]]

          [:div.row.endorser-info
           [:div {:class "col-md-9 col-sm-9 col-xs-12"}
            (if (not-empty description)
              [:div {:class "row about"}
               [:div.col-xs-12 [:b (t :badge/About) ":"]]
               [:div.col-xs-12 description]])

            (if (not-empty email)
              [:div {:class "row"}
               [:div.col-xs-12 [:b (t :badge/Contact)":"]]
               [:div.col-xs-12
                [:span [:a {:href (str "mailto:" email)} email]]]])

            (if (not-empty url)
              [:div {:class "row"}
               [:div.col-xs-12 [:b (t :badge/Website) ":"]]
               [:div.col-xs-12
                [:a {:target "_blank" :href url} name]]])]

           (when (not-empty image)
             [:div {:class "col-md-3 col-sm-3 col-xs-12"}
              [:div.profile-picture-wrapper
               [:img.profile-picture {:src (str "/" image)}]]])]

          (into [:div]
                (for [endorsement (:endorsement @state)]
                  (endr/endorsement-row endorsement)))]]))))

(defn creator-content [creator-id]
  [:div "TODO"]

  )
