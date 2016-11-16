(ns salava.user.password
  (:require [buddy.hashers :as hashers]))

(defn check-password [password hash]
  (hashers/check password hash))
