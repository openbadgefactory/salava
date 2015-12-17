(ns salava.page.view-test
  (:require [midje.sweet :refer :all]
            [salava.test-utils :refer [test-api-request]]
            [salava.core.helper :refer [dump]]))

(def user-id 1)

(def page-id 1)

(facts "about viewing a page"
       (let [{:keys [status body]} (test-api-request :get (str "/page/view/" page-id))]
         (fact "page exists"
               status => 200
               (nil? body) => false)
         (fact "page has valid attributes" (keys body) => (just [:description :tags :first_name :last_name :password :name :visible_before :visible_after :theme :ctime :id :padding :border :blocks :user_id :visibility :mtime] :in-any-order)))

       (let [{:keys [status body]} (test-api-request :get (str "/page/view/99"))]
         (fact "page does not exist"
               status => 500
               body => "{\"errors\":{\"description\":\"missing-required-key\",\"tags\":\"missing-required-key\",\"first_name\":\"missing-required-key\",\"password\":\"missing-required-key\",\"name\":\"missing-required-key\",\"visible_before\":\"missing-required-key\",\"visible_after\":\"missing-required-key\",\"theme\":\"missing-required-key\",\"ctime\":\"missing-required-key\",\"id\":\"missing-required-key\",\"padding\":\"missing-required-key\",\"last_name\":\"missing-required-key\",\"user_id\":\"missing-required-key\",\"border\":\"(not (map? nil))\",\"visibility\":\"missing-required-key\",\"mtime\":\"missing-required-key\"}}")))

