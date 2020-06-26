(ns salava.extra.customField.ui.block
  (:require
   [salava.extra.customField.ui.gender :as g]
   [salava.extra.customField.ui.helper :as h]))


(defn ^:export user_gender_form []
  (g/gender-form))

(defn ^:export user_gender_form_register [state]
  (g/gender-form-registration state))

(defn ^:export field_input_valid [field value error-atom]
 (prn value)
 #(h/valid-input? field value error-atom))
