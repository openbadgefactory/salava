(ns salava.extra.customField.ui.manage
  (:require
   [reagent.core :refer [atom cursor]]
   [salava.core.ui.layout :as layout]
   [salava.core.ui.ajax-utils :as ajax]
   [salava.core.ui.helper :refer [path-for]]
   [salava.core.i18n :refer [t]]
   [reagent-modals.modals :as m]
   [clojure.string :refer [trim split-lines]]))

(defn init-organizations [state]
  (ajax/POST
   (path-for "/obpv1/customField/org/list")
   {:handler (fn [data]
               (reset! (cursor state [:orgs]) data))}))

(defn add-organization [state]
 (let [orgs @(cursor state [:orgs])
       org-atom (cursor state [:org])]
  (ajax/POST
   (path-for "/obpv1/customField/config/update/orglist")
   {:params {:org @org-atom}
    :handler (fn []
               (init-organizations state)
               (reset! org-atom  []))})))

(defn delete-organization [id state]
  (ajax/DELETE
    (path-for (str "/obpv1/customField/config/update/orglist/"id) true)
    {:handler (fn [data]
               (when (= "success" (:status data))
                (init-organizations state)))}))

(defn content [state]
 (let [orgs @(cursor state [:orgs])
       orgs (->> @(cursor state [:orgs]) (filter #(re-find (re-pattern (str "(?i)" @(cursor state [:search]))) (:name %))))]
  [:div
   [m/modal-window]
   [:div.panel.panel-default
    [:div.panel-heading
     [:div.row
      [:div.col-md-12
       [:div.col-md-9 {:style {:margin "8px auto"}} [:span.panel-title [:strong (t :extra-customField/Organizationlist) (str " (" (count orgs) ")")]]]
       [:div.col-md-3
        [:input.form-control
          {:on-change #(reset! (cursor state [:search]) (.-target.value %))
           :type "text"
           :id "searchOrg"
           :placeholder (str (t :extra-customField/Search) "...")}]]]]]
    [:div.panel-body
     [:div.col-md-12
      [:p [:strong  (t :extra-customField/Aboutaddingorganizations)]]
      ;[:hr.border]
      [:div {:style {:max-height "700px" :overflow "auto"}}
       (if (seq orgs)
        (reduce
          #(conj %1 ^{:key (:id %)}[:li.list-group-item
                                    [:button.close
                                     {:type "button"
                                      :aria-label (t :core/Delete)
                                      :on-click (fn [] (delete-organization (:id %2) state))}
                                     [:span {:aria-hidden "true"
                                             :dangerouslySetInnerHTML {:__html "&times;"}}]]
                                    (:name %2)])
          [:ul.list-group]
          orgs))]]]

    [:div.panel-footer
     [:div.row
      [:div.col-md-12
        [:label {:for "org"}
          (t :extra-customField/Addorganization)]
        [:textarea.form-control
         {:rows 6
          :style {:max-width "unset"}
          :type "text"
          :id "org"
          :on-change #(do
                        (reset! (cursor state [:org]) (->> (split-lines (.-target.value %)) (mapv (fn [s] (trim s))))))}]
        [:button.btn.btn-primary
         {:type "button"
          :on-click #(add-organization state)
          :disabled (not (seq @(cursor state [:org])))}
         (t :core/Add)]]]]]]))


(defn handler [site-navi]
  (let [state (atom {:org [] :orgs [] :search ""})]
   (init-organizations state)
   (fn []
    (layout/default site-navi [content state]))))
