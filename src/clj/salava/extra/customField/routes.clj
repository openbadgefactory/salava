(ns salava.extra.customField.routes
 (:require
  [compojure.api.sweet :refer :all]
  [ring.util.http-response :refer :all]
  [schema.core :as s]
  [salava.core.access :as access]
  [salava.extra.customField.db :as db]
  [salava.extra.customField.schemas :as schemas]
  [salava.core.layout :as layout]))


(defn route-def [ctx]
  (routes
   (context "/admin" []
            (layout/main ctx "/customField"))

   (context "/obpv1/customField" []
            :tags ["customField"]
            :no-doc true

            (POST "/gender/value" []
                 :return schemas/gender #_(s/maybe (s/enum "male" "female" "other"))
                 :summary "Get user gender"
                 :auth-rules access/signed
                 :current-user current-user
                 (ok (db/custom-field-value ctx "gender" (:id current-user))))

            (POST "/gender/value/:user_id" []
                 :return schemas/gender 
                 :summary "Get user gender"
                 :auth-rules access/signed
                 :path-params [user_id :- s/Int]
                 :current-user current-user
                 (ok (db/custom-field-value ctx "gender" user_id)))

            (POST "/gender" []
                 :return {:status (s/enum "success" "error")}
                 :summary "set user gender"
                 :auth-rules access/signed
                 :current-user current-user
                 :body-params [gender :- schemas/gender #_(s/enum "male" "female" "other")]
                 (ok (db/update-field ctx "gender" gender (:id current-user))))

            (POST "/gender/register" req
                 :summary "Save new user gender"
                 :body-params [gender :- schemas/gender]
                 :current-user current-user
                 (-> (ok)
                     (assoc-in [:session] (assoc (get req :session {}) :custom-fields (merge (get-in req [:session :custom-fields] {}) {:gender gender})))))

            (POST "/org/register" req
                 :summary "Save new user organization"
                 :body-params [organization :- s/Str]
                 :current-user current-user
                 (-> (ok)
                     (assoc-in [:session] (assoc (get req :session {}) :custom-fields  (merge (get-in req [:session :custom-fields] {}) {:organization organization})))))

            (POST "/org/list" []
                  :summary "Get organization list"
                  :current-user current-user
                  (ok (db/organizations ctx)))

            (POST "/org/value" []
                  :return (s/maybe s/Str)
                  :summary "Get user organization"
                  :auth-rules access/signed
                  :current-user current-user
                  (ok (db/custom-field-value ctx "organization" (:id current-user))))

            (POST "/org/value/:user_id" []
                  :return (s/maybe s/Str)
                  :summary "Get user organization"
                  :auth-rules access/signed
                  :current-user current-user
                  :path-params [user_id :- s/Int]
                  (ok (db/custom-field-value ctx "organization" user_id)))

            (POST "/config/update/orglist" []
                  :return {:status (s/enum "success" "error")}
                  :summary "Update org list"
                  :auth-rules access/admin
                  :current-user current-user
                  :body-params [org :- [s/Str]]
                  (ok (db/update-organization-list ctx org)))

            (DELETE "/config/update/orglist/:id" []
                  :return {:status (s/enum "success" "error")}
                  :summary "Update org list"
                  :auth-rules access/admin
                  :current-user current-user
                  :path-params [id :- s/Int]
                  (ok (db/delete-organization! ctx id)))

            (POST "/org" []
                  :return {:status (s/enum "success" "error")}
                  :summary "set user organization"
                  :auth-rules access/signed
                  :current-user current-user
                  :body-params [organization :- s/Str]
                  (ok (db/update-field ctx "organization" organization (:id current-user)))))))
