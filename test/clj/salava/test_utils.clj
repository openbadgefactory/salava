(ns salava.test-utils
  (:require [clj-http.client :as client]
            [clj-http.cookies :as cookies]
            [ring.mock.request :as mock]
            [com.stuartsierra.component :as component]
            [salava.core.system]
            [clojure.string :refer [split]]
            [salava.core.migrator :as migrator]
            [cheshire.core :as cheshire]
            [slingshot.slingshot :refer :all]))


(defn get-system []
  (-> (salava.core.system/base-system "test_config")
      (dissoc :http-server)
      (component/start)))

(defn stop-system [system]
  (migrator/reset-seeds (migrator/test-config))
  (component/stop system))


(defn parse-body [body]
  (if body
    (cheshire/parse-string (slurp body) true)))

(defn test-api-request
  ([system method url] (test-api-request system method url {} ""))
  ([system method url post-params login-token]
   (try+
    (let [response ((get-in system [:handler :handler]) (-> (mock/request method url)
                                                            (mock/header "cookie" login-token)
                                                            (mock/content-type "application/json")
                                                            (mock/body  (cheshire/generate-string post-params))))]
      (update response :body parse-body))
    (catch Exception _))))

(defn parse-cookie [headers]
  (-> headers
      (get  "Set-Cookie")
      first
      str
      (split  #";")
      first))

(:headers response)
(defn login [system email password]
  (let [response (test-api-request system :post "/app/obpv1/user/login" {:email email :password password} "")
        cookie (if (:headers response)
                 (parse-cookie (:headers response))
                 nil)]
    (assoc response :cookie cookie)))

(defn test-upload [url upload-data]
  (try+
    (client/request
      {:method           :post
       :url              (str api-root url)
       :multipart        upload-data
       :as               :json
       :throw-exceptions false
       :cookie-store *cookie-store*})
    (catch Exception _)))

(def test-users
  [{:id 1
    :email "test.user@example.com"
    :password "testtest"}
   {:id 2
    :email "another.user@example.com"
    :password "testtest"}
   {:id 3
    :email "third.user@example.com"
    :password "testtest"}
   {:id 4
    :email "fourth.user@example.com"
    :password "testtest"}
   {:id 5
    :email "fifth.user@example.com"
    :password "testtest"}
   {:id 6
    :email "sixth.user@example.com"
    :password "testtest"}
   {:id 7
    :email "seventh.user@example.com"
    :password "testtest"}
   {:id 8
    :email "eight.user@example.com"
    :password "testtest"}
   {:id 9
    :email "test.discendum@gmail.com"
    :password "testtest"}])


(defn login-with-user [system user-id]
  (let [user (->> test-users
                  (filter #(= (:id %) user-id))
                  first)]
    (login system (:email user) (:password user))))




