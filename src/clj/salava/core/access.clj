(ns salava.core.access
  (:require [buddy.auth :refer [authenticated?]]))

(defn authenticated [req]
  (and
   (authenticated? req)
   (get-in req [:identity :activated])))

(defn signed [req]
  (authenticated? req))

(defn admin [req]
  (and
   (authenticated? req)
   (= "admin" (get-in req [:identity :role]))))
