(ns salava.extra.customField.ui.organization
  (:require
   [reagent.core :refer [cursor atom]]
   [reagent-modals.modals :as m]
   [salava.extra.customField.ui.helper :as h]
   [salava.core.ui.ajax-utils :as ajax]
   [salava.core.ui.helper :refer [path-for]]
   [salava.core.i18n :refer [t]]))

(defn init-organizations [state]
  (ajax/POST
   (path-for "/obpv1/customField/org/list")
   {:handler (fn [data]
               (reset! (cursor state [:orgs]) data))}))

(defn init-field-value [field-atom]
 (ajax/POST
  (path-for "/obpv1/customField/org/value")
  {:handler (fn [data]
              (reset! field-atom #_(cursor state [:org]) data))}))

(defn organization-field-registration [form-state]
  (let [visible (h/field-enabled? "organization")
        compulsory-field? (h/compulsory-field? "organization")
        state (atom {:visible visible :compulsory? compulsory-field? :org nil :orgs []})]
   (init-organizations state)
   (fn []
    (let [compulsory? (cursor state [:compulsory?])
          visible (cursor state [:visible])
          org-atom (cursor state [:org])
          organizations (cursor state [:orgs])]
      (when @visible
        [:div.col-md-12
         [:div.form-group
          [:label.col-sm-4 {:for "select-org"}
           (t :extra-customField/Organization) (when @compulsory? [:span.form-required " *"])]
          [:div.col-sm-8
           (reduce
            (fn [r org]
              (conj r [:option {:value (:name org)} (:name org)]))
            [:select#select-org.form-control
             {:on-change (fn [x]
                           (do
                            (reset! org-atom (-> x .-target .-value))
                            (swap! (cursor form-state [:custom-fields]) assoc :organization @org-atom)
                            (ajax/POST
                             (path-for "/obpv1/customField/org/register" true)
                             {:params {:organization @org-atom}
                              :handler (fn [data])})))}
             [:option {:value nil :disabled (not (clojure.string/blank? @org-atom))} (t :extra-customField/SelectOrganization)]]
            @organizations)]]])))))

(defn organization-field []
  (let [visible (h/field-enabled? "organization")
        state (atom {:org nil :visible visible :orgs []})]
   (init-organizations state)
   (init-field-value (cursor state [:org]))
   (fn []
    (let [orgs (cursor state [:orgs])
          org-atom (cursor state [:org])
          visible (cursor state [:visible])]
     (when @visible
      [:div.form-group
       [:label.col-md-3 {:for "select-org"} (t :extra-customField/Organization)]
       [:div.col-md-9
        (reduce
         (fn [r org]
           (conj r [:option {:value (:name org) :selected (= (:name org) @org-atom)} (:name org)]))
         [:select#select-org.form-control
          {:on-change (fn [x]
                        (do
                         (reset! org-atom (-> x .-target .-value))
                         (ajax/POST
                          (path-for "/obpv1/customField/org" true)
                          {:params {:org @org-atom}
                           :handler (fn [data]
                                      (when (= (:status data) "success")
                                        (init-field-value org-atom)))})))}
          [:option {:value nil :disabled (not (clojure.string/blank? @org-atom))} (t :extra-customField/SelectOrganization)]]
         @orgs)]])))))

(defn ^:export init_field_value [field-atom]
 (ajax/POST
  (path-for "/obpv1/customField/org/value")
  {:handler (fn [data]
              (reset! field-atom data))}))
