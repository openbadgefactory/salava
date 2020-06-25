(ns salava.extra.customField.ui.block
  (:require
   [salava.extra.customField.ui.gender :as g]))

(defn ^:export user_gender_form []
  (g/gender-form))
