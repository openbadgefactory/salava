(ns salava.admin.schemas
  (:require [schema.core :as s
             :include-macros true] ;; cljs only

            [salava.core.countries :refer [all-countries]]
            [salava.user.schemas :as u]))

#_(s/defschema Stats {:register-users (s/maybe s/Int)
                      :last-month-active-users (s/maybe s/Int)
                      :last-month-registered-users (s/maybe s/Int)
                      :all-badges (s/maybe s/Int)
                      :last-month-added-badges (s/maybe s/Int)
                      :pages (s/maybe s/Int)})

(s/defschema Stats {:users {:total (s/maybe s/Int)
                            :not-activated (s/maybe s/Int)
                            :activated (s/maybe s/Int)
                            :since-last-login (s/maybe s/Int)
                            :since-last-month (s/maybe s/Int)
                            :since-3-month (s/maybe s/Int)
                            :since-6-month (s/maybe s/Int)
                            :since-1-year (s/maybe s/Int)
                            :last-month-login-count (s/maybe s/Int)
                            :3-month-login-count (s/maybe s/Int)
                            :6-month-login-count (s/maybe s/Int)
                            :1-year-login-count (s/maybe s/Int)
                            :internal (s/maybe s/Int)
                            :public (s/maybe s/Int)}
                    ;:last-month-active-users (s/maybe s/Int)
                    ;:last-month-registered-users (s/maybe s/Int)
                    :badges {:total (s/maybe s/Int)
                             :accepted (s/maybe s/Int)
                             :pending (s/maybe s/Int)
                             :declined (s/maybe s/Int)
                             :private (s/maybe s/Int)
                             :public (s/maybe s/Int)
                             :internal (s/maybe s/Int)
                             :since-last-login (s/maybe s/Int)
                             :since-last-month (s/maybe s/Int)
                             :since-3-month (s/maybe s/Int)
                             :since-6-month (s/maybe s/Int)
                             :since-1-year (s/maybe s/Int)}

                    ;:last-month-added-badges (s/maybe s/Int)
                    :pages {:since-last-login (s/maybe s/Int)
                            :since-last-month (s/maybe s/Int)
                            :since-3-month (s/maybe s/Int)
                            :since-6-month (s/maybe s/Int)
                            :since-1-year (s/maybe s/Int)
                            :total (s/maybe s/Int)
                            :internal (s/maybe s/Int)
                            :private (s/maybe s/Int)
                            :public (s/maybe s/Int)}})

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

                          :role        (s/enum "user" "admin")
                          :ctime s/Int
                          :last_login (s/maybe s/Int)
                          :deleted s/Bool
                          :activated  s/Bool
                          :has_password? s/Int
                          :service [(s/maybe s/Str)]}})

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
                            :creator_image       (s/maybe s/Str)}})


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
                     :status (s/enum "open" "closed")
                     :last_name s/Str})

(s/defschema Closed_ticket (-> {:mtime s/Int}
                               (merge Ticket)))

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
