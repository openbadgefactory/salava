(ns salava.extra.customField.ui.manage
  (:require
   [reagent.core :refer [atom cursor]]
   [salava.core.ui.layout :as layout]
   [salava.core.ui.ajax-utils :as ajax]
   [salava.core.ui.helper :refer [path-for]]
   [salava.core.i18n :refer [t]]
   [reagent-modals.modals :as m]
   [clojure.string :refer [trim]]))

(defn init-organizations [state]
  (ajax/POST
   (path-for "/obpv1/customField/org/list")
   {:handler (fn [data]
               (reset! (cursor state [:orgs]) data))}))

(defn add-organization [state]
 (let [orgs @(cursor state [:orgs])
       org-atom (cursor state [:org])]
  (if (some #(= (trim @org-atom) (:name %)) orgs)
    (m/modal!
     [:div
      [:div.modal-header
       [:button {:type "button"
                 :class "close"
                 :data-dismiss "modal"
                 :aria-label "OK"}
        [:span {:aria-hidden "true"
                :dangerouslySetInnerHTML {:__html "&times;"}}]]]
      ;[:h4.modal-title (translate-text message)]]
      [:div.modal-body
       [:div.alert.alert-warning
        (t :extra-customField/Nameexistsinlist)]]
      [:div.modal-footer
       [:button {:type "button"
                 :class "btn btn-primary"
                 :data-dismiss "modal"}
        "OK"]]]
     {:hide (fn [] (reset! org-atom ""))})

   (ajax/POST
    (path-for "/obpv1/customField/config/update/orglist")
    {:params {:org [(trim @org-atom)]}
     :handler (fn []
                (init-organizations state)
                (reset! org-atom  ""))}))))

(defn delete-organization [id state]
  (ajax/DELETE
    (path-for (str "/obpv1/customField/config/update/orglist/"id) true)
    {:handler (fn [data]
               (when (= "success" (:status data))
                (init-organizations state)))}))

(defn content [state]
 (let [orgs @(cursor state [:orgs])]
  [:div
   [m/modal-window]
   [:div.panel.panel-default
    [:div.panel-heading
     (t :extra-customField/Organizationlist) (str "(" (count orgs) ")")]
    [:div.panel-body
     [:p (t :extra-customField/Aboutaddingorganizations)]
     [:hr.line]
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
        orgs)
      [:div (t :extra-customField/Yettoaddorg)])]

    [:div.panel-footer
     [:div.input-group.col-md-6.col.sm-12
        [:input.form-control
         {:style {:max-width "unset"}
          :type "text"
          :id "org"
          ;:value (process-time @(cursor state [:space :valid_until]))
          :on-change #(do
                        (reset! (cursor state [:org]) (.-target.value %)))}]
        [:span.input-group-btn
         [:button.btn.btn-primary.input-btn
          {:type "button"
           :style {:margin-top  "unset"}
           :value @(cursor state [:org])
           :on-click #(add-organization state)
           :disabled (clojure.string/blank? @(cursor state [:org]))}
          (t :core/Add)]]]]]]))

(defn handler [site-navi]
  (let [state (atom {:org "" :orgs []})]
   (init-organizations state)
   (fn []
    (layout/default site-navi [content state]))))
