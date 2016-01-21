(ns salava.core.access
  (:require [buddy.auth :refer [authenticated?]]))

(defn authenticated [req]
  (println req)
  (authenticated? req))

(defn admin [req]
  (and (authenticated? req)
       (#{:admin} (:role (:identity req)))))