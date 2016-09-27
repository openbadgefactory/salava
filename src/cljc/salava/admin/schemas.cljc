(ns salava.admin.schemas
  (:require [schema.core :as s
             :include-macros true ;; cljs only
             ]
            [salava.core.countries :refer [all-countries]]
            [salava.user.schemas :as u]))

(s/defschema Stats {:register-users (s/maybe s/Int)
                    :last-month-active-users (s/maybe s/Int)
                    :last-month-registered-users (s/maybe s/Int)
                    :all-badges (s/maybe s/Int)
                    :last-month-added-badges (s/maybe s/Int)
                    :pages (s/maybe s/Int)})

(s/defschema User-name-and-email {:name s/Str
                                  :email s/Str})

(s/defschema User {:name s/Str
                   :image_file (s/maybe s/Str)
                   :item_owner_id (s/maybe s/Int)
                   :item_owner s/Str
                   :info {:emails [{:email            s/Str
                                    :verified         s/Bool
                                    :primary_address  s/Bool
                                    :backpack_id      (s/maybe s/Int)
                                    :ctime            s/Int
                                    :mtime            s/Int}]
                          :ctime s/Int
                          :last_login (s/maybe s/Int)
                          :deleted s/Bool}})

(s/defschema Page {:name s/Str
                   :image_file (s/maybe s/Str)
                   :item_owner_id (s/maybe s/Int)
                   :item_owner s/Str
                   :info {:emails [{:email            s/Str
                                    :verified         s/Bool
                                    :primary_address  s/Bool
                                    :backpack_id      (s/maybe s/Int)
                                    :ctime            s/Int
                                    :mtime            s/Int}]}})


(s/defschema Badge {:name s/Str
                    :image_file (s/maybe s/Str)
                    :item_owner_id (s/maybe s/Int)
                    :item_owner s/Str
                    :info {:issuer_content_name (s/maybe s/Str)
                           :issuer_content_url  (s/maybe s/Str)
                           :issuer_contact      (s/maybe s/Str)
                           :issuer_image        (s/maybe s/Str)
                           :creator_name        (s/maybe s/Str)
                           :creator_url         (s/maybe s/Str)
                           :creator_email       (s/maybe s/Str)
                           :creator_image       (s/maybe s/Str)
                           :emails [{:email            s/Str
                                    :verified         s/Bool
                                    :primary_address  s/Bool
                                    :backpack_id      (s/maybe s/Int)
                                    :ctime            s/Int
                                    :mtime            s/Int}]}})

(s/defschema Badges {:name s/Str
                     :image_file (s/maybe s/Str)
                     :item_owner_id [(s/maybe s/Int)]
                     :item_owner [s/Str]
                     :info {:issuer_content_name (s/maybe s/Str)
                            :issuer_content_url  (s/maybe s/Str)
                            :issuer_contact      (s/maybe s/Str)
                            :issuer_image        (s/maybe s/Str)
                            :creator_name        (s/maybe s/Str)
                            :creator_url         (s/maybe s/Str)
                            :creator_email       (s/maybe s/Str)
                            :creator_image       (s/maybe s/Str)
                            }})

(s/defschema Report {:description (s/maybe s/Str)
                     :report_type (s/enum "inappropriate" "bug" "mistranslation" "other" "fakebadge")
                     :item_content_id  (s/maybe s/Str)
                     :item_id  (s/maybe s/Int)
                     :item_url (s/maybe s/Str)
                     :item_name (s/maybe s/Str)
                     :item_type (s/enum "badge" "page" "user" "badges")
                     :reporter_id (s/maybe s/Int)})

(s/defschema Ticket {:id s/Int
                     :description (s/maybe s/Str)
                     :report_type (s/enum "inappropriate" "bug" "mistranslation" "other" "fakebadge")
                     :item_id  (s/maybe s/Int)
                     :item_content_id  (s/maybe s/Str)
                     :item_url (s/maybe s/Str)
                     :item_name (s/maybe s/Str)
                     :item_type (s/enum "badge" "page" "user" "badges")
                     :reporter_id (s/maybe s/Int)
                     :ctime s/Int
                     :first_name s/Str
                     :last_name s/Str})

(s/defschema Url-parser {:item-type (s/enum "badge" "page" "user")
                         :item-id   (s/maybe s/Int)})



(s/defschema UserSearch {:name          (s/constrained s/Str #(and (>= (count %) 0)
                                                                   (<= (count %) 255)))
                         :country       (apply s/enum (conj (keys all-countries) "all"))
                         
                         :order_by      (s/enum "name" "ctime" "common_badge_count")
                         :filter   (s/enum  "all" "deleted")})

(s/defschema UserProfiles {:first_name (s/constrained s/Str #(and (>= (count %) 1)
                                                                  (<= (count %) 255)))
                           :last_name  (s/constrained s/Str #(and (>= (count %) 1)
                                                          (<= (count %) 255)))
                           :country    (apply s/enum (keys all-countries))
                              
                           :ctime s/Int
                           :id s/Int
                           :deleted (s/enum true false)
                           :email (s/maybe s/Str)})

(s/defschema Countries (s/constrained [s/Str] (fn [c]
                                                (and
                                                  (some #(= (first c) %) (keys all-countries))
                                                  (some #(= (second c) %) (vals all-countries))))))
