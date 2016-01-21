(ns salava.user.schemas
  (:require [schema.core :as s
             :include-macros true ;; cljs only
             ]
            [salava.core.schema-helper :as h]
            [salava.core.countries :refer [all-countries]]))

(s/defschema User {:email      (s/constrained s/Str #(and (>= (count %) 1)
                                                          (<= (count %) 255)
                                                          (h/email-address? %)))
                   :first_name (s/constrained s/Str #(and (>= (count %) 1)
                                                          (<= (count %) 255)))
                   :last_name  (s/constrained s/Str #(and (>= (count %) 1)
                                                          (<= (count %) 255)))
                   :country    (apply s/enum (keys all-countries))
                   :password   (s/constrained s/Str #(and (>= (count %) 6)
                                                          (<= (count %) 50)))
                   :password_verify (s/constrained s/Str #(and (>= (count %) 6)
                                                               (<= (count %) 50)))})

(s/defschema RegisterUser (dissoc User :password :password_verify))

(s/defschema LoginUser (select-keys User [:email :password]))

(s/defschema ActivateUser (merge {:code      s/Str
                                  :user_id   s/Int}
                                 (select-keys User [:password :password_verify])))