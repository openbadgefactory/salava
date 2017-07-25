(ns salava.social.routes
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [salava.core.layout :as layout]
            [schema.core :as s]
            [salava.core.access :as access]
            [salava.social.db :as so]
            [salava.factory.db :as f]
            [salava.badge.main :as b]
            salava.core.restructure))




(defn route-def [ctx]
  (routes
    (context "/social" []
             (layout/main ctx "/")
             (layout/main ctx "/connections")
             (layout/main ctx "/stream"))

    (context "/obpv1/social" []
             :tags ["social"]
             (GET "/messages/:badge_id/:page_count" []
                  ;:return
                  #_{:messages      [{:id              s/Int
                                    :user_id         s/Int
                                    :badge_id        s/Str 
                                    :message         s/Str 
                                    :ctime           s/Int
                                    :first_name      s/Str
                                    :last_name       s/Str
                                    :profile_picture (s/maybe s/Str)}]
                   :messages_left s/Int}
                  :summary "Get 10 messages. Page_count tells OFFSET "
                  :path-params [badge_id :- s/Str
                                page_count :- s/Int]
                  :auth-rules access/signed
                  :current-user current-user
                  (do
                    (ok (so/get-badge-messages-limit ctx badge_id page_count (:id current-user))
                        )))

             (GET "/messages_count/:badge_id" []
                  :return {:new-messages s/Int
                           :all-messages s/Int}
                   :summary "Returns count of not viewed messages and all messages"
                   :path-params [badge_id :- s/Str]
                   :auth-rules access/signed
                   :current-user current-user
                   (do
                     (ok (so/get-badge-message-count ctx badge_id (:id current-user))
                      )))

             (POST "/messages/:badge_id" []
                   :return {:status (s/enum "success" "error") :connected? (s/maybe  s/Str)}
                   :summary "Create new message"
                   :path-params [badge_id :- s/Str]
                   :body [content {:message s/Str
                                   :user_id s/Int}]
                   :auth-rules access/authenticated
                   :current-user current-user
                   (let [{:keys [message user_id]} content]
                     (ok (so/message! ctx badge_id user_id message)
                      )))

             (POST "/delete_message/:message_id" []
                   :return (s/enum "success" "error")
                   :summary "Delete message"
                   :path-params [message_id :- s/Int]
                   :auth-rules access/authenticated
                   :current-user current-user
                   (ok (so/delete-message! ctx message_id (:id current-user)))
                   )

             (POST "/delete_connection_badge/:badge_id" []
                   :return {:status (s/enum "success" "error") :connected? s/Bool}
                   :summary "Delete message"
                   :path-params [badge_id :- s/Str]
                   :auth-rules access/authenticated
                   :current-user current-user
                   (ok (so/delete-connection-badge! ctx (:id current-user) badge_id))
                   )

             (POST "/hide_event/:event_id" []
                   :return (s/enum "success" "error")
                   :summary "Delete message"
                   :path-params [event_id :- s/Int]
                   :auth-rules access/authenticated
                   :current-user current-user
                   (ok (so/hide-user-event! ctx (:id current-user) event_id))
                   )
             
             (POST "/create_connection_badge/:badge_id" []
                   :return {:status (s/enum "success" "error") :connected? s/Bool}
                   :summary "Delete message"
                   :path-params [badge_id :- s/Str]
                   :auth-rules access/authenticated
                   :current-user current-user
                   (ok (so/create-connection-badge! ctx (:id current-user) badge_id))
                   )


             (GET "/connections_badge" []
                   :summary "Return users all badge connections"
                   :auth-rules access/signed
                   :current-user current-user
                   (do
                     (ok (so/get-connections-badge ctx (:id current-user)))))

             (GET "/events" []
                   :summary "Returns users events"
                   :auth-rules access/signed
                   :current-user current-user
                   (do
                     (f/save-pending-assertions ctx (:id current-user))
                     (ok (let [pending-badges (b/user-badges-pending ctx (:id current-user))
                               events (so/get-all-events-add-viewed ctx (:id current-user))
                               tips (so/get-user-tips ctx (:id current-user))
                               ;admin-events (if (= "admin" (:role current-user)) (so/get-user-admin-events-sorted ctx (:id current-user)) [])
                               events {:tips tips
                                       :pending-badges pending-badges
                                       :events events}
                               ;events (if (and (not (empty? admin-events)) (= "admin" (:role current-user))) (merge events {:admin-events admin-events}) events)
                               ]

                           events))))
             
             (GET "/connected/:badge_id" []
                  :return s/Bool
                   :summary "Returns Bool if user has connected with asked badge-id"
                   :path-params [badge_id :- s/Str]
                   :auth-rules access/signed
                   :current-user current-user
                   (do
                     (ok (so/is-connected? ctx (:id current-user) badge_id)
                      )))
             
             )))

