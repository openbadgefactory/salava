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
                  "organization" schemas/organization*)
                 (case field
                  "gender" schemas/gender
                  "organization" schemas/organization
                  nil))]
    (when (and (not (input-valid? schema value)) error-atom)
      (reset! error-atom (t (keyword (str "extra-customField/" field "fielderror")))))
    (input-valid? schema value)))
