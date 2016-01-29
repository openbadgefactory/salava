(ns salava.gallery.badges-test
  (:require [midje.sweet :refer :all]
            [salava.test-utils :refer [test-api-request login! logout! test-user-credentials]]))

(def test-user 4)

(def another-user 5)

(def user-with-no-public-badges 3)

(def public-badge-id "f6e2a6480a832a29200007cfc602ed7e79b9ccb00b2611c58041e71a681216ea")

(def another-public-badge-id "c1a766b5c505e158b4ee1fb3c97df553d7aaac9bcbc49e450a6e71fd1cde8546")

(def not-existing-public-badge-id "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")

(def search-criteria
  {:recipient ""
   :issuer ""
   :badge ""
   :country ""})

(facts "about badge gallery"
       (fact "user must be logged in to view badges in gallery"
             (:status (test-api-request :post "/gallery/badges")) => 401)

       (apply login! (test-user-credentials test-user))

       (fact "recipient name, issuer name, badge name and country are required parameters when searching badges from gallery"
             (let [{:keys [status body]} (test-api-request :post "/gallery/badges" (dissoc search-criteria :recipient))]
               status => 400
               body => "{\"errors\":{\"recipient\":\"missing-required-key\"}}")
             (let [{:keys [status body]} (test-api-request :post "/gallery/badges" (dissoc search-criteria :issuer))]
               status => 400
               body => "{\"errors\":{\"issuer\":\"missing-required-key\"}}")
             (let [{:keys [status body]} (test-api-request :post "/gallery/badges" (dissoc search-criteria :badge))]
               status => 400
               body => "{\"errors\":{\"badge\":\"missing-required-key\"}}")
             (let [{:keys [status body]} (test-api-request :post "/gallery/badges" (dissoc search-criteria :country))]
               status => 400
               body => "{\"errors\":{\"country\":\"missing-required-key\"}}"))

       (fact "badges from same region as user are shown by default in badge gallery"
             (let [{:keys [status body]} (test-api-request :post "/gallery/badges" search-criteria)
                   {:keys [user-country badges countries]} body]
               status => 200
               user-country => "FI"
               (keys countries) => (just [:FI :US :SE] :in-any-order)
               (count badges) => 3
               (keys (first badges)) => (just [:description :name :image_file :recipients :issuer_name :ctime :id :issuer_url :mtime] :in-any-order)))

       (fact "badges from other regions can be searched"
             (let [{:keys [status body]} (test-api-request :post "/gallery/badges" (assoc search-criteria :country "SE"))
                   {:keys [user-country badges countries]} body]
               status => 200
               user-country => "FI"
               (keys countries) => (just [:FI :US :SE] :in-any-order)
               (count badges) => 1
               (keys (first badges)) => (just [:description :name :image_file :recipients :issuer_name :ctime :id :issuer_url :mtime] :in-any-order)))

       (fact "all public badges can be viewed"
             (let [{:keys [status body]} (test-api-request :post "/gallery/badges" (assoc search-criteria :country "all"))
                   {:keys [user-country badges countries]} body]
               status => 200
               user-country => "FI"
               (keys countries) => (just [:FI :US :SE] :in-any-order)
               (count badges) => 4
               (keys (first badges)) => (just [:description :name :image_file :recipients :issuer_name :ctime :id :issuer_url :mtime] :in-any-order)))

       (fact "badges can be searched by the name of the badge"
             (let [{:keys [status body]} (test-api-request :post "/gallery/badges" (assoc search-criteria :badge "testing gallery 2" :country "all"))
                   {:keys [user-country badges countries]} body]
               status => 200
               user-country => "FI"
               (keys countries) => (just [:FI :US :SE] :in-any-order)
               (count badges) => 1
               (keys (first badges)) => (just [:description :name :image_file :recipients :issuer_name :ctime :id :issuer_url :mtime] :in-any-order)))

       (fact "badges can be searched by recipient's name"
             (let [{:keys [status body]} (test-api-request :post "/gallery/badges" (assoc search-criteria :recipient "Example" :country "all"))
                   {:keys [user-country badges countries]} body]
               status => 200
               user-country => "FI"
               (keys countries) => (just [:FI :US :SE] :in-any-order)
               (count badges) => 1
               (keys (first badges)) => (just [:description :name :image_file :recipients :issuer_name :ctime :id :issuer_url :mtime] :in-any-order)))

       (fact "badges can be searched by the name of the issuer"
             (let [{:keys [status body]} (test-api-request :post "/gallery/badges" (assoc search-criteria :issuer "Test issuer" :country "all"))
                   {:keys [user-country badges countries]} body]
               status => 200
               user-country => "FI"
               (keys countries) => (just [:FI :US :SE] :in-any-order)
               (count badges) => 2
               (keys (first badges)) => (just [:description :name :image_file :recipients :issuer_name :ctime :id :issuer_url :mtime] :in-any-order)))

       (fact "if no badges match the search criteria, badge collection is empty"
             (let [{:keys [status body]} (test-api-request :post "/gallery/badges" (assoc search-criteria :badge "no results" :country "all"))
                   {:keys [user-country badges countries]} body]
               status => 200
               user-country => "FI"
               (keys countries) => (just [:FI :US :SE] :in-any-order)
               (count badges) => 0))

       (logout!)


       (apply login! (test-user-credentials another-user))

       (fact "badges from same region as user are shown by default in badge gallery"
             (let [{:keys [status body]} (test-api-request :post "/gallery/badges" search-criteria)
                   {:keys [user-country badges countries]} body]
               status => 200
               user-country => "SE"
               (keys countries) => (just [:FI :US :SE] :in-any-order)
               (count badges) => 1))

       (logout!))

