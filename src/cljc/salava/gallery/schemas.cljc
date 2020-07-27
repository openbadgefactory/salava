(ns salava.gallery.schemas
  #?(:clj (:require [schema.core :as s]
                    [schema.coerce :as c]
                    [salava.core.countries :refer [all-countries]]
                    [salava.user.schemas :as u]
                    [compojure.api.sweet :refer [describe]])
     :cljs (:require [schema.core :as s :include-macros true]
                     [salava.core.countries :refer [all-countries]]
                     [salava.user.schemas :as u]
                     [schema.coerce :as c])))

#?(:cljs (defn describe [v _] v))


(def constrained-str (s/constrained s/Str #(and (>= (count %) 0)
                                                (<= (count %) 255))))

(def gallery-id (describe s/Int "internal gallery badge id, use 0 if gallery badge id is unknown"))

(s/defschema UserSearch {:name          (s/constrained s/Str #(and (>= (count %) 0)
                                                                   (<= (count %) 255)))
                         :country       (apply s/enum (conj (keys all-countries) "all"))
                         :common_badges s/Bool
                         :order_by      (s/enum "name" "ctime" "common_badge_count")
                         (s/optional-key :email) (s/maybe s/Str)
                         (s/optional-key :page_count) (s/maybe s/Int)
                         (s/optional-key :space-id) s/Int
                         (s/optional-key :custom-field-filters) (s/maybe {(s/optional-key :gender) (s/maybe (s/enum "Male" "Female" "Notspecified" "notset"))
                                                                          (s/optional-key :organization) (s/maybe s/Str)})})

(s/defschema UserProfiles (-> u/User
                              (select-keys [:first_name :last_name :country :profile_picture])
                              (merge {:id s/Int
                                      :ctime s/Int
                                      :common_badge_count s/Int})
                              (assoc (s/optional-key :endorsement) (s/maybe {(s/optional-key :received) (s/maybe (s/enum "pending" "accepted"))
                                                                             (s/optional-key :request) (s/maybe s/Str)})
                                     (s/optional-key :gender) (s/maybe (s/enum "Male" "Female" "Notspecified" "notset"))
                                     (s/optional-key :organization) (s/maybe s/Str))))

