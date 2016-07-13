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
