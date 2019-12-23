(ns salava.user.ui.input
  (:require [salava.core.countries :refer [all-countries-sorted]]
            [reagent.core :refer [atom]]
            [salava.core.i18n :refer [t]]
            [salava.core.ui.helper :refer [input-valid?]]
            [salava.core.helper :refer [dump]]
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
                             (if error-message-atom(reset! error-message-atom (:message ""))))

             :value       @atom}]))



(defn radio-button-selector [name values atom]
  (into [:div]
        (map (fn [value]
               [:label.radio-inline
                [:input {:type      "radio"
                         :name      name
                         :value     value
                         :default-checked (= @atom value)
                         :on-change  #(reset! atom value)
                         :id "languages"}]

                (t (keyword (str "core/" value)))]) values)))


(defn select-selector [values atom init-text]
  (into [:select {:id        "input-language"
                  :class     "form-control"
                  :value     @atom
                  :on-change #(reset! atom (.-target.value %))}
         [:option {:value ""
                   :key   ""}
          (str "- " init-text " -")]
         (doall (for [value values]
                  [:option {:value value
                            :key   value} (t (keyword (str "core/" value)))]))]))

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

(defn email-whitelist [values email-atom]
  (let [current-value (atom (first values))
        text-atom (atom "")
        ]
    (fn []
      [:div.input-group
       [:input {:class       "form-control"
                :id          (str "input-emailwhitelist")
                :name        "email-text"
                :type        "text"

                :on-change   #(do
                                (reset! text-atom (.-target.value %))
                                (reset! email-atom (str @text-atom @current-value))
                               ; (if error-message-atom(reset! error-message-atom (:message "")))
                                )
                :value       @text-atom}]
       (if (= 1 (count values))
         [:span {:class "input-group-addon"}  @current-value]
         [:div {:class "input-group-btn"}
          [:button {:type "button" :class "btn btn-default dropdown-toggle" :data-toggle "dropdown" :aria-haspopup "true" :aria-expended "false"} @current-value  [:span.caret]]
          [:ul {:class "dropdown-menu dropdown-menu-right"}
           (doall
            (for [value values]
              [:li {:key value}
               [:a {:href "#" :on-click #(do
                                           (reset! current-value value)
                                           (reset! email-atom (str @text-atom @current-value))
                                           (.preventDefault %))} value]]))]])])))

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
