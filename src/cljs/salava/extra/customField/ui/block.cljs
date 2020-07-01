(ns salava.extra.customField.ui.block
  (:require
   [salava.extra.customField.ui.gender :as g]
   [salava.extra.customField.ui.helper :as h]
   [salava.extra.customField.ui.organization :as org]
   [reagent.session :as session]
   [reagent.core :refer [cursor atom]]
   [salava.core.ui.helper :refer [plugin-fun path-for]]
   [reagent-modals.modals :as m]
   [salava.core.ui.modal :as mo]
   [salava.core.helper :refer [dump]]
   [salava.core.i18n :refer [t]]
   [clojure.string :refer [capitalize]]))


(defn ^:export user_gender_field []
  (g/gender-field))

(defn ^:export user_gender_field_register [state]
  (g/gender-field-registration state))

(defn ^:export field_input_valid [field value error-atom]
 #(h/valid-input? field value error-atom))


(defn ^:export org_field_register [state]
  (org/organization-field-registration state))

(defn ^:export org_field []
 (org/organization-field))

(defn init-custom-fields [state fields]
 (doall
  (for [f fields]
    (as-> (first (plugin-fun (session/get :plugins) f "init_field_value") ) $
          (when (ifn? $) ($ (cursor state [:custom-fields (keyword f)])))))))

(defn ^:export custom_fields_alert [state]
 (let [fields (filter :compulsory? (session/get :custom-fields))
       values (cursor state [:custom-fields])]
    (when (some nil? (vals @values))
      [:div.alert.alert-info.alert-dismissible {:role "alert"}
       [:button.close
        {:type "button"
         :data-dismiss "alert"
         :aria-label "Close"}
        [:span {:aria-hidden             "true"
                :dangerouslySetInnerHTML {:__html "&times;"}}]]
       [:p (t :extra-customField/Attentionrequired) " " [:strong [:a.alert-link {:href (path-for "/user/edit")} (t :user/Accountsettings)]]]
       (reduce-kv
        (fn [r k v]
          (when (nil? v)
            (conj r [:li (t (keyword (str "extra-customField/" (capitalize (name k)))))])))
        [:ul]
        @values)])))

(defn ^:export custom_fields_init [state]
 (let [fields (map :name (filter :compulsory? (session/get :custom-fields)))
       fmap (into {} (map #(hash-map (keyword %) nil) fields))]
  (when (seq fields)
    (reset! (cursor state [:custom-fields]) fmap)
    (init-custom-fields state fields))))
