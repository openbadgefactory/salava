(ns salava.social.routes
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [salava.core.layout :as layout]
            [schema.core :as s]
            [salava.core.access :as access]
            [salava.social.db :as so]
            [salava.user.db :as u]
            [salava.factory.db :as f]
            [salava.badge.main :as b]
            [salava.social.schemas :as schemas]
            salava.core.restructure))

(defn route-def [ctx]
  (routes
    (context "/social" []
             (layout/main ctx "/")
             (layout/main ctx "/connections")
             (layout/main ctx "/stream")
             (layout/main ctx "/stats"))

    (context "/obpv1/social" []
             :tags ["social"]
          (GET "/messages/:badge_id/:page_count" []
               :return schemas/social-messages
               :summary "Get 10 messages. Page_count tells OFFSET "
               :path-params [badge_id :- s/Str
                             page_count :- s/Int]
               :auth-rules access/signed
               :current-user current-user
               (do
                 (ok (so/get-badge-messages-limit ctx badge_id page_count (:id current-user)))))

          (GET "/messages_count/:badge_id" []
               :return schemas/message-count
               :summary "Returns count of not viewed messages and all messages"
               :path-params [badge_id :- s/Str]
               :auth-rules access/signed
               :current-user current-user
               (ok (so/get-badge-message-count ctx badge_id (:id current-user))))

          (GET "/connected/:badge_id" []
              :return s/Bool
              :summary "Returns Bool if user has connected with asked badge-id"
              :path-params [badge_id :- s/Str]
              :auth-rules access/signed
              :current-user current-user
              (ok (so/is-connected? ctx (:id current-user) badge_id)))

          (GET "/pending_badges" []
               :return schemas/pending-badges
               :summary "Check and return user's pending badges"
               :auth-rules access/signed
               :current-user current-user
               (f/save-pending-assertions ctx (:id current-user))
               (ok {:pending-badges (b/user-badges-pending ctx (:id current-user))}))

          (GET "/events" []
               :summary "Returns users events"
               :auth-rules access/signed
               :current-user current-user
               (ok (let [events (so/get-all-events-add-viewed ctx (:id current-user))
                         tips (so/get-user-tips ctx (:id current-user))
                         accepted-terms? (u/get-accepted-terms-by-id ctx (:id current-user))
                         events {:tips tips
                                 :events events
                                 :terms-accepted (:status accepted-terms?)}]
                     events)))

          (GET "/issuer_connected/:issuer_content_id" []
              :return s/Bool
              :summary "check issuer connection status"
              :path-params [issuer_content_id :- s/Str]
              :auth-rules access/authenticated
              :current-user current-user
              (ok (so/issuer-connected? ctx (:id current-user) issuer_content_id)))


          (POST "/messages/:badge_id" []
                :return {:status (s/enum "success" "error") :connected? (s/maybe  s/Str)}
                :summary "Create new message"
                :path-params [badge_id :- s/Str]
                :body [content schemas/new-message #_{:message s/Str
                                                      :user_id s/Int}]
                :auth-rules access/authenticated
                :current-user current-user
                (let [{:keys [message user_id]} content]
                  (ok (so/message! ctx badge_id user_id message))))

          (POST "/delete_message/:message_id" []
                :return (s/enum "success" "error")
                :summary "Delete message"
                :path-params [message_id :- s/Int]
                :auth-rules access/authenticated
                :current-user current-user
                (ok (so/delete-message! ctx message_id (:id current-user))))

          (POST "/create_connection_badge/:badge_id" []
                :return {:status (s/enum "success" "error") :connected? s/Bool}
                :summary "Create badge connection; follow badge"
                :path-params [badge_id :- s/Str]
                :auth-rules access/authenticated
                :current-user current-user
                (ok (so/create-connection-badge! ctx (:id current-user) badge_id)))

          (POST "/delete_connection_badge/:badge_id" []
                :return {:status (s/enum "success" "error") :connected? s/Bool}
                :summary "Delete badge connection"
                :path-params [badge_id :- s/Str]
                :auth-rules access/authenticated
                :current-user current-user
                (ok (so/delete-connection-badge! ctx (:id current-user) badge_id)))

          (POST "/hide_event/:event_id" []
                :return (s/enum "success" "error")
                :summary "Hide user event"
                :path-params [event_id :- s/Int]
                :auth-rules access/authenticated
                :current-user current-user
                (ok (so/hide-user-event! ctx (:id current-user) event_id)))

          (POST "/events/hide_all" []
                :return {:status (s/enum "success" "error")}
                :summary "Hide all user events"
                :auth-rules access/signed
                :current-user current-user
                (ok (so/hide-all-user-events! ctx (:id current-user))))

          #_(GET "/connections_badge" []
                 :summary "Return users all badge connections"
                 :auth-rules access/signed
                 :current-user current-user
                 (do
                   (ok (so/get-connections-badge ctx (:id current-user)))))

            (POST "/create_connection_issuer/:issuer_content_id" []
                  :return {:status (s/enum "success" "error")}
                  :summary "add issuer to favorites"
                  :path-params [issuer_content_id :- s/Str]
                  :auth-rules access/authenticated
                  :current-user current-user
                  (ok (so/create-connection-issuer! ctx (:id current-user) issuer_content_id)))

            (POST "/delete_connection_issuer/:issuer_content_id" []
                  :return {:status (s/enum "success" "error")}
                  :summary "remove issuer from favourites"
                  :path-params [issuer_content_id :- s/Str]
                  :auth-rules access/authenticated
                  :current-user current-user
                  (ok (so/delete-issuer-connection! ctx (:id current-user) issuer_content_id)))

            #_(GET "/connections_issuer" []
                   :summary "Return all user issuer connection"
                   :auth-rules access/authenticated
                   :current-user current-user
                   (ok (so/get-user-issuer-connections ctx (:id current-user)))))))
