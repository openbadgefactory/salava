(ns salava.extra.application.db
  (:require [clojure.tools.logging :as log]
            [yesql.core :refer [defqueries]]
            [clojure.tools.logging :as log]
            [clojure.java.jdbc :as jdbc]
            [salava.core.countries :refer [all-countries sort-countries]]
            [slingshot.slingshot :refer :all]
            [salava.core.util :refer [get-db]]))

(defqueries "sql/extra/application/queries.sql")




(defn add-badge-advert [ctx advert]
  (let [{:keys [remote_url remote_id remote_issuer_id info application_url issuer_content_id badge_content_id criteria_content_id kind country not_before not_after]} advert]
    (insert-badge-advert<! {:remote_url remote_url :remote_id remote_id :remote_issuer_id remote_issuer_id :info info :application_url application_url :issuer_content_id issuer_content_id :badge_content_id badge_content_id :criteria_content_id criteria_content_id :kind kind :country country :not_before not_before :not_after not_after} (get-db ctx))))

(defn get-badge-adverts [ctx]
  (select-badge-adverts {} (get-db ctx)))

(defn user-country
  "Return user's country id"
  [ctx user-id]
  (select-user-country {:id user-id} (into {:row-fn :country :result-set-fn first} (get-db ctx))))

(defn badge-adverts-countries
  "Return user's country id and list of all countries which have badge adverts"
  [ctx user-id]
  (let [current-country (user-country ctx user-id)
        countries (select-badge-advert-countries {} (into {:row-fn :country} (get-db ctx)))]
    (hash-map :countries (-> all-countries
                             (select-keys (conj countries current-country))
                             (sort-countries)
                             (seq))
              :user-country current-country)))

(def advert
  {:remote_url "http://www.google.fi"
   :remote_id "352"
   :remote_issuer_id "22"
   :info "" 
   :application_url "https://openbadgefactory.com/c/earnablebadge/NM6JZVe7HCeH/apply"
   :issuer_content_id "94ae9fd572b6a198364e8049c382cc3cd97bbea04dd7cfb079526c7d777d3e15"
   :badge_content_id "1602c43286576f04dc5718769177f3c59f2255125ea8969ef27e3b289dec4b99"
   :criteria_content_id "5fde78f923854eb457c428a0ba67ca1042f96cd0d072c1294362aad722cb3e8d" 
   :kind "application"
   :country "fi"
   :not_before nil
   :not_after nil})

(def ctx {:config {:core {:site-name "Perus salava"
 
                          :share {:site-name "jeejjoee"
                                  :hashtag "KovisKisko"}
                          
                          :site-url "http://localhost:3000"
                          
                          :base-path "/app"
                          
                          :asset-version 2
                          
                          :languages [:en :fi]
                          
                          :plugins [:badge :page :gallery :file :user :oauth :admin :social :registerlink :mail :extra/application :extra/passport]

                          :http {:host "localhost" :port 3000 :max-body 100000000}
                          :mail-sender "sender@example.com"}
                   :user {:email-notifications true}}
          :db (hikari-cp.core/make-datasource {:adapter "mysql",
                                               :username "root",
                                               :password "isokala",
                                               :database-name "salava_extra1",
                                               :server-name "localhost"})})



(add-badge-advert ctx advert)
(get-badge-adverts ctx)
