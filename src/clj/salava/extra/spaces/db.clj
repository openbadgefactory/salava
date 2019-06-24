(ns salava.extra.spaces.db
  (:require [yesql.core :refer [defqueries]]
            [salava.core.util :as u]
            [clojure.java.jdbc :as jdbc]
            [salava.extra.spaces.util :refer [save-image!]]
            [clojure.tools.logging :as log]
            [slingshot.slingshot :refer :all]))

(defqueries "sql/extra/spaces/main.sql")
(defrecord Space_member [id user_id space_id role default_space])
(defrecord Pending_admin [id space_id email])

(defn create-space-admin! [ctx space-id email]
  (log/info "Creating space admin " email)
  (if-let [user-id (select-email-address {:email email} (into {:result-set-fn first :key-fn :user_id} (u/get-db ctx)))]
    (create-space-member! (->Space_member nil user-id space-id "admin" 0) (u/get-db ctx))
    (create-pending-space-admin! (->Pending_admin nil space-id email) (u/get-db ctx)))
  (log/info "Space admin " email " created!"))

(defn space-exists?
 "check if space name already exists"
 [ctx space]
 (if-let [check (empty? (select-space-by-name {:name (:name space)} (u/get-db ctx)))] false true))


(defn create-new-space! [ctx space]
  (log/info "Creating space" (:name space))
  (try+
   (if (space-exists? ctx space)
    (if (seq (:admin space))
      (jdbc/with-db-transaction [tx (:connection (u/get-db ctx))]
                                (let [space_id (-> space
                                                   (dissoc :id :admin)
                                                   (assoc :logo (save-image! ctx (:logo space)) :banner (save-image! ctx (:banner space)))
                                                   (create-space<! {:connection tx})
                                                   :generated_key)]

                                  (do (doseq [email (:admin space)]
                                        (create-space-admin! ctx space_id email))
                                      (log/info "Finished creating space")
                                      {:status "success"})))
      (do
        (log/error "Error, No space admin defined")
        (throw+ {:status "Error" :message (str "Space with name " (:name space) " already exists")})
        {:status "Error"}))
    (do
     (log/error "Error!" (str "Space with name " (:name space) " already exists"))
     (throw+ {:status "Error" :message (str "Space with name " (:name space) " already exists")})
     {:status "Error"}))


   (catch Object _
     (log/error "Create space error!")
     (log/error _)
     {:status "error"})))
