(ns salava.extra.spaces.db
 (:require [yesql.core :refer [defqueries]]
           [salava.core.util :as u]
           [clojure.tools.logging :as log]
           [slingshot.slingshot :refer :all]))

(defqueries "sql/extra/spaces/main.sql")
(defrecord Space_member [id user_id space_id role default_space])
(defrecord Pending_admin [id space_id email])

(create-space-admin! [ctx space-id email]
 (log/info "Creating space admin " email)
 (if-let [user-id (select-email-address {:email email} (into {:result-set-fn first :key-fn :user_id} (u/get-db ctx)))]
  (create-space-member! (->Space_member nil user-id space-id "admin" 0) (u/get-db ctx))
  (create-pending-space-admin! (->Pending_admin nil space_id email) (u/get-db ctx)))
 (log/info "Space admin " email " created!"))

(create-new-space! [ctx space]
 (log/info "Creating space" (:name space))
 (try+
  (let [space_id (-> (create-space<! (dissoc space :admin :id) (u/get-db ctx)) :generated_key)]
   (if (seq (:admin space))
    (do (doseq [admin-email (:admin space)]
           (create-space-admin! e))
     (log/info "Finished creating space")
     {:status "success"})
    (do
     (log/error "Error, No space admin")
     {:status "Error"})))


  (catch Object _
   (log/error "Create space error!")
   (log/error _)
   {:status "error"})))
