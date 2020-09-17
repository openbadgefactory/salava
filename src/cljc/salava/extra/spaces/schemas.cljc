(ns salava.extra.spaces.schemas
  #? (:clj (:require
            [schema.core :as s]
            [clojure.string :refer [blank?]])
      :cljs (:require
             [schema.core :as s :include-macros true]
             [clojure.string :refer [blank?]])))

(s/defschema space-properties {:css (s/maybe {:p-color (s/maybe s/Str)
                                              (s/optional-key :s-color) (s/maybe s/Str)
                                              (s/optional-key :t-color) (s/maybe s/Str)})})

(s/defschema space {:uuid s/Str
                    :id s/Int
                    :name (s/conditional #(not (blank? %)) s/Str)
                    :alias (s/conditional #(not (blank? %)) s/Str)
                    :description (s/conditional #(not (blank? %)) s/Str)
                    :url (s/maybe (s/conditional #(not (blank? %)) s/Str))
                    :logo (s/conditional #(not (blank? %)) s/Str)
                    (s/optional-key :banner) (s/maybe s/Str)
                    :visibility (s/enum "open" "controlled" "private")
                    :status (s/enum "active" "suspended" "expired" "deleted")
                    (s/optional-key :css) (:css space-properties)
                    (s/optional-key  :valid_until) (s/maybe s/Int)
                    :ctime s/Int
                    :mtime s/Int
                    (s/optional-key :member_count) s/Int
                    (s/optional-key :last_modified_by) (s/maybe s/Int)
                    (s/optional-key :messages) (s/maybe {(s/optional-key :messages_enabled) s/Bool
                                                         (s/optional-key :all_issuers_enabled) s/Bool
                                                         (s/optional-key :enabled_issuers) [(s/maybe s/Str)]})})



(s/defschema space-member {:id s/Int
                           :default_space s/Bool
                           :first_name s/Str
                           :last_name s/Str
                           :profile_picture (s/maybe s/Str)
                           :space_id s/Int})

(s/defschema memberlist [(s/maybe (assoc space-member  :role (s/enum "admin" "member")
                                                       :mtime s/Int
                                                       :status (s/enum "accepted" "pending")
                                                       (s/optional-key "organization") (s/maybe s/Str)
                                                       (s/optional-key "gender") (s/maybe s/Str)))])

(s/defschema space-member? {:user_id s/Int
                            :status (s/enum "accepted" "pending")
                            :role (s/enum "admin" "member")})

(s/defschema space-info (-> space
                           (dissoc :admins)
                           (assoc :admins [(s/maybe space-member)] :message-tool-enabled s/Bool)))

(s/defschema spaces [(s/maybe space)])

(s/defschema create-space (-> space
                              (dissoc :uuid :ctime :mtime :status :visibility :id)
                              (assoc :admins [s/Int])))

(s/defschema edit-space (-> create-space
                            (dissoc :admins)
                            (assoc :id s/Int
                                   (s/optional-key :messages) (s/maybe {(s/optional-key :messages_enabled) s/Bool
                                                                        (s/optional-key :enabled_issuers) [(s/maybe s/Str)]
                                                                        (s/optional-key :all_issuers_enabled) s/Bool}))))
(def report_user {:id s/Int
                  :activated s/Bool
                  :badgecount s/Int
                  :sharedbadges s/Int
                  :name s/Str
                  :completionPercentage s/Int
                  :emailaddresses s/Str
                  :profile_picture (s/maybe s/Str)
                  :profile_visibility (s/enum "internal" "public")
                  :ctime s/Int
                  (s/optional-key :organization) (s/maybe s/Str)
                  (s/optional-key :gender) (s/maybe s/Str)})

(def report_badges {:id s/Int
                    :badge_image (s/maybe s/Str)
                    :badge_name s/Str
                    :deleted s/Int
                    :visibility (s/enum "private" "public" "internal")
                    :status (s/enum "accepted" "pending" "declined")
                    :expires_on (s/maybe s/Int)
                    :issued_on s/Int})

(s/defschema report {:total s/Int :user_count s/Int :users (s/maybe [(s/maybe report_user)])})

(s/defschema badges-for-report [(s/maybe {:user_id s/Int :badges (s/maybe [(s/maybe report_badges)])})])

(s/defschema message-tool-badges {:badge_count s/Int :badges [(s/maybe (-> (select-keys report_badges [:id :badge_name :badge_image])
                                                                           (assoc :issuer_name s/Str :badge_id s/Str)))]})

(s/defschema gallery-spaces {:space_count s/Int :spaces [(s/maybe (-> (select-keys space [:id :logo :ctime :mtime :visibility :name])
                                                                      (assoc :member_count s/Int)))]})

(s/defschema user-spaces [(s/maybe (-> (select-keys space [:id :name :logo :ctime])
                                       (merge (select-keys space-member [:default_space :space_id]))
                                       (merge space-member?)
                                       (merge space-properties)))])

(s/defschema stats {:users {:Totalusersno (s/maybe s/Int)
                            :activatedusers (s/maybe s/Int)
                            :notactivatedusers (s/maybe s/Int)
                            :internalusers (s/maybe s/Int)
                            :publicusers (s/maybe s/Int)
                            :userssincelastlogin (s/maybe s/Int)
                            :userssincelastmonth (s/maybe s/Int)
                            :userssince3month (s/maybe s/Int)
                            :userssince6month (s/maybe s/Int)
                            :userssince1year (s/maybe s/Int)}

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
                                                :createdsincelastmonth (s/maybe s/Int)}
                     (s/optional-key :spaces) (s/maybe {:spacessincelastlogin (s/maybe s/Int)
                                                        :spacessincelastmonth (s/maybe s/Int)
                                                        :spacessince3month (s/maybe s/Int)
                                                        :spacessince6month (s/maybe s/Int)
                                                        :spacessince1year (s/maybe s/Int)
                                                        :Totalspacesno (s/maybe s/Int)})})
