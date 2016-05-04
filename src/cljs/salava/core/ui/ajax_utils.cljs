(ns salava.core.ui.ajax-utils
  (:require [ajax.core :as ajax]
            [salava.core.ui.helper :refer [navigate-to]]
            [salava.core.i18n :refer [t]]))

(defn error-handler [additional-error-fn]
  {:error-handler (fn [{:keys [status status-text]}]
                    (if (= status 401)
                      (navigate-to "/user/login")
                      additional-error-fn))})

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

(defn DELETE
  ([url params]
   (DELETE url params (fn [])))
  ([url params error-fn]
   (ajax/DELETE url
              (merge params {:response-format :json :keywords? true} (error-handler error-fn)))))
