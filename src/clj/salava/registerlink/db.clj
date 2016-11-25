(ns salava.registerlink.db
  (:require [yesql.core :refer [defqueries]]
            [clojure.set :refer [rename-keys]]
            [clojure.java.jdbc :as jdbc]
            [salava.core.helper :refer [dump]]
            [slingshot.slingshot :refer :all]
            [salava.core.util :refer [get-db]]))

(defqueries "sql/admin/queries.sql")



(defn insert-config [ctx name value]
  (try+
   (insert-config<! {:name name :value value} (get-db ctx))
   (catch Object _
     "error")))

(defn get-config-value [ctx name]
  (select-name-value-config {:name name} (into {:result-set-fn first :row-fn :value} (get-db ctx))))

(defn get-register-token
  [ctx]
  (let [name "register-token"]
    (get-config-value ctx name)))


(defn get-register-active [ctx]
 (let [name "register-active"]
   (get-config-value ctx name)
   (if (= "1" (get-config-value ctx name))
     true
     false)))

(defn get-token-active [ctx]
  {:token  (get-register-token ctx)
   :active (get-register-active ctx)})

(defn right-token? [ctx temp-token]
  (let [{:keys [token active]} (get-token-active ctx)]
    (and active (= token temp-token))))

(defn create-register-token! [ctx token]
  (try+
   (insert-config ctx "register-token" token)
   "success"
   (catch Object _
     "error")))

(defn create-register-active! [ctx active]
  (try+
   (insert-config ctx "register-active" active)
   "success"
   (catch Object _
     "error")))