(s/defschema Countries (s/constrained [s/Str] (fn [c]
                                                (and
                                                  (some #(= (first c) %) (keys all-countries))
                                                  (some #(= (second c) %) (vals all-countries))))))

(s/defschema GalleryBadges {:gallery_id          s/Int
                            :badge_id            s/Str
                            :ctime               s/Int
                            :image_file          s/Str
                            :issuer_content_name s/Str
                            :name                s/Str
                            :recipients          s/Int
                            (s/optional-key :selfie_id) (s/maybe s/Str)})


#_(s/defschema Badgesgallery {:badge_count s/Int
                              :badges       [GalleryBadges]
                              :countries    [Countries]
                              :tags         [{:badge_id_count s/Int
                                              :badge_ids      s/Str
                                              :tag            s/Str}]
                              :user-country s/Str})

(s/defschema Badgesgallery {:badge_count s/Int
                            :badges       [GalleryBadges]})

(s/defschema BadgesgalleryTags {:tags [s/Str]})

(s/defschema BadgesgalleryCountries {:countries [Countries]})

(s/defschema BadgeQuery {:country (describe s/Str "Filter by country code. Use all to get all badges")
                         (s/optional-key :tags) (describe constrained-str "Filter by tag")
                         (s/optional-key :badge-name) (describe constrained-str "Filter by badge name")
                         (s/optional-key :issuer-name)(describe constrained-str "Filter by issuer name")
                         (s/optional-key :order) (describe (s/enum "recipients" "mtime" "name" "issuer_content_name") "Select order, default mtime")
                         (s/optional-key :recipient-name) (describe constrained-str "Filter by badge earner")
                         :page_count (describe constrained-str "Page offset. 0 for first page, Each page returns 20 badges")
                         (s/optional-key :only-selfie?) (describe s/Bool "show only badges issued in passport")
                         (s/optional-key :space-id) (describe (s/maybe s/Int) "Show gallery badges within a space with space-id. ")
                         (s/optional-key :fetch-private) (s/maybe s/Bool)})

(s/defschema MultilanguageContent {:default_language_code s/Str
                                   :language_code         s/Str
                                   :name                  s/Str
                                   :badge_id              s/Str
                                   :image_file            s/Str
                                   :description           s/Str
                                   :issuer_content_id     s/Str
                                   :issuer_content_name   s/Str
                                   :issuer_content_url    s/Str
                                   :issuer_description    (s/maybe s/Str)
                                   :issuer_verified       (s/maybe s/Int)
                                   :issuer_contact        (s/maybe s/Str)
                                   :issuer_image          (s/maybe s/Str)
                                   :creator_content_id    (s/maybe s/Str)
                                   :creator_name          (s/maybe s/Str)
                                   :creator_description   (s/maybe s/Str)
                                   :creator_url           (s/maybe s/Str)
                                   :creator_email         (s/maybe s/Str)
                                   :creator_image         (s/maybe s/Str)
                                   :criteria_content      s/Str
                                   :criteria_url          s/Str
                                   :alignment             [(s/maybe {:name s/Str
                                                                     :url  s/Str
                                                                     :description s/Str})]
                                   :endorsement_count     (s/maybe s/Int)
                                   :remote_url            (s/maybe s/Str)
                                   (s/optional-key :last_received) (s/maybe s/Int)})

(s/defschema BadgeContent {:badge {:badge_id        s/Str
                                   :average_rating  (s/maybe s/Num)
                                   :content         [MultilanguageContent]
                                   :verified_by_obf s/Bool
                                   :issued_by_obf   s/Bool
                                   :issuer_verified (s/maybe s/Int)
                                   :obf_url         s/Str
                                   :remote_url      (s/maybe s/Str)
                                   :rating_count    (s/maybe s/Int)
                                   :endorsement_count (s/maybe s/Int)
                                   :gallery_id (s/maybe s/Int)}
                           :public_users       (s/maybe [{:id              s/Int
                                                          :first_name      s/Str
                                                          :last_name       s/Str
                                                          :profile_picture (s/maybe s/Str)}])
                           :private_user_count (s/maybe s/Int)})

(s/defschema badge-content-p {:badge {:badge_id s/Str
                                      :gallery_id (s/maybe s/Int)
                                      :content [(-> MultilanguageContent
                                                    (dissoc :remote_url :endorsement_count :issuer_verified))]}})

(s/defschema page-p {:description (s/maybe s/Str)
                     :id s/Int
                     :name s/Str
                     :badges [(s/maybe {:name s/Str :image_file (s/maybe s/Str)})]
                     :ctime s/Int
                     :mtime s/Int
                     :user_id (describe s/Int "internal id of page owner")})

(s/defschema page (-> page-p
                      (assoc :first_name constrained-str
                             :last_name constrained-str
                             :profile_picture (s/maybe s/Str))))

(s/defschema gallery-pages-p {:pages [(s/maybe page-p)] :user-country (s/maybe s/Str) :countries [Countries]})

(s/defschema gallery-pages (-> gallery-pages-p (assoc :pages [(s/maybe page)])))

(s/defschema pages-search {:country (describe (s/maybe s/Str) "Filter by country code. Use all to get all pages")
                           (s/optional-key :owner)(describe (s/maybe s/Str) "Search by page owner")
                           (s/optional-key :space-id) (describe (s/maybe  s/Int) "Show gallery pages within a space with space-id")})

(s/defschema public-badge-recipient {:id (s/maybe s/Int)
                                     :first_name (s/maybe s/Str)
                                     :last_name (s/maybe s/Str)
                                     :profile_picture (s/maybe s/Str)})

(s/defschema public-badge-recipient-p {:id (s/maybe s/Int)})

(s/defschema recipients {:all_recipients_count s/Int
                         :private_user_count s/Int
                         :public_users (describe [(s/maybe public-badge-recipient)] "User ids of users who have published their badge")})
