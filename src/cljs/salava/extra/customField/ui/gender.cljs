(ns salava.extra.customField.ui.gender
  (:require
   [reagent.core :refer [atom cursor create-class]]
   [salava.core.ui.ajax-utils :as ajax]
   [salava.core.ui.helper :refer [path-for]]
   [salava.core.i18n :refer [t]]
   [salava.extra.customField.ui.helper :as h]))

(def genders ["male" "female" "other"])

(defn init-user-gender [state]
  (ajax/GET
   (path-for "/obpv1/customField/gender" true)
   {:handler (fn [data]
               (reset! (cursor state [:gender]) data))}))

(defn update-gender-setting [gender state]
  (reset! (cursor state [:gender]) gender)
  (ajax/POST
   (path-for "/obpv1/customField/gender" true)
   {:params {:gender gender}
    :handler (fn [data]
               (when (= "success" (:status data))
                 (init-user-gender state)))}))

(defn gender-form-registration []
  (let [visible (h/field-enabled? "gender")
        compulsory-field? (h/compulsory-field? "gender")
        state (atom {:visible visible :compulsory? compulsory-field?})]

     (fn []
      (let [compulsory? (cursor state [:compulsory?])
            visible (cursor state [:visible])]
       (when @visible
         [:div.form-group.margin-0.col-sm-6
          [:span._label (t :extra-customField/Gender) (when @compulsory? [:span.form-required " *"])]
          #_[:label {:class ""
                     :for   "input-last-name"}
             (t :user/Lastname)
             [:span.form-required " *"]]
          [:div {:class (str "form-bar " (if (input/last-name-valid? @last-name-atom) "form-bar-success" "form-bar-error"))}
           [input/text-field {:name "last-name" :atom last-name-atom}]]])))))


(defn gender-form-modal [])

(defn gender-form []
 (let [visible (h/field-enabled? "gender")
       state (atom {:gender nil :visible visible})]
   (init-user-gender state)
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
