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

(s/defschema Stats {:users {:Totalusersno (s/maybe s/Int)
                            :activatedusers (s/maybe s/Int)
                            :notactivatedusers (s/maybe s/Int)
                            :internalusers (s/maybe s/Int)
                            :publicusers (s/maybe s/Int)
                            :userssincelastlogin (s/maybe s/Int)
                            :userssincelastmonth (s/maybe s/Int)
                            :userssince3month (s/maybe s/Int)
                            :userssince6month (s/maybe s/Int)
                            :userssince1year (s/maybe s/Int)
                            :logincountsincelastlogin (s/maybe s/Int)
                            :1monthlogincount (s/maybe s/Int)
                            :3monthlogincount (s/maybe s/Int)
                            :6monthlogincount (s/maybe s/Int)
                            :1yearlogincount (s/maybe s/Int)}

                     :userbadges {:Totalbadgesno (s/maybe s/Int)
                                     :acceptedbadgescount (s/maybe s/Int)
                                      :pendingbadgescount (s/maybe s/Int)
                                      :declinedbadgescount (s/maybe s/Int)
                                      :privatebadgescount (s/maybe s/Int)
                                      :publicbadgescount (s/maybe s/Int)
                                      :internalbadgescount (s/maybe s/Int)
                                      :badgessincelastlogin (s/maybe s/Int)
                                      :badgessincelastmonth (s/maybe s/Int)
                                      :badgessince3month (s/maybe s/Int)
                                      :badgessince6month (s/maybe s/Int)
                                      :badgessince1year (s/maybe s/Int)
                                      :factorybadges (s/maybe s/Int)}

                    :issuers {:issuerssincelastlogin (s/maybe s/Int)
                              :issuerssincelastmonth (s/maybe s/Int)
                              :issuerssince3month (s/maybe s/Int)
                              :issuerssince6month (s/maybe s/Int)
                              :issuerssince1year (s/maybe s/Int)
                              :Totalissuersno (s/maybe s/Int)}

                     :pages {:pagessincelastlogin (s/maybe s/Int)
                             :pagessincelastmonth (s/maybe s/Int)
                             :pagessince3month (s/maybe s/Int)
                             :pagessince6month (s/maybe s/Int)
                             :pagessince1year (s/maybe s/Int)
                             :Totalpagesno (s/maybe s/Int)
                             :internalpagescount (s/maybe s/Int)
                             :privatepagescount (s/maybe s/Int)
                             :publicpagescount (s/maybe s/Int)}

                     :user-badge-correlation [{:badge_count (s/maybe s/Int) :user_count (s/maybe s/Int)}]
                     (s/optional-key :issued) {:Totalissuedno (s/maybe s/Int)
                                               :issuedsincelastmonth (s/maybe s/Int)
                                               :issuedsincelastlogin (s/maybe s/Int)}
                     (s/optional-key :created) {:Totalcreatedno (s/maybe s/Int)
                                                :createdsincelastlogin (s/maybe s/Int)
                                                :createdsincelastmonth (s/maybe s/Int)}})

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
