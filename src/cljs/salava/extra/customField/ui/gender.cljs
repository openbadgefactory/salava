(ns salava.extra.customField.ui.gender
  (:require
   [reagent.core :refer [atom cursor create-class]]
   [salava.core.ui.ajax-utils :as ajax]
   [salava.core.ui.helper :refer [path-for]]
   [salava.core.i18n :refer [t]]
   [salava.extra.customField.ui.helper :as h]))

(def genders ["male" "female" "other"])

(defn init-field-value [field-atom]
  (ajax/GET
   (path-for "/obpv1/customField/gender" true)
   {:handler (fn [data]
               (reset! field-atom #_(cursor state [:gender]) data))}))

(defn update-gender-setting [gender state]
  (reset! (cursor state [:gender]) gender)
  (ajax/POST
   (path-for "/obpv1/customField/gender" true)
   {:params {:gender gender}
    :handler (fn [data]
               (when (= "success" (:status data))
                 (init-field-value (cursor state [:gender]))))}))

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
   (init-field-value (cursor state [:gender]))
   (fn []
     (let [gender-atom (cursor state [:gender])
           visible (cursor state [:visible])]
      (when @visible
        [:div.form-group
         [:span._label.col-md-3 {:for "genderForm"} (t :extra-customField/Gender)]
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

(defn ^:export init_field_value [field-atom]
  (ajax/GET
   (path-for "/obpv1/customField/gender" true)
   {:handler (fn [data]
               (reset! field-atom data))
    :finally (fn [])}))
