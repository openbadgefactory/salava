(ns salava.user.password
  (:require [buddy.hashers :as hashers]))

(defn check-password [password hash]
  (if (re-find #"^pbkdf2" hash)
    (hashers/check password hash)
    false))
