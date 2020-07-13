(ns salava.extra.customField.ui.organization
  (:require
   [reagent.core :refer [cursor atom]]
   [reagent-modals.modals :as m]
   [salava.extra.customField.ui.helper :as h]
   [salava.core.ui.ajax-utils :as ajax]
   [salava.core.ui.helper :refer [path-for]]
   [salava.core.i18n :refer [t]]
   [reagent.session :as session]))

(defn init-organizations [state]
  (ajax/POST
   (path-for "/obpv1/customField/org/list")
   {:handler (fn [data]
               (reset! (cursor state [:orgs]) data))}))

(defn init-field-value [field-atom user-id]
 (let [url (if user-id (str "/obpv1/customField/org/value/" user-id) "/obpv1/customField/org/value")]
   (ajax/POST
    (path-for url true)
    {:handler (fn [data]
                (reset! field-atom #_(cursor state [:org]) data))})))

(defn organization-field-registration [form-state]
  (let [visible (h/field-enabled? "organization")
        compulsory-field? (h/compulsory-field? "organization")
        state (atom {:visible visible :compulsory? compulsory-field? :org nil :orgs [] :input-confirmed false})]
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

           [:div.input-group
            [:input.form-control
              {:type "text"
               :placeholder (t :extra-customField/SelectOrganization)
               :list "select-org"
               :on-change (fn [x]
                           (reset! (cursor state [:input-confirmed]) false)
                           (reset! org-atom (-> x .-target .-value)))}]


            [:div.input-group-btn
             [:button.btn.btn-primary
              {:style {:word-break "unset" :white-space "normal"}
               :type "button"
               :on-click #(do
                            (.preventDefault %)
                            (ajax/POST
                             (path-for "/obpv1/customField/org/register" true)
                             {:params {:organization @org-atom}
                              :handler (fn [data])

                              :finally (fn []
                                         (when-not (clojure.string/blank? @org-atom) (reset! (cursor state [:input-confirmed]) true))
                                         (swap! (cursor form-state [:custom-fields]) assoc :organization @org-atom))}))}
              (when @(cursor state [:input-confirmed]) [:i.fa.fa-check-circle.fa-fw.inline])
              [:span {:style {:display "inline-block" :margin "0 2px"}}  " OK"]]]




            (reduce
             (fn [r org]
               (conj r [:option {:value (:name org)} (:name org)]))
             [:datalist#select-org]
             (sort-by :name @organizations))]
           [:span.help-block.text-muted (t :extra-customField/Selectorganizationinstruction)]]]])))))


(defn organization-field []
 (let [visible (h/field-enabled? "organization")
       state (atom {:org nil :visible visible :orgs [] :input-confirmed false})]
    (init-organizations state)
    (init-field-value (cursor state [:org]) nil)
    (fn []
      (let [orgs (cursor state [:orgs])
            org-atom (cursor state [:org])
            visible (cursor state [:visible])]
       (when @visible
        [:div.form-group
         [:label.col-md-3 {:for "select-org"} (t :extra-customField/Organization)]
         [:div.col-md-9
          [:div.input-group
           [:input.form-control.dropdowninput
            {:type "text"
             :placeholder (t :extra-customField/SelectOrganization)
             :value @org-atom
             :list "organization-list"
             :on-change (fn [x]
                         (reset! (cursor state [:input-confirmed]) false)
                         (reset! org-atom (-> x .-target .-value)))}]

           [:span.input-group-btn
            [:button.btn.btn-primary.btn-bulky
             {:style {:margin-top "unset"}
              :type "button"
              :on-click #(do
                           (.preventDefault %)
                           (ajax/POST
                            (path-for "/obpv1/customField/org" true)
                            {:params {:organization @org-atom}
                             :handler (fn [data]
                                        (when (= (:status data) "success")
                                          (init-field-value org-atom nil)))
                             :finally (fn []
                                         (when-not (clojure.string/blank? @org-atom) (reset! (cursor state [:input-confirmed]) true)))}))}

             (when @(cursor state [:input-confirmed]) [:i.fa.fa-check-circle.fa-fw]) "OK"]]
           (reduce
            (fn [r org]
              (conj r [:option {:value (:name org)} (:name org)]))
            [:datalist#organization-list]
            (sort-by :name @orgs))]
          [:span.help-block.text-muted (t :extra-customField/Selectorganizationinstruction)]]])))))

(defn ^:export init_custom_field_value [field-atom]
 (ajax/POST
  (path-for "/obpv1/customField/org/value")
  {:handler (fn [data]
              (reset! field-atom data))}))

(defn ^:export admintool_custom_field [user-id]
  (let [visible (h/field-enabled? "organization")
        state (atom {:org nil :visible visible :orgs []})]
    (init-field-value (cursor state [:org]) user-id)
    (fn []
      (let [org-atom (cursor state [:org])
            visible (cursor state [:visible])]
        (when @visible
          [:div.row
           [:span._label.col-xs-4 (t :extra-customField/Organization) ": "]
           [:div.col-xs-6 (or @org-atom (t :extra-customField/Notset))]])))))

(defn ^:export custom_field_filter [content-state fetch-fn]
  (let [visible (h/field-enabled? "organization")
        state (atom {:visible visible :org (session/get :site-name) :orgs []})]
    (init-organizations state)
    (fn []
     (let [visible (cursor state [:visible])
           org-atom (cursor state [:org])
           orgs (cursor state [:orgs])
           custom-filters (cursor content-state [:custom-field-filters])]

       (when @visible
        [:div.form-group
         [:span._label.filter-opt {:class "control-label col-sm-2"} (t :extra-customField/Organization)]
         [:div.col-md-10
          (reduce
           (fn [r org]
             (conj r [:option {:value (:name org) :selected (= (:name org) @org-atom)} (:name org)]))
           (conj [:select#select-org.form-control
                   {:on-change (fn [x]
                                (do
                                  (reset! org-atom (-> x .-target .-value))
                                  (if (clojure.string/blank?  @org-atom)
                                    (reset! custom-filters (dissoc @custom-filters :organization))
                                    (reset! custom-filters (assoc @custom-filters :organization @org-atom)))
                                  (fetch-fn)))}
                   [:option {:value ""} (t :extra-customField/All)]]
                 [:option {:value "notset"} (t :extra-customField/notset)])
           @orgs)]])))))
