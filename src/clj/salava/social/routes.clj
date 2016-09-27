(ns salava.social.routes
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [salava.core.layout :as layout]
            [schema.core :as s]
            [salava.core.access :as access]
            [salava.social.db :as so]
            salava.core.restructure))




(defn route-def [ctx]
  (routes
    (context "/social" []
             (layout/main ctx "/"))

    (context "/obpv1/social" []
             :tags ["social"]
             

             (GET "/messages/:badge_content_id" []
                  :return [{:id               s/Int
                            :user_id          s/Int
                            :badge_content_id s/Str 
                            :message          s/Str 
                            :ctime            s/Int
                            :first_name       s/Str
                            :last_name        s/Str
                            :profile_picture  (s/maybe s/Str)}]
                   :summary "Get all messages"
                   :path-params [badge_content_id :- s/Str]
                   :auth-rules access/admin
                   :current-user current-user
                   (do
                     (ok (so/get-badge-messages ctx badge_content_id (:id current-user))
                      )))

             (GET "/messages/:badge_content_id/:page_count" []
                  :return {:messages [{:id               s/Int
                                       :user_id          s/Int
                                       :badge_content_id s/Str 
                                       :message          s/Str 
                                       :ctime            s/Int
                                       :first_name       s/Str
                                       :last_name        s/Str
                                       :profile_picture  (s/maybe s/Str)}]
                           :messages_left s/Int}
                   :summary "Get 10 messages. Page_count tells OFFSET "
                   :path-params [badge_content_id :- s/Str
                                 page_count :- s/Int]
                   :auth-rules access/authenticated
                   :current-user current-user
                   (do
                     (ok (so/get-badge-messages-limit ctx badge_content_id page_count (:id current-user))
                      )))

             (POST "/messages/:badge_content_id" []
                   :return (s/enum "success" "error")
                   :summary "Create new message"
                   :path-params [badge_content_id :- s/Str]
                   :body [content {:message s/Str
                                   :user_id s/Int}]
                   :auth-rules access/authenticated
                   :current-user current-user
                   (let [{:keys [message user_id]} content]
                     (ok (so/message! ctx badge_content_id user_id message)
                      )))

             (POST "/delete_message/:message_id" []
                   :return (s/enum "success" "error")
                   :summary "Delete message"
                   :path-params [message_id :- s/Int]
                   :auth-rules access/authenticated
                   :current-user current-user
                   (ok (so/delete-message! ctx message_id (:id current-user)))
                   )
             
             )))
