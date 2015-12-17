(ns salava.page.my-test
  (:require [midje.sweet :refer :all]
            [salava.test-utils :refer [test-api-request]]
            [salava.core.i18n :refer [t]]))

(def user-id 1)

(def page-to-be-deleted 2)

(facts "about user's pages"
       (let [{:keys [status body]} (test-api-request :get (str "/page/" user-id))]
         (fact "user has two pages"
               status => 200
               (count body) => 2)
         (fact "first page has valid attributes"
               (keys (first body)) => (just [:description :tags :password :name :visible_before :visible_after :theme :ctime :id :badges :padding :border :visibility :mtime] :in-any-order)))
       (fact "user has no pages"
             (let [{:keys [status body]} (test-api-request :get "/page/99")]
               status => 200
               body => [])))

(facts "about creating a page"
       (fact "new page cannot be created without a valid user-id"
             (let [{:keys [status body]} (test-api-request :post "/page/create" {})]
               status => 400
               body => "{\"errors\":{\"userid\":\"missing-required-key\"}}")
             (let [{:keys [status body]} (test-api-request :post "/page/create" {:not_valid_parameter 1})]
               status => 400
               body => "{\"errors\":{\"userid\":\"missing-required-key\",\"not_valid_parameter\":\"disallowed-key\"}}")
             (let [{:keys [status body]} (test-api-request :post "/page/create" {:userid "not integer"})]
               status => 400
               body => "{\"errors\":{\"userid\":\"(not (integer? \\\"not integer\\\"))\"}}"))
       (let [{:keys [status body]} (test-api-request :post "/page/create" {:userid user-id})
            new-page-id (Integer/parseInt body)]
         (fact "page was created successfully"
               status => 200
               new-page-id => 3)
         (let [my-pages-body (:body (test-api-request :get (str "/page/" user-id)))]
           (fact "user has three pages"
                 (count my-pages-body) => 3)
           (fact "new page has the default name"
                 (->> my-pages-body
                      (filter #(= new-page-id (:id %)))
                      first
                      :name) => (t :page/Untitled)))))

(facts "about deleting a page"
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
               body => 0))
       (fact "user has two pages"
             (let [{:keys [status body]} (test-api-request :get (str "/page/" user-id))]
               status => 200
               (count body) => 2)))