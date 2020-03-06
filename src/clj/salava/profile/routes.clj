(ns salava.profile.routes
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [salava.core.layout :as layout]
            [salava.core.util :refer [get-base-path]]
            [schema.core :as s]
            [salava.profile.db :as p]
            [salava.core.access :as access]
            [salava.profile.schemas :as schemas]
            salava.core.restructure))

(defn route-def [ctx]
  (routes
    (context "/profile" []
             (layout/main-meta ctx "/:id" :user)
             (layout/main-meta ctx "/:id/embed" :user))

    (context "/obpv1/profile" []
             :tags ["user_profile"]

             (GET "/:userid" []
                  :return schemas/user-profile
                  :summary "Get user information and profile fields"
                  :path-params [userid :- s/Int]
                  :current-user current-user
                  (let [profile (p/user-information-and-profile ctx userid (:id current-user))
                        visibility (get-in profile [:user :profile_visibility])]
                    (if (or (= visibility "public")
                            (and (= visibility "internal") current-user))
                      (ok profile)
                      (ok (if (get-in profile [:user :id])
                            {:visibility "internal"}
                            {:visibility "gone"}))
                      #_(unauthorized))))

             (GET "/user/edit" []
                  :no-doc true
                  :return schemas/user-profile-for-edit
                  :summary "Get user information and profile fields for editing"
                  :auth-rules access/signed
                  :current-user current-user
                  (ok (p/user-profile-for-edit ctx (:id current-user))))

            (GET "/user/tips" []
                 :no-doc true
                 :summary "Calculate profile completion and get tips"
                 :auth-rules access/authenticated
                 :current-user current-user
                 (ok (p/profile-metrics ctx (:id current-user))))

            (POST "/user/edit" []
                  :no-doc true
                  :return {:status (s/enum "success" "error") :message s/Str}
                  :body [profile schemas/edit-user-profile]
                  :summary "Save user profile"
                  :auth-rules access/authenticated
                  :current-user current-user
                  (ok (p/save-user-profile ctx profile (:id current-user))))

            (PUT "/reorder" []
                  :return {:status (s/enum "success" "error") (s/optional-key :message) s/Str}
                  :body [data schemas/reorder-profile-resource]
                  :summary "Reorder tabs, blocks or profile fields"
                  :auth-rules access/authenticated
                  :current-user current-user
                  (ok (p/reorder! ctx data (:id current-user))))

            (PUT "/user/edit" []
                  :return {:status (s/enum "success" "error") :message s/Str}
                  :body [profile schemas/edit-user-profile-p]
                  :summary "Save user profile"
                  :auth-rules access/authenticated
                  :current-user current-user
                  (ok (p/save-user-profile-p ctx profile (:id current-user))))

            (PUT "/field" []
                 :return {:status (s/enum "success" "error") (s/optional-key :message) s/Str}
                 :body [fields schemas/fields]
                 :summary "Add profile field, multiple fields can be added at once"
                 :auth-rules access/authenticated
                 :current-user current-user
                 (ok (p/add-profile-fields! ctx fields (:id current-user))))

            (PUT "/tab" []
                  :return {:status (s/enum "success" "error") (s/optional-key :message) s/Str}
                  :body [tabs [(:id schemas/profile-tab)]]
                  :summary "Add pages as tabs to profile, multiple tabs can be added at once"
                  :auth-rules access/authenticated
                  :current-user current-user
                  (ok (p/add-profile-tabs! ctx tabs (:id current-user))))

            (PUT "/block" []
                 :return {:status (s/enum "success" "error") (s/optional-key :message) s/Str}
                 :body [block schemas/add-showcase-block]
                 :summary "Add block to profile. Currently, only badge showcase blocks can be added to profile. Add id to update existing block"
                 :auth-rules access/authenticated
                 :current-user current-user
                 (ok (p/add-profile-block! ctx block (:id current-user))))

            (DELETE "/block" []
                 :return {:status (s/enum "success" "error") (s/optional-key :message) s/Str}
                 :body [ids [s/Int]]
                 :summary "Delete showcase block. multiple blocks can be deleted at once"
                 :auth-rules access/authenticated
                 :current-user current-user
                 (ok (p/delete-profile-blocks! ctx ids (:id current-user))))

            (DELETE "/field" []
                   :return {:status (s/enum "success" "error") (s/optional-key :message) s/Str}
                   :body [ids [(s/maybe s/Int)]]
                   :summary "Delete profile field by id, multiple fields can be deleted at once"
                   :auth-rules access/authenticated
                   :current-user current-user
                   (ok (p/delete-profile-fields! ctx ids (:id current-user))))

           (DELETE "/tab" []
                   :return {:status (s/enum "success" "error") (s/optional-key :message) s/Str}
                   :body [tabs [(s/maybe s/Int)]]
                   :summary "Delete profile tab by id, multiple tabs can be deleted at once"
                   :auth-rules access/authenticated
                   :current-user current-user
                   (ok (p/delete-profile-tabs! ctx tabs (:id current-user)))))))
