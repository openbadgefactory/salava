(ns salava.extra.customField.ui.gender
  (:require
   [reagent.core :refer [atom cursor create-class]]
   [salava.core.ui.ajax-utils :as ajax]
   [salava.core.ui.helper :refer [path-for]]
   [salava.core.i18n :refer [t]]
   [salava.extra.customField.ui.helper :as h]
   [salava.core.ui.grid :as grid]))

(def genders ["male" "female" "other"])

(defn init-value [field-atom user-id]
  (let [url (if user-id (str "/obpv1/customField/gender/value/" user-id) "/obpv1/customField/gender/value")]
    (ajax/POST
     (path-for url true)
     {:handler (fn [data]
                 (reset! field-atom data))})))

(defn update-gender-setting [gender state]
  (reset! (cursor state [:gender]) gender)
  (ajax/POST
   (path-for "/obpv1/customField/gender" true)
   {:params {:gender gender}
    :handler (fn [data]
               (when (= "success" (:status data))
                 (init-value (cursor state [:gender]) nil)))}))

(defn save-new-user-gender [gender state]
 (reset! (cursor state [:gender]) gender)
 (ajax/POST
  (path-for "/obpv1/customField/gender/register" true)
  {:params {:gender gender}
   :handler (fn [data])}))

(defn gender-field-registration [form-state]
  (let [visible (h/field-enabled? "gender")
        compulsory-field? (h/compulsory-field? "gender")
        state (atom {:visible visible :compulsory? compulsory-field? :gender ""})]
     (fn []
      (let [compulsory? (cursor state [:compulsory?])
            visible (cursor state [:visible])
            gender-atom (cursor state [:gender])]

       (when @visible
        [:div.col-md-12
         [:div.form-group
          [:span._label.col-sm-4 (t :extra-customField/Gender) (when @compulsory? [:span.form-required " *"])]
          [:div.col-sm-8
           (into [:fieldset
                  [:legend.sr-only  (t :extra-spaces/Gender)]]
                 (map (fn [value]
                        [:label.radio-inline {:for (str "gender-" value)}
                         [:input {:type      "radio"
                                  :name      (str "radio" name)
                                  :value     value
                                  :checked (= @gender-atom value)
                                  :on-change  #(do
                                                 (swap! (cursor form-state [:custom-fields]) assoc :gender value)
                                                 (save-new-user-gender value state))
                                  :id (str "language-" value)}]

                         (t (keyword (str "extra-customField/" value)))]) genders))]]])))))

(defn gender-field []
 (let [visible (h/field-enabled? "gender")
       state (atom {:gender nil :visible visible})]
   (init-value (cursor state [:gender]) nil)
   (fn []
     (let [gender-atom (cursor state [:gender])
           visible (cursor state [:visible])]
      (when @visible
        [:div.form-group
         [:span._label.col-md-3 (t :extra-customField/Gender)]
         [:div.col-md-9
           (into [:fieldset
                  [:legend.sr-only  (t :extra-spaces/Gender)]]
                 (map (fn [value]
                        [:label.radio-inline {:for (str "gender-" value)}
                         [:input {:type      "radio"
                                  :name      (str "radio" name)
                                  :value     value
                                  ;:default-checked (= @gender-atom value)
                                  :checked (= @gender-atom value)
                                  :on-change  #(update-gender-setting value state)
                                  :id (str "language-" value)}]

                         (t (keyword (str "extra-customField/" value)))]) genders))]])))))

(defn ^:export admintool_custom_field [user-id]
  (let [visible (h/field-enabled? "gender")
        state (atom {:gender nil :visible visible})]
    (init-value (cursor state [:gender]) user-id)
    (fn []
      (let [gender-atom (cursor state [:gender])
            visible (cursor state [:visible])]
        (when @visible
          [:div.row
           [:span._label.col-xs-4 (t :extra-customField/Gender) ": "]
           [:div.col-xs-6 (or @gender-atom (t :extra-customField/Notset))]])))))

(defn ^:export init_custom_field_value [field-atom]
  (ajax/POST
   (path-for "/obpv1/customField/gender/value" true)
   {:handler (fn [data]
               (reset! field-atom data))
    :finally (fn [])}))

(defn ^:export custom_field_filter [content-state fetch-fn]
 (let [visible (h/field-enabled? "gender")
       state (atom {:visible visible :gender "All"})]
  (fn []
    (let [visible (cursor state [:visible])
          gender-atom (cursor state [:gender])
          custom-filters (cursor content-state [:custom-field-filters])]
      (when @visible
        [:div.form-group
         [:span._label.filter-opt {:class "control-label col-sm-2"} (t :extra-customField/Gender)]
         [:div.col-md-10
           (into [:fieldset
                  [:legend.sr-only  (t :extra-spaces/Gender)]]
                 (map (fn [value]
                        [:label.radio-inline {:for (str "gender-" value)}
                         [:input {:type      "radio"
                                  :value     value
                                  :checked (= @gender-atom value)
                                  :on-change  #(do (reset! gender-atom value)
                                                   (if (= "All" value)
                                                     (reset! custom-filters (dissoc @custom-filters :gender))
                                                     (reset! custom-filters (assoc @custom-filters :gender @gender-atom)))
                                                   (fetch-fn))
                                  :id (str "language-" value)}]

                         (t (keyword (str "extra-customField/" value)))])  (into ["All"] (conj genders "notset"))))]])))))

(defn ^:export custom_field_filter_space [content-state list-key fetch-fn]
   (let [visible (h/field-enabled? "gender")
         state (atom {:visible visible :gender "All"})]
    (fn []
      (let [visible (cursor state [:visible])
            gender-atom (cursor state [:gender])
            custom-filters (cursor content-state [:custom-field-filters])]
       (when @visible
         [:div.form-group
          [:span._label.filter-opt {:class "control-label col-sm-2"} (t :extra-customField/Gender)]
          [:div.col-md-10
            (into [:fieldset
                   [:legend.sr-only  (t :extra-spaces/Gender)]]
                  (map (fn [value]
                         [:label.radio-inline {:for (str "gender-" value)}
                          [:input {:type      "radio"
                                   :value     value
                                   :checked (= @gender-atom value)
                                   :on-change  #(do (reset! gender-atom value)
                                                    (if (= "All" value)
                                                      (reset! custom-filters (dissoc @custom-filters :gender))
                                                      (reset! custom-filters (assoc @custom-filters :gender @gender-atom)))
                                                    (when fetch-fn (fetch-fn)))
                                   :id (str "language-" value)}]

                          (t (keyword (str "extra-customField/" value)))])  (into ["All"] (conj genders "notset"))))]])))))
