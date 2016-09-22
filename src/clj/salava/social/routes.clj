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
                   :summary "Get all tickets with open status"
                   :path-params [badge_content_id :- s/Str]
                   :auth-rules access/admin
                   :current-user current-user
                   (do
                     (ok (so/get-badge-messages ctx badge_content_id)
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
             
             )))
