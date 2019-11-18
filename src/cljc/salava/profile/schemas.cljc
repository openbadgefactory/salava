(ns salava.profile.schemas
  (:require [schema.core :as s
             :include-macros true]
            [salava.core.schema-helper :as h]
            [salava.core.countries :refer [all-countries]]))

#?(:cljs (defn describe [v _] v))

(defn either [s1 s2]
 #?(:clj (s/either s1 s2)
    :cljs (s/cond-pre s1 s2)))

(def block-types ["pages" "badges" "location" "showcase"])

(def additional-fields
  [{:type "email" :key :user/Emailaddress}
   {:type "phone" :key :user/Phonenumber}
   {:type "address" :key :user/Address}
   {:type "city" :key :user/City}
   {:type "state" :key :user/State}
   {:type "country" :key :user/Country}
   {:type "facebook" :key :user/Facebookaccount}
   {:type "linkedin" :key :user/LinkedInaccount}
   {:type "twitter" :key :user/Twitteraccount}
   {:type "pinterest" :key :user/Pinterestaccount}
   {:type "instagram" :key :user/Instagramaccount}
   {:type "blog" :key :user/Blog}])

(s/defschema badge {:id                           s/Int
                    :name                         s/Str
                    :image_file                   (s/maybe s/Str)
                    (s/optional-key :description) (s/maybe s/Str)
                    (s/optional-key :visibility)  (s/enum "private" "public" "internal")})

(s/defschema user-setting {(s/optional-key :activated) (s/maybe s/Bool)
                           (s/optional-key :email_notifications) (s/maybe s/Bool)
                           (s/optional-key :private) (s/maybe s/Bool)})

(s/defschema profile-field {:id          s/Int
                            :field_order s/Int
                            :value       (s/maybe s/Str)
                            :field       (s/constrained s/Str #(some (fn [f] (= % f) ) (map :type additional-fields)))})

(s/defschema badge-showcase {(s/optional-key :id)          s/Int
                             :type                         (s/eq "showcase")
                             (s/optional-key :block_order) s/Int
                             :title                        (s/maybe s/Str)
                             :badges                       [(s/maybe badge)]
                             :format                       (s/enum "short" "long")})

(s/defschema profile-block (either
                            {:block_order s/Int
                             :type (apply s/enum block-types)
                             :hidden s/Bool}
                            badge-showcase))

(s/defschema profile-tab {:id         s/Int
                          :name       s/Str
                          :visibility (s/enum "private" "public" "internal")})

(s/defschema picture-file {:id s/Int
                           :name s/Str
                           :path s/Str
                           :size s/Int
                           :mime_type s/Str
                           :ctime s/Int
                           :mtime s/Int})

(s/defschema user {:id                 s/Int
                   :role               (s/enum "user" "admin")
                   :first_name         (s/constrained s/Str #(and (>= (count %) 1)
                                                                  (<= (count %) 255)))
                   :last_name          (s/constrained s/Str #(and (>= (count %) 1)
                                                                  (<= (count %) 255)))
                   :country            (apply s/enum (keys all-countries))
                   :language           (s/enum "fi" "en" "fr" "es" "pl" "pt" "ar" "nl" "sv")
                   :profile_visibility (s/enum "public" "internal")
                   :profile_picture    (s/maybe s/Str)
                   :about              (s/maybe s/Str)})

(s/defschema user-profile {:user       (merge user user-setting)
                           :profile    [(s/maybe profile-field)]
                           :visibility (s/enum "public" "internal")
                           :blocks     [(s/maybe profile-block)]
                           :theme      s/Int
                           :tabs       [(s/maybe profile-tab)]
                           :owner?     s/Bool})

(s/defschema user-profile-for-edit {:user_id       s/Int
                                    :user          (-> user (select-keys [:about :profile_picture :profile_visibility]))
                                    :profile       [(s/maybe profile-field)]
                                    :picture_files [(s/maybe picture-file)]})

(s/defschema block-for-edit  (s/conditional
                               #(= (:type %) "showcase") badge-showcase
                               #(= (:type %) "badges")   {:block_order s/Int :type (s/eq "badges") :hidden (s/maybe s/Bool)}
                               #(= (:type %) "pages")    {:block_order s/Int :type (s/eq "pages") :hidden (s/maybe s/Bool)}
                               #(= (:type %) "location") {:block_order s/Int :type (s/eq "location") (s/optional-key :hidden) (s/maybe s/Bool)}))

(s/defschema edit-user-profile (assoc (-> user (select-keys [:profile_visibility :profile_picture :about]))
                                      :fields [{:field (apply s/enum (map :type additional-fields)) :value (s/maybe s/Str)}]
                                      :blocks [(s/maybe block-for-edit)]
                                      :theme (s/maybe s/Int)
                                      :tabs  [(s/maybe profile-tab #_{:id s/Int :name s/Str :visibility s/Str})]))

#_(s/defschema ShowcaseBlock {:type (s/eq "showcase")
                              :title  (s/maybe s/Str)
                              :badges [{:id (s/maybe s/Int) (s/optional-key :visibility) s/Str}]
                              :format (s/enum "short" "medium" "long")})

#_(s/defschema BlockForEdit (s/conditional
                                     #(= (:type %) "showcase") (assoc ShowcaseBlock (s/optional-key :id) s/Int
                                                                (s/optional-key :block_order) s/Int)
                                     #(= (:type %) "badges") {:block_order s/Int :type (s/eq "badges") :hidden (s/maybe s/Bool)}
                                     #(= (:type %) "pages") {:block_order s/Int :type (s/eq "pages") :hidden (s/maybe s/Bool)}
                                     #(= (:type %) "location") {:block_order s/Int :type (s/eq "location") (s/optional-key :hidden) (s/maybe s/Bool)}))

#_(s/defschema EditProfile (assoc (-> user (select-keys [:profile_visibility :profile_picture :about]))
                                  :fields [{:field (apply s/enum (map :type additional-fields)) :value (s/maybe s/Str)}]
                                  :blocks [(s/maybe block-for-edit)]
                                  :theme (s/maybe s/Int)
                                  :tabs  [(s/maybe profile-tab #_{:id s/Int :name s/Str :visibility s/Str})]))
