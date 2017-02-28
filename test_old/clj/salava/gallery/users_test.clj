(ns salava.gallery.users-test
  (:require [midje.sweet :refer :all]
            [salava.test-utils :refer [test-api-request login! logout! test-user-credentials]]))

(def test-user 1)

(def search-data
  {:name ""
   :country "all"
   :common_badges false})

(facts "about viewing user profiles in gallery"
       (fact "user must be logged in to view profiles"
             (:status (test-api-request :post "/gallery/profiles" search-data)) => 401)

       (apply login! (test-user-credentials test-user))

       (fact "user's name must be valid"
             (:status (test-api-request :post "/gallery/profiles" (dissoc search-data :name))) => 400
             (:status (test-api-request :post "/gallery/profiles" (assoc search-data :name nil))) => 400
             (:status (test-api-request :post "/gallery/profiles" (assoc search-data :name (apply str (repeat 256 "a"))))) => 400
             (:status (test-api-request :post "/gallery/profiles" (assoc search-data :name "Test"))) => 200)

       (fact "country must be valid"
             (:status (test-api-request :post "/gallery/profiles" (dissoc search-data :country))) => 400
             (:status (test-api-request :post "/gallery/profiles" (assoc search-data :country nil))) => 400
             (:status (test-api-request :post "/gallery/profiles" (assoc search-data :country ""))) => 400
             (:status (test-api-request :post "/gallery/profiles" (assoc search-data :country "XX"))) => 400
             (:status (test-api-request :post "/gallery/profiles" (assoc search-data :country "all"))) => 200
             (:status (test-api-request :post "/gallery/profiles" (assoc search-data :country "FI"))) => 200)

       (fact "show common badges option must be valid"
             (:status (test-api-request :post "/gallery/profiles" (dissoc search-data :common_badges))) => 400
             (:status (test-api-request :post "/gallery/profiles" (assoc search-data :common_badges nil))) => 400
             (:status (test-api-request :post "/gallery/profiles" (assoc search-data :common_badges true))) => 200
             (:status (test-api-request :post "/gallery/profiles" (assoc search-data :common_badges false))) => 200)

       (let [{:keys [status body]} (test-api-request :post "/gallery/profiles" search-data)
             users (:users body)]
         (fact "user can search profiles successfully"
               status => 200)
         (fact "there are five users with public profile"
               (count users) => 3)
         (fact "user's own profile is public"
               (some #(= test-user (:id %)) users) => true))

       (logout!))