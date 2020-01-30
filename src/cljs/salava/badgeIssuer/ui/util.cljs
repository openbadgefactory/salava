(ns salava.badgeIssuer.ui.util
  (:require
    [reagent.core :refer [atom cursor create-class]]
    [reagent-modals.modals :as m]
    [reagent.session :as session]
    [salava.badgeIssuer.schemas :as schemas]
    [salava.core.i18n :refer [t]]
    [salava.core.ui.ajax-utils :as ajax]
    [salava.core.ui.helper :refer [path-for input-valid? navigate-to]]
    [salava.core.ui.modal :as mo]))

(defn validate-inputs [s b]
  (doall
    [(input-valid? (:name s) (:name b))
     (input-valid? (:description s) (:description b))
     (input-valid? (:criteria s) (:criteria b))
     (input-valid? (:image s) (:image b))
     (input-valid? (:issuable_from_gallery s) (:issuable_from_gallery b))]))

(defn save-selfie-badge [state reload-fn]
  (reset! (cursor state [:error-message]) nil)
  (let [badge-info (-> @(cursor state [:badge])
                       (select-keys [:id :name :criteria :description :image :issuable_from_gallery]))
                       ;(assoc :issuable_from_gallery (if (:issuable_from_gallery @state) 1 0)))
        validate-info (validate-inputs schemas/save-selfie-badge badge-info)]
    (if (some false? validate-info)
      (reset! (cursor state [:error-message])
        (case (.indexOf validate-info false)
          0 (t :badgeIssuer/Namefieldempty)
          1 (t :badgeIssuer/Descriptionfieldempty)
          2 (t :badgeIssuer/Criteriafieldempty)
          (t :badgeIssuer/Errormessage)))

      (ajax/POST
        (path-for "/obpv1/selfie/create")
        {:params badge-info
         :handler (fn [data]
                    (when (= "success" (:status data))
                      (reset! (cursor state [:badge :id]) (:id data))))
         :finally (fn [] (when reload-fn (reload-fn)))}))))

(defn delete-selfie-badge [state]
  (let [id @(cursor state [:badge :id])]
    (ajax/DELETE
      (path-for (str "/obpv1/selfie/" id))
      {:handler (fn [])})))

(defn generate-image [state]
  (reset! (cursor state [:generating-image]) true)
  (ajax/GET
    (path-for "/obpv1/selfie/generate_image")
    {:handler (fn [{:keys [status url message]}]
                (when (= "success" status)
                  (reset! (cursor state [:badge :image]) url)
                  (reset! (cursor state [:generating-image]) false)))}))

(defn toggle-setting [setting]
  (if (pos? @setting) 
    (reset! setting 0)
    (reset! setting 1)))
