(ns salava.badgeIssuer.ui.modal
  (:require
    [clojure.string :refer [blank?]]
    [salava.core.i18n :refer [t]]))

(defn preview-badge [badge]
 (let [{:keys [name description tags criteria image]} badge]
   [:div {:id "badge-info" :class "row flip"}
    [:div {:class "col-md-3"}
     [:div.badge-image
      [:img {:src image :alt name}]]]
    [:div {:class "col-md-9 badge-info view-tab" :style {:display "block"}}
     [:div.row
      [:div {:class "col-md-12"}
       [:h1.uppercase-header name]
       [:div.description description]]]

     (when-not (blank? criteria)
       [:div {:class "row criteria-html"}
        [:div.col-md-12
         [:h2.uppercase-header (t :badge/Criteria)]
         [:div {:dangerouslySetInnerHTML {:__html criteria}}]]])

     [:div.row
      (if (not (empty? tags))
        (into [:div.col-md-12 {:style {:margin "10px 0"}}]
              (for [tag tags]
                [:span {:id "tag"
                        :style {:font-weight "bold" :padding "0 2px"}}
                 (str "#" tag)])))]]]))


(defn preview-badge-handler [params]
  (let [badge (:badge params)]
    (fn []
      (preview-badge badge))))


(def ^:export modalroutes
  {:badgeIssuer {:preview preview-badge-handler}})
