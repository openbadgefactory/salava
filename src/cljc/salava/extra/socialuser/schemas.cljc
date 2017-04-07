(ns salava.extra.socialuser.schemas
  (:require [schema.core :as s
             :include-macros true ;; cljs only
             ]
            [salava.core.schema-helper :as h]))



(s/defschema AcceptedUserConnections {:first_name      s/Str
                                      :last_name       s/Str
                                      :profile_picture (s/maybe s/Str)
                                      :status          (s/enum "accepted" "pending" "declined")
                                      :user_id         s/Int})


(s/defschema UserConnection  {:owner_id s/Int,
                              :user_id  s/Int,
                              :status   (s/maybe s/Str),
                              :ctime    (s/maybe s/Int)})

(s/defschema PendingUsers {:owner_id        s/Int
                           :user_id         s/Int
                           :first_name      s/Str
                           :last_name       s/Str
                           :profile_picture (s/maybe s/Str)})


