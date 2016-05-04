(ns salava.core.restructure
  (:require [compojure.api.meta :refer [restructure-param]]
            [salava.core.session :refer [wrap-rule]]
            [salava.core.access :as access]))

(defmethod restructure-param :auth-rules
  [_ rule acc]
  (update-in acc [:middleware] conj [wrap-rule rule]))

(defmethod restructure-param :current-user
  [_ binding acc]
  (update-in acc [:letks] into [binding `(:identity ~'+compojure-api-request+)]))

(defmethod restructure-param :flash-message
  [_ binding acc]
  (update-in acc [:letks] into [binding `(:flash ~'+compojure-api-request+)]))
