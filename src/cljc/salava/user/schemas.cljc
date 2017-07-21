(ns salava.user.schemas
  (:require [schema.core :as s
             :include-macros true ;; cljs only
             ]
            [salava.core.schema-helper :as h]
            [salava.core.countries :refer [all-countries]]))

(def contact-fields
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

(s/defschema User {:email      (s/constrained s/Str #(and (>= (count %) 1)
                                                          (<= (count %) 255)
                                                          (h/email-address? %)))
                   :first_name (s/constrained s/Str #(and (>= (count %) 1)
                                                          (<= (count %) 255)))
                   :last_name  (s/constrained s/Str #(and (>= (count %) 1)
                                                          (<= (count %) 255)))
                   :country    (apply s/enum (keys all-countries))
                   :language   (s/enum "fi" "en" "fr" "es" "pl")
                   :password   (s/constrained s/Str #(and (>= (count %) 6)
                                                          (<= (count %) 50)))
                   :password_verify (s/constrained s/Str #(and (>= (count %) 6)
                                                               (<= (count %) 50)))
                   :profile_visibility (s/enum "public" "internal")
                   :profile_picture (s/maybe s/Str)
                   :about (s/maybe s/Str)})

(s/defschema RegisterUser (merge {:token (s/maybe s/Str)}
                           (dissoc User :profile_visibility :profile_picture :about)))

(s/defschema LoginUser (select-keys User [:email :password]))

(s/defschema ActivateUser (merge {:code      s/Str
                                  :user_id   s/Int}
                                 (select-keys User [:password :password_verify])))

(s/defschema EditUser (-> {:email_notifications s/Bool}
                          (merge (select-keys User [:first_name :last_name :language :country]))))


(s/defschema EditUserPassword{:current_password (s/maybe (:password User))
                              :new_password (s/maybe (:password User))
                              :new_password_verify (s/maybe (:password User))})

(s/defschema EmailAddress {:email            (:email User)
                           :verified         s/Bool
                           :primary_address  s/Bool
                           :backpack_id      (s/maybe s/Int)
                           :ctime            s/Int
                           :mtime            s/Int})
