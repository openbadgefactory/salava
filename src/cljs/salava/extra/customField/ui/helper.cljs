(ns salava.extra.customField.ui.helper
 (:require
  [reagent.core :refer [atom]]
  [reagent.session :as session]
  [salava.core.ui.helper :refer [input-valid?]]
  [salava.extra.customField.schemas :as schemas]
  [salava.core.i18n :refer [t]]))


(defn field-enabled? [field]
 (let [fields (map :name (session/get :custom-fields nil))]
   (some #(= field %) fields)))

(defn compulsory-field? [field]
 (let [fields (session/get :custom-fields nil)]
  (true? (:compulsory? (first (filter #(= field (:name %)) fields))))))

(defn valid-input? [field value error-atom]
  (let [schema (if (compulsory-field? field)
                 (case field
                  "gender" schemas/gender*
                  nil)
                 (case field
                  "gender" schemas/gender
                  nil))]
     (prn schema "fdfd" field (compulsory-field? field) value)
    (when-not (input-valid? schema value)
      (reset! error-atom (t (keyword (str "extra-customField/" field "fielderror"))))
      false)))
