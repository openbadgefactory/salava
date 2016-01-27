(ns salava.page.theme-test
  (:require [midje.sweet :refer :all]
            [salava.test-utils :refer [test-api-request login! logout! test-user-credentials]]
            [salava.page.themes :refer [default-theme-id default-border-id border-attributes]]
            [salava.core.helper :refer [dump]]))

(def test-user 1)

(def page-id 1)

(def page-owned-by-another-user 3)

(facts "about editing a page theme"
       (fact "user must be logged in to edit page theme"
             (:status (test-api-request :get (str "/page/edit_theme/" page-id))) => 401)

       (apply login! (test-user-credentials test-user))

       (fact "user cannot try to edit theme of the page owned by another user"
             (let [{:keys [status body]} (test-api-request :get (str "/page/edit_theme/" page-owned-by-another-user))]
               status => 500
               body => "{\"errors\":\"(not (map? nil))\"}"))

       (let [{:keys [status body]} (test-api-request :get (str "/page/edit_theme/" page-id))]
         (fact "page exists"
               status => 200
               (nil? body) => false)
         (fact "page has valid attributes" (keys body) => (just [:description :tags :first_name :last_name :password :name :visible_before :visible_after :theme :ctime :id :padding :border :blocks :user_id :visibility :mtime] :in-any-order)))

       (logout!))

(facts "about saving page theme"
       (fact "user must be logged in to edit page theme"
             (:status (test-api-request :post (str "/page/save_theme/" page-id) {:theme 1 :border 1 :padding 0})) => 401)

       (apply login! (test-user-credentials test-user))

       (fact "user cannot try to edit theme of the page owned by another user"
             (let [{:keys [status body]} (test-api-request :post (str "/page/save_theme/" page-owned-by-another-user) {:theme 1 :border 1 :padding 0})]
               status => 200
               (:status body) => "error"))

       (fact "theme-id, border-id and padding are required"
             (let [{:keys [status body]} (test-api-request :post (str "/page/save_theme/" page-id) {})]
               status => 400
               body => "{\"errors\":{\"padding\":\"missing-required-key\",\"border\":\"missing-required-key\",\"theme\":\"missing-required-key\"}}")
             (let [{:keys [status body]} (test-api-request :post (str "/page/save_theme/" page-id) {:theme "not-valid" :border "not-valid" :padding "not-valid"})]
               status => 400
               body => "{\"errors\":{\"padding\":\"(not (integer? \\\"not-valid\\\"))\",\"border\":\"(not (integer? \\\"not-valid\\\"))\",\"theme\":\"(not (integer? \\\"not-valid\\\"))\"}}"))

       (fact "if theme-id and border-id are not valid, use default values"
             (:status (test-api-request :post (str "/page/save_theme/" page-id) {:theme 99 :border 99 :padding 0})) => 200
             (let [{:keys [status body]} (test-api-request :get (str "/page/view/" page-id))
                   theme (:theme body)
                   border (:border body)]
               status => 200
               theme => default-theme-id
               border => (border-attributes default-border-id)))

       (fact "padding must be valid"
             (let [{:keys [status body]} (test-api-request :post (str "/page/save_theme/" page-id) {:theme 1 :border 1 :padding -1})]
               status => 400
               body => (contains "{\"errors\":{\"padding\":\"(not "))
             (let [{:keys [status body]} (test-api-request :post (str "/page/save_theme/" page-id) {:theme 1 :border 1 :padding 51})]
               status => 400
               body => (contains "{\"errors\":{\"padding\":\"(not "))
             (:status (test-api-request :post (str "/page/save_theme/" page-id) {:theme 1 :border 1 :padding 0})) => 200
             (:status (test-api-request :post (str "/page/save_theme/" page-id) {:theme 1 :border 1 :padding 50})) => 200)

       (logout!))
