(ns salava.admin.schemas
  (:require [schema.core :as s
             :include-macros true ;; cljs only
             ]))

(s/defschema Stats {:register-users (s/maybe s/Int)
                    :last-month-active-users (s/maybe s/Int)
                    :last-month-registered-users (s/maybe s/Int)
                    :all-badges (s/maybe s/Int)
                    :last-month-added-badges (s/maybe s/Int)
                    :pages (s/maybe s/Int)})

(s/defschema User-name-and-email {:name s/Str
                                  :email s/Str})

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
                     :last_name s/Str})

(s/defschema Url-parser {:item-type (s/enum "badge" "page" "user")
                         :item-id   (s/maybe s/Int)})
