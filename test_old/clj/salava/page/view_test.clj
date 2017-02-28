(ns salava.page.view-test
  (:require [midje.sweet :refer :all]
            [salava.test-utils :refer [test-api-request login! logout! test-user-credentials]]
            [salava.core.helper :refer [dump]]))

(def test-user 1)

(def page-id 1)

(def page-owned-by-another-user 3)

(facts "about viewing a page"
       (fact "user must be logged in to view a page"
             (:status (test-api-request :get (str "/page/view/" page-id))) => 401)

       (apply login! (test-user-credentials test-user))

       (fact "user cannot view other user's page"
             (:status (test-api-request :get (str "/page/view/" page-owned-by-another-user))) => 401)

       (let [{:keys [status body]} (test-api-request :get (str "/page/view/" page-id))]
         (fact "page exists"
               status => 200
               (nil? body) => false)
         (fact "page has valid attributes" (keys (:page body)) => (just [:description :tags :first_name :password :name :owner? :visible_before :visible_after :theme :ctime :id :padding :last_name :blocks :user_id :border :visibility :mtime] :in-any-order)))

       (fact "page does not exist"
             (:status (test-api-request :get (str "/page/view/99"))) => 401)

       (logout!))

