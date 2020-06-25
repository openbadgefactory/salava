(ns salava.extra.customField.routes
 (:require
  [compojure.api.sweet :refer :all]
  [ring.util.http-response :refer :all]
  [schema.core :as s]
  [salava.core.access :as access]
  [salava.extra.customField.db :as db]))

(defn route-def [ctx]
  (routes
   (context "/obpv1/customField" []
            (GET "/gender" []
                 :return (s/enum "male" "female" "other")
                 :summary "Get user gender"
                 :auth-rules access/signed
                 :current-user current-user
                 (ok (db/custom-field-value ctx "gender" (:id current-user))))

            (POST "/gender" []
                 :return {:status (s/enum "success" "error")}
                 :summary "set user gender"
                 :auth-rules access/signed
                 :current-user current-user
                 :body-params [gender :- (s/enum "male" "female" "other")]
                 (ok (db/update-field ctx "gender" gender (:id current-user)))))))
