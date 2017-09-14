(ns salava.gallery.schemas
  (:require [schema.core :as s
             :include-macros true ;; cljs only
             ]
            [salava.core.countries :refer [all-countries]]
            [salava.user.schemas :as u]))

(s/defschema UserSearch {:name          (s/constrained s/Str #(and (>= (count %) 0)
                                                                   (<= (count %) 255)))
                         :country       (apply s/enum (conj (keys all-countries) "all"))
                         :common_badges s/Bool
                         :order_by      (s/enum "name" "ctime" "common_badge_count")})

(s/defschema UserProfiles (-> u/User
                              (select-keys [:first_name :last_name :country :profile_picture])
                              (merge {:id s/Int
                                      :ctime s/Int
                                      :common_badge_count s/Int})))

(s/defschema Countries (s/constrained [s/Str] (fn [c]
                                                (and
                                                  (some #(= (first c) %) (keys all-countries))
                                                  (some #(= (second c) %) (vals all-countries))))))


(s/defschema Badgesgallery {:badge_count s/Int
                            :badges       [{:badge_id            s/Str
                                            :ctime               s/Int
                                            :image_file          s/Str
                                            :issuer_content_name s/Str
                                            :name                s/Str
                                            :recipients          s/Int}]
                            :countries    [Countries]
                            :tags         [{:badge_id_count s/Int
                                            :badge_ids      s/Str
                                            :tag            s/Str}]
                            :user-country s/Str})


(s/defschema MultilanguageContent {:default_language_code s/Str
                                   :language_code         s/Str
                                   :name                  s/Str
                                   :badge_id              s/Str
                                   :image_file            s/Str
                                   :description           s/Str
                                   :issuer_content_name   s/Str
                                   :issuer_content_url    s/Str
                                   :issuer_description    (s/maybe s/Str)
                                   :issuer_verified       (s/maybe s/Int)
                                   :issuer_contact        (s/maybe s/Str)
                                   :issuer_image          (s/maybe s/Str)
                                   :creator_name          (s/maybe s/Str)
                                   :creator_description   (s/maybe s/Str)
                                   :creator_url           (s/maybe s/Str)
                                   :creator_email         (s/maybe s/Str)
                                   :creator_image         (s/maybe s/Str)
                                   :criteria_content      s/Str
                                   :criteria_url          s/Str
                                   :remote_url            (s/maybe s/Str)})

(s/defschema BadgeContent {:badge {:badge_id        s/Str
                                   :average_rating  (s/maybe s/Num)
                                                :content         [MultilanguageContent]
                                                :verified_by_obf s/Bool
                                                :issued_by_obf   s/Bool
                                                :issuer_verified (s/maybe s/Int)
                                                :obf_url         s/Str
                                                :remote_url      (s/maybe s/Str)
                                                :rating_count    (s/maybe s/Int)}
                           :public_users       (s/maybe [{:id              s/Int
                                                          :first_name      s/Str
                                                          :last_name       s/Str
                                                          :profile_picture (s/maybe s/Str)}])
                           :private_user_count (s/maybe s/Int)})
