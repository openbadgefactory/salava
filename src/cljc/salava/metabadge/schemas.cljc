(ns salava.metabadge.schemas
  (:require [schema.core :as s :include-macros true ]
            [salava.badge.schemas :refer [Badge UserBadgeContent]]))

(s/defschema Metabadge {:name s/Str
                        :image_file (s/maybe s/Str)
                        (s/optional-key :metabadge_id) (s/maybe s/Str)
                        (s/optional-key :id) (s/maybe s/Str)
                        (s/optional-key :description) (s/maybe s/Str)
                        (s/optional-key :criteria_content) (s/maybe s/Str)
                        (s/optional-key :url) (s/maybe s/Str)
                        (s/optional-key :user_badge_id) s/Int
                        (s/optional-key :issued_on) s/Int
                        (s/optional-key :status) (s/enum "pending" "accepted" "declined")
                        (s/optional-key :deleted) s/Int
                        (s/optional-key :milestone?) s/Bool})



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
