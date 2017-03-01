(ns salava.test-utils
  (:require [clj-http.client :as client]
            [clj-http.cookies :as cookies]
            [ring.mock.request :as mock]
            [com.stuartsierra.component :as component]
            [slingshot.slingshot :refer :all]))

(def test-config (-> (clojure.java.io/resource "test_config/core.edn") slurp read-string))

(def api-root
  (let [host (get-in test-config [:http :host])
        port (get-in test-config [:http :port])
        base-path (get-in test-config [:base-path] "")]
    (str "http://" host ":"port base-path "/obpv1" )))

(def ^:dynamic *cookie-store*
  (cookies/cookie-store))

(defn login! [email password]
  (client/request {:url (str api-root "/user/login")
                   :method :post
                   :form-params {:email email :password password}
                   :as :json
                   :content-type :json
                   :cookie-store *cookie-store*}))

(defn logout! []
  (client/request {:url (str api-root "/user/logout")
                   :method :post
                   :as :json
                   :content-type :json
                   :cookie-store *cookie-store*}))

(defn get-system []
  (-> (salava.core.system/base-system "test_config")
      (dissoc :http-server)
      (component/start)))

(defn stop-system [system]
  (component/stop system))

(defn test-api-request
  ([system method url] (test-api-request system method url {}))
  ([system method url post-params]
   (try+
    ((get-in system [:handler :handler]) (mock/request method url))
    (catch Exception _))))

(defn test-api-request1
  ([method url] (test-api-request method url {}))
  ([method url post-params]
   (try+
     (client/request
       {:method       method
        :url          (str api-root url)
        :content-type :json
        :as :json
        :form-params  post-params
        :throw-exceptions false
        :cookie-store *cookie-store*})
     (catch Exception _))))

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

(defn test-user-credentials [user-id]
  (let [user (->> test-users
                  (filter #(= (:id %) user-id))
                  first)]
    [(:email user) (:password user)]))


