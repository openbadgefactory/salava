(ns salava.badgeIssuer.main
  (:require
   [salava.badgeIssuer.creator :refer [generate-image]]
   [salava.badgeIssuer.db :as db]))


(defn initialize
  ([ctx user]
   {:image (:url (generate-image ctx user))
    :name ""
    :criteria ""
    :description ""
    :tags ""
    :issuable_from_gallery 0
    :id nil})
  ([ctx user id]
   (let [selfie-badge (db/user-selfie-badge ctx (:id user) id)]
     selfie-badge)))
