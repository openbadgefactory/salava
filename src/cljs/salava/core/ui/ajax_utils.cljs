(ns salava.core.ui.ajax-utils
  (:require [ajax.core :as ajax]
            [reagent.session :as session]
            [salava.core.ui.helper :refer [navigate-to current-route-path]]
            [salava.core.helper :refer [dump]]
            [salava.core.i18n :refer [t]]))

(defn error-handler [additional-error-fn]
  {:error-handler (fn [{:keys [status status-text]}]
                    (do
                      (if (and (not (session/get :user)) (= status 401))
                       (do
                         ;(session/put! :referrer (.-location.pathname js/window))
                         (session/put! :referrer (current-route-path))
                        (navigate-to "/user/login"))
                        (additional-error-fn))
                      ))})

(defn loading-message []
  [:div.ajax-message
   [:i {:class "fa fa-cog fa-spin fa-2x "}]
   [:span (str (t :core/Loading) "...")]])

(defn GET
  ([url params]
    (GET url params (fn [])))
  ([url params error-fn]
   (ajax/GET url
             (merge params {:response-format :json :keywords? true} (error-handler error-fn)))))

(defn POST
  ([url params]
   (POST url params (fn [])))
  ([url params error-fn]
   (ajax/POST url
             (merge params {:response-format :json :keywords? true} (error-handler error-fn)))))

(defn PUT
  ([url params]
   (PUT url params (fn [])))
  ([url params error-fn]
   (ajax/PUT url
             (merge params {:response-format :json :keywords? true} (error-handler error-fn)))))

(defn DELETE
  ([url params]
   (DELETE url params (fn [])))
  ([url params error-fn]
   (ajax/DELETE url
              (merge params {:response-format :json :keywords? true} (error-handler error-fn)))))
