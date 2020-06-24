(ns salava.mobile.schemas
  #? (:clj (:require [schema.core :as s]
                     [schema.coerce :as c]
                     [compojure.api.sweet :refer [describe]])
           :cljs (:require [schema.core :as s :include-macros true])))

#? (:cljs (defn describe [v _] v))

(defn either [s1 s2]
  #? (:clj (s/either s1 s2)
      :cljs (s/cond-pre s1 s2)))

(s/defschema user-badge-m {:id           (describe s/Int "internal user-badge id")
                           :badge_id     (describe s/Str "internal badge content id")
                           :name         (s/maybe s/Str)
                           :description  (s/maybe s/Str)
                           :image_file   (s/maybe s/Str)
                           :issued_on    (s/maybe s/Int)
                           :expires_on   (s/maybe s/Int)
                           :issuer_id    (s/maybe s/Str)
                           :issuer_name  (s/maybe s/Str)
                           :issuer_email (s/maybe s/Str)
                           :issuer_url   (s/maybe s/Str)
                           :issuer_image_file  (s/maybe s/Str)
                           :issuer_description (s/maybe s/Str)
                           :creator_id    (s/maybe s/Str)
                           :creator_name  (s/maybe s/Str)
                           :creator_email (s/maybe s/Str)
                           :creator_url   (s/maybe s/Str)
                           :creator_image_file  (s/maybe s/Str)
                           :creator_description (s/maybe s/Str)
                           :default_language_code (s/maybe s/Str)
                           :visibility   (describe (s/enum "private" "internal" "public") "internal user-badge visibility")
                           :status       (describe (s/enum "pending" "accepted" "declined") "internal user-badge acceptance status")
                           :revoked      s/Bool
                           :ctime        s/Int
                           :mtime        s/Int
                           :tags         (describe [s/Str] "internal tags added by current user")})


(s/defschema user-badges-m {:badges [user-badge-m]})

(s/defschema pending-badges-first-m {:id (s/maybe s/Int)})


(s/defschema alignment-m {:name        (s/maybe s/Str)
                          :description (s/maybe s/Str)
                          :url         (s/maybe s/Str)})

(s/defschema badge-content-m {:name             (s/maybe s/Str)
                              :language_code    (s/maybe s/Str)
                              :description      (s/maybe s/Str)
                              :criteria_content (s/maybe s/Str)
                              :criteria_url     (s/maybe s/Str)
                              :alignment        [alignment-m]})

(s/defschema user-badge-content-m (-> user-badge-m
                                      (assoc :content              [badge-content-m]
                                             :first_name           (describe (s/maybe s/Str) "badge earner's first name")
                                             :last_name            (describe (s/maybe s/Str) "badge earner's last name")
                                             :owner_id             (describe (s/maybe s/Int) "internal id of badge owner")
                                             :endorsement_count    (s/maybe s/Int)
                                             :evidence_count       (s/maybe s/Int)
                                             :congratulation_count (s/maybe s/Int)
                                             :assertion_url        (s/maybe s/Str)
                                             :assertion_jws        (s/maybe s/Str)
                                             :gallery_id           (s/maybe (describe s/Int "internal gallery badge id"))
                                             :show_recipient_name  (describe s/Bool "used internally; when set, earner's name is shown in badge ")
                                             :share_url            (s/maybe s/Str)
                                             )))

(s/defschema endorsement-m {:id           (s/maybe s/Str)
                            :content      (s/maybe s/Str)
                            :issuer_id    (s/maybe s/Str)
                            :issuer_name  (s/maybe s/Str)
                            :issuer_email (s/maybe s/Str)
                            :issuer_url   (s/maybe s/Str)
                            :issuer_image_file  (s/maybe s/Str)
                            :issuer_description (s/maybe s/Str)
                            :issued_on    (s/maybe s/Int)})


(s/defschema user-badge-endorsements-m {:badge  [endorsement-m]
                                        :issuer [endorsement-m]
                                        :user   [endorsement-m]})

(s/defschema congratulations-m {:congratulations [{:id    (s/maybe s/Int)
                                                   :first_name      (s/maybe s/Str)
                                                   :last_name       (s/maybe s/Str)
                                                   :profile_picture (s/maybe s/Str)
                                                   :ctime (s/maybe s/Int)}]})


(s/defschema gallery-badge-m {:id            (describe (s/maybe s/Int) "internal gallery id")
                              :badge_id      (describe (s/maybe s/Str) "internal badge content id")
                              :name          (s/maybe s/Str)
                              :description   (s/maybe s/Str)
                              :image_file    (s/maybe s/Str)
                              :issuer_id     (s/maybe s/Str)
                              :issuer_name   (s/maybe s/Str)
                              :issuer_email  (s/maybe s/Str)
                              :issuer_url    (s/maybe s/Str)
                              :issuer_image_file  (s/maybe s/Str)
                              :issuer_description (s/maybe s/Str)
                              :creator_id    (s/maybe s/Str)
                              :creator_name  (s/maybe s/Str)
                              :creator_email (s/maybe s/Str)
                              :creator_url   (s/maybe s/Str)
                              :creator_image_file    (s/maybe s/Str)
                              :creator_description   (s/maybe s/Str)
                              :recipient_count       (s/maybe s/Int)
                              :default_language_code (s/maybe s/Str)
                              :content [badge-content-m]})

