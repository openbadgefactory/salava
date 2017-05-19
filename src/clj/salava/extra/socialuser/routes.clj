(ns salava.extra.socialuser.routes
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [salava.extra.socialuser.schemas :as schemas]
            [salava.core.layout :as layout]
            [schema.core :as s]
            [salava.core.helper :refer [dump]]
            [salava.core.access :as access]
            [salava.extra.socialuser.db :as db]
            salava.core.restructure))




(defn route-def [ctx]
  (routes
    #_(context "/social" []
             (layout/main ctx "/")
             (layout/main ctx "/connections")
             (layout/main ctx "/stream"))

    (context "/obpv1/socialuser" []
             :tags ["socialuser"]
             
             (GET "/accepted-connections" []
                  :return [schemas/AcceptedUserConnections]
                  :summary "Returns user all accepted user connections"
                  :auth-rules access/authenticated
                  :current-user current-user
                  (do
                    (ok (db/get-user-following-connections-user ctx (:id current-user)))))
             
             (DELETE "/user-connection/:user_id" []
                     :path-params [user_id :- s/Int]
                     :return {:status (s/enum "success" "error")
                              (s/optional-key :message) (s/maybe s/Str)}
                     :summary "Delete connection"
                     :auth-rules access/authenticated
                     :current-user current-user
                     (ok (db/delete-connections-user ctx (:id current-user) user_id)
                      ))
             (GET "/user-connection/:user_id" []
                   :summary "get connection with user"
                   :return schemas/UserConnection
                   :path-params [user_id :- s/Int]
                   :auth-rules access/signed
                   :current-user current-user
                   (ok
                    (db/get-connections-user ctx (:id current-user) user_id)
                    ))
             

             (POST "/user-connection/:user_id" []
                   :summary "create connection with user"
                   :return {:status (s/enum "success" "error")
                            (s/optional-key :message) (s/maybe s/Str)}                  
                   :path-params [user_id :- s/Int]
                   :auth-rules access/authenticated
                   :current-user current-user
                   (ok
                    (db/create-connections-user! ctx (:id current-user) user_id)
                    ))

             (GET "/user-connection-config" []
                  :summary "returns user config accepting status"
                  :return (s/enum "accepted" "pending" "declined")
                  :auth-rules access/signed
                  :current-user current-user
                  (ok
                   (db/get-user-connections-accepting ctx (:id current-user))
                    
                   ))
             
             
             (POST "/user-connection-config/:status" []
                   :summary "change user config accepting" 
                   :return {:status (s/enum "success" "error")
                            (s/optional-key :message) (s/maybe s/Str)}                  
                   :path-params [status :- (s/enum "accepted" "pending" "declined")]
                   :auth-rules access/signed
                   :current-user current-user
                   (ok
                    (db/set-user-connections-accepting ctx (:id current-user) status)))
             
             (GET "/user-pending-requests" []
                  :summary "Get pending requests"
                  :return [schemas/PendingUsers]
                  :auth-rules access/signed
                  :current-user current-user
                  (ok
                   (db/get-pending-requests ctx (:id current-user))

                   ))

             (GET "/connections" []
                  :summary "Returns user all  user connections; pending and accepted"
                  :auth-rules access/signed
                  :current-user current-user
                  :return {:followers-users  [schemas/FollowersUsers]
                           :following-users [schemas/AcceptedUserConnections]}
                  (do
                    (let [followers-users  (db/get-user-followers-connections ctx (:id current-user))
                          following-users (db/get-user-following-connections-user ctx (:id current-user))]
                      
                      (ok {:followers-users followers-users
                           :following-users following-users}))))
             
             (POST "/user-pending-requests/:owner_id/:status" []
                   :summary "Change pending request to accept or declined" 
                   :return {:status (s/enum "success" "error")
                            (s/optional-key :message) (s/maybe s/Str)}                  
                   :path-params [owner_id :- s/Int
                                 status :- (s/enum "accepted" "declined")]
                   :auth-rules access/authenticated
                   :current-user current-user
                   (ok
                    (db/set-pending-request ctx owner_id (:id current-user) status)
                    )))))

