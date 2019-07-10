(ns salava.metabadge.schemas
  (:require [schema.core :as s :include-macros true ]
            [salava.badge.schemas :refer [Badge UserBadgeContent]]))

(s/defschema Metabadge {:name s/Str
                        (s/optional-key :image_file) (s/maybe s/Str)
                        (s/optional-key :metabadge_id) (s/maybe s/Str)
                        (s/optional-key :id) (s/maybe s/Str)
                        (s/optional-key :description) (s/maybe s/Str)
                        (s/optional-key :criteria_content) (s/maybe s/Str)
                        (s/optional-key :url) (s/maybe s/Str)
                        (s/optional-key :user_badge_id) s/Int
                        (s/optional-key :issued_on) s/Int
                        (s/optional-key :status) (s/enum "pending" "accepted" "declined")
                        (s/optional-key :deleted) s/Int
                        (s/optional-key :milestone?) s/Bool
                        (s/optional-key :received) s/Bool
                        (s/optional-key :image) (s/maybe s/Str)
                        (s/optional-key :criteria) (s/maybe s/Str)})



(s/defschema RequiredBadges [(-> Metabadge (assoc (s/optional-key :required_badge_id) s/Str))])

(s/defschema MilestoneBadge (s/maybe (-> Metabadge
                                         (assoc :min_required s/Int
                                                :completion_status s/Int
                                                :required_badges RequiredBadges))))

(s/defschema Milestone? (s/maybe {(s/optional-key :id) s/Bool
                                  (s/optional-key :milestone) s/Bool}))

(s/defschema BadgeMetabadge (s/maybe {(s/optional-key :milestones) [MilestoneBadge]
                                      (s/optional-key :required-in) [MilestoneBadge]}))

(s/defschema AllMetabadges {:in_progress [MilestoneBadge] :completed [MilestoneBadge]})



(s/defschema MetabadgeBadgeInput {:metabadge_id      s/Str
                                  :required_badge_id s/Str
                                  :name              s/Str
                                  :description       s/Str
                                  :criteria          s/Str
                                  :image             s/Str
                                  :ctime             s/Int
                                  :mtime             s/Int})

(s/defschema MetabadgeInput {:id           s/Str
                             :name         s/Str
                             :description  s/Str
                             :criteria     s/Str
                             :image        s/Str
                             :min_required s/Int
                             :ctime        s/Int
                             :mtime        s/Int
                             :required_badges [MetabadgeBadgeInput]})

(s/defschema MetabadgeUpdate {:remote_issuer_id s/Str
                              :metabadges [MetabadgeInput]
                              :deleted_metabadges [s/Str]
                              :deleted_badges [s/Str]})
