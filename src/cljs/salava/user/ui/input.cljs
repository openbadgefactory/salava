(ns salava.user.ui.input
  (:require [salava.core.countries :refer [all-countries-sorted]]
            [salava.core.i18n :refer [t]]
            [salava.core.ui.helper :refer [input-valid?]]
            [salava.user.schemas :as schemas]))

(defn text-field [input-data]
  (let [{:keys [name atom placeholder password? error-message-atom]} input-data]
    [:input {:class       "form-control"
             :id          (str "input-" name)
             :name        name
             :type        (if password? "password" "text")
             :placeholder placeholder
             :on-change   #(do
                             (reset! atom (.-target.value %))
                             (if error-message-atom
                               (reset! error-message-atom (:message ""))))
             :value       @atom}]))

(defn radio-button-selector [values atom]
  (into [:div]
        (for [value values]
          [:label.radio-inline
           [:input {:type      "radio"
                    :name      "value"
                    :value     value
                    :default-checked   (= @atom value)
                    :on-change  #(reset! atom value)}]
           (t (keyword (str "core/" value)))])))

(defn country-selector [atom]
  [:select {:id "input-country"
            :class "form-control"
            :value     @atom
            :on-change #(reset! atom (.-target.value %))}
   [:option {:value ""
             :key ""}
    "- " (t :user/Choosecountry) " -"]
   (for [[country-key country-name] (map identity all-countries-sorted)]
     [:option {:value country-key
               :key country-key} country-name])])

(defn email-valid? [email-address]
  (input-valid? (:email schemas/User) email-address))

(defn language-valid? [language]
  (input-valid? (:language schemas/User) language))

(defn password-valid? [password]
  (input-valid? (:password schemas/User) password))

(defn first-name-valid? [first-name]
  (input-valid? (:first_name schemas/User) first-name))

(defn last-name-valid? [last-name]
  (input-valid? (:last_name schemas/User) last-name))

(defn country-valid? [country]
  (input-valid? (:country schemas/User) country))
