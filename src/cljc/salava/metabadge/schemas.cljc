(ns salava.metabadge.schemas
  (:require [schema.core :as s :include-macros true ]
            [salava.badge.schemas :refer [Badge UserBadgeContent]]))

(s/defschema Badge-info {:name s/Str
                         :description (s/maybe s/Str)
                         :image (s/maybe s/Str)
                         :criteria (s/maybe s/Str)})

(s/defschema User_badge {:id s/Int
                         :issued_on (s/maybe s/Int)
                         :status (s/maybe (s/enum "pending" "accepted" "declined"))
                         :deleted s/Int})

(s/defschema Requiredbadges [{:badge-info Badge-info
                              :user_badge (s/maybe User_badge)
                              :current (s/maybe s/Bool)
                              :url (s/maybe s/Str)
                              :received (s/maybe s/Bool)
                              :id (s/maybe s/Str)
                              }])

(s/defschema Metabadgecontent {:required_badges Requiredbadges
                               :milestone? s/Bool
                               :badge (merge Badge-info {:received (s/maybe s/Bool)
                                                         :url (s/maybe s/Str)
                                                         :id (s/maybe s/Str)})
                               :name (s/maybe s/Str)
                               :min_required (s/maybe s/Int)
                               :id (s/maybe s/Str)})

(s/defschema Metabadge (s/maybe {:metabadge (s/maybe [Metabadgecontent])}))

(s/defschema Milestone? (s/maybe {(s/optional-key :required_badge) s/Bool
                                  (s/optional-key :milestone) s/Bool}))

(s/defschema Allmetabadges (s/maybe (merge Metabadgecontent {:completion_status s/Int})))
