(ns salava.badgeIssuer.ui.util
  (:require
    [reagent.core :refer [atom cursor create-class]]
    [reagent-modals.modals :as m]
    [reagent.session :as session]
    [salava.core.i18n :refer [t]]
    [salava.core.ui.ajax-utils :as ajax]
    [salava.core.ui.helper :refer [path-for]]
    [salava.core.ui.modal :as mo]))


(defn save-selfie-badge [state reload-fn]
  (let [badge-info @(cursor state [:badge])]
    (ajax/POST
      (path-for "/obpv1/selfie/create")
      {:params badge-info
       :handler (fn [data]
                  (when (= "success" (:status data))
                    (reset! (cursor state [:badge :id]) (:id data))))
       :finally (fn [] (when reload-fn (reload-fn)))})))

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
