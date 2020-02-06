(ns salava.badgeIssuer.ui.criteria
  (:require
   [reagent.core :refer [atom cursor create-class]]
   [reagent.session :as session]
   [salava.core.ui.ajax-utils :as ajax]
   [salava.core.ui.helper :refer [path-for disable-background-image]]
   [salava.core.ui.layout :as layout]))

(defn init-data [id state]
 (ajax/GET
  (path-for (str "/obpv1/selfie/criteria/" id))
  {:handler (fn [data]
              (reset! state data))}))

(defn logo []
  [:div.col-md-8.col-md-offset-2 {:style {:margin "40px 0"}}
   [:div {:class "logo-image logo-image-url img-responsive"
          :title "OBP logo"
          :aria-label "OBP logo"}]
   #_[:div {:class "logo-image logo-image-icon-url visible-xs visible-sm  visible-md"}]])

(defn content [state]

   (create-class
    {:reagent-render
     (fn []
       (let [{:keys [id criteria_content name description image_file]} @state]
        [:div
         [:div#content
          [:div.container.main-container
           [:div.row
            [:div.col-md-2.col-sm-3]
            [:div.col-md-10.col-sm-9
             [:div.col-md-12
              [:div#selfie_criteria
               [:div.row
                (logo)]
               [:div.panel.panel-default.thumbnail
                [:div.panel-body
                 [:div.row.flip
                  [:div.col-md-3.badge-image
                   [:img {:src (str "/" image_file) :alt ""}]]
                  [:div.col-md-9
                    [:div.col-md-12
                     [:h1.uppercase-header name]
                     [:div.description.col description]
                     [:hr.border]
                     [:div.criteria-background
                      {:dangerouslySetInnerHTML {:__html criteria_content}}]]]]]]]]]]]]
         (layout/footer nil)]))
     :component-will-mount
     (fn [] (disable-background-image))}))



(defn handler [site-navi params]
  (let [id (:id params)
        state (atom {:id id})]
    (init-data id state)
    (fn []
      (layout/embed-page [content state]))))