(facts "about personal badge gallery"
       (fact "user must be logged in to view some user's public badges"
             (:status (test-api-request :post "/gallery/badges/2")) => 401)

       (apply login! (test-user-credentials test-user))

       (fact "user-id must be valid"
             (let [{:keys [status body]} (test-api-request :post (str "/gallery/badges/not-integer"))]
               status => 400
               body => "{\"errors\":{\"userid\":\"(not (integer? \\\"not-integer\\\"))\"}}"))

       (fact "user can view public badges of spesific user"
             (let [{:keys [status body]} (test-api-request :post (str "/gallery/badges/" test-user))
                   badges (:badges body)]
               status => 200
               (count badges) => 2
               (keys (first badges)) => (just [:description :name :image_file :expires_on :issued_on :issuer_name :id :issuer_url :badge_content_id :mtime :assertion_url :visibility] :in-any-order)))

       (fact "another user have no public badges"
             (let [{:keys [status body]} (test-api-request :post (str "/gallery/badges/" user-with-no-public-badges))
                   badges (:badges body)]
               status => 200
               (count badges) => 0))

       (fact "non-existing user have no public badges"
             (let [{:keys [status body]} (test-api-request :post (str "/gallery/badges/99"))
                   badges (:badges body)]
               status => 200
               (count badges) => 0))

       (logout!))

(facts "about badge's public view"
       (fact "user must be logged in to view badge's public information"
             (:status (test-api-request :get "/gallery/public_badge_content/f6e2a6480a832a29200007cfc602ed7e79b9ccb00b2611c58041e71a681216ea")) => 401)

       (apply login! (test-user-credentials test-user))

       (fact "badge-content-id must be valid"
             (let [{:keys [status body]} (test-api-request :get "/gallery/public_badge_content/not-valid")]
               status => 400
               body => (contains "{\"errors\":{\"badge-content-id\":\"(not"))

             (let [{:keys [status body]} (test-api-request :get (str "/gallery/public_badge_content/" not-existing-public-badge-id))]
               status => 500
               body => "{\"errors\":{\"badge\":{\"description\":\"missing-required-key\",\"name\":\"missing-required-key\",\"image_file\":\"missing-required-key\",\"issuer_contact\":\"missing-required-key\",\"issuer_name\":\"missing-required-key\",\"html_content\":\"missing-required-key\",\"criteria_url\":\"missing-required-key\",\"issuer_url\":\"missing-required-key\"}}}"))

       (fact "badge's public information is correct"
             (let [{:keys [status body]} (test-api-request :get (str "/gallery/public_badge_content/" public-badge-id))
                   {:keys [badge public_users private_user_count]} body]
               status => 200
               private_user_count => 1
               (count public_users) => 2
               (keys badge) => (just [:description :name :image_file :recipient :rating_count :issuer_contact :issuer_name :html_content :criteria_url :issuer_url :average_rating] :in-any-order))

             (let [{:keys [status body]} (test-api-request :get (str "/gallery/public_badge_content/" another-public-badge-id))
                   {:keys [badge public_users private_user_count]} body]
               status => 200
               private_user_count => 0
               (count public_users) => 1
               (keys badge) => (just [:description :name :image_file :recipient :rating_count :issuer_contact :issuer_name :html_content :criteria_url :issuer_url :average_rating] :in-any-order)))

       (logout!))
