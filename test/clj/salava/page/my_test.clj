(ns salava.page.my-test
  (:require [midje.sweet :refer :all]
            [salava.test-utils :refer [test-api-request login! logout! test-user-credentials]]
            [salava.core.i18n :refer [t]]))

(def test-user 1)

(def test-user-with-no-pages 3)

(def page-to-be-deleted 2)

(def page-owned-by-another-user 3)

(facts "about user's pages"
       (fact "user must be logged in to view pages"
             (:status (test-api-request :get (str "/page"))) => 401)

       (apply login! (test-user-credentials test-user))

       (let [{:keys [status body]} (test-api-request :get (str "/page"))]
         (fact "user has two pages"
               status => 200
               (count body) => 2)
         (fact "first page has valid attributes"
               (keys (first body)) => (just [:description :tags :password :name :visible_before :visible_after :theme :ctime :id :badges :padding :border :visibility :mtime] :in-any-order)))

       (logout!)


       (apply login! (test-user-credentials test-user-with-no-pages))

       (fact "another user has no pages"
             (let [{:keys [status body]} (test-api-request :get "/page")]
               status => 200
               body => []))

       (logout!))

(facts "about creating a page"
       (fact "user must be logged in to create a page"
             (:status (test-api-request :post "/page/create")) => 401)

       (apply login! (test-user-credentials test-user))

       (let [{:keys [status body]} (test-api-request :post "/page/create")
            new-page-id (Integer/parseInt body)]
         (fact "page was created successfully"
               status => 200
               new-page-id => 4)
         (let [my-pages-body (:body (test-api-request :get (str "/page")))]
           (fact "user has three pages"
                 (count my-pages-body) => 3)
           (fact "new page has the default name"
                 (->> my-pages-body
                      (filter #(= new-page-id (:id %)))
                      first
                      :name) => (t :page/Untitled))))
       (logout!))

(facts "about deleting a page"
       (fact "user must be logged in to delete a page"
             (:status (test-api-request :delete (str "/page/" page-to-be-deleted))) => 401)

       (apply login! (test-user-credentials test-user))

       (fact "user must own the page to delete it"
             (let [{:keys [status body]} (test-api-request :delete (str "/page/" page-owned-by-another-user))]
               status => 200
               body => nil))

       (fact "page cannot be deleted without a valid page-id"
             (:status (test-api-request :delete (str "/page/"))) => 404
             (let [{:keys [status body]} (test-api-request :delete (str "/page/not-integer"))]
               status => 400
               body => "{\"errors\":{\"pageid\":\"(not (integer? \\\"not-integer\\\"))\"}}"))

       (fact "page was deleted successfully"
             (let [{:keys [status body]} (test-api-request :delete (str "/page/" page-to-be-deleted))]
               status => 200
               body => 1))

       (fact "non-existing page was not deleted"
             (let [{:keys [status body]} (test-api-request :delete "/page/99")]
               status => 200
               body => nil))

       (fact "user has two pages"
             (let [{:keys [status body]} (test-api-request :get (str "/page"))]
               status => 200
               (count body) => 2))

       (logout!))