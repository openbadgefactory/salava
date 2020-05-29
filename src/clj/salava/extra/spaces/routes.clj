(ns salava.extra.spaces.routes
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [ring.swagger.upload :as upload]
            [schema.core :as s]
            [salava.core.helper :refer [dump]]
            [salava.core.layout :as layout]
            [salava.extra.spaces.space :as space]
            [salava.extra.spaces.db :as db]
            [salava.extra.spaces.util :as util]
            [salava.core.access :as access]
            [salava.extra.spaces.schemas :as schemas] ;cljc
            [clojure.string :refer [split]]))

(defn route-def [ctx]
  (routes
   (context "/admin" []
            (layout/main ctx "/spaces")
            (layout/main ctx "/spaces/creator"))

   (context "/obpv1/spaces" []
            :tags ["spaces"]

            (GET "/" []
                  :auth-rules access/admin
                  :summary "Get all spaces"
                  :current-user current-user
                  (ok (db/all-spaces ctx)))

            (GET "/:id" []
                  :auth-rules access/admin
                  :summary "Get space"
                  :path-params [id :- s/Int]
                  :current-user current-user
                  (ok (space/get-space ctx id)))

            (POST "/create" []
                  :return {:status (s/enum  "success" "error") (s/optional-key :message) s/Str}
                  :body [space schemas/create-space]
                  :auth-rules access/admin
                  :summary "Create new space"
                  :current-user current-user
                  (ok (space/create! ctx space)))

            (POST "/suspend/:id" []
                  :return {:success s/Bool}
                  :summary "Suspend space"
                  :path-params [id :- s/Str]
                  (ok (space/suspend! ctx id)))

            (POST "/upload_image/:kind" []
                  :return {:status (s/enum "success" "error") :url s/Str (s/optional-key :message) (s/maybe s/Str)}
                  :multipart-params [file :- upload/TempFileUpload]
                  :path-params [kind :- (s/enum "logo" "banner")]
                  :middleware [upload/wrap-multipart-params]
                  :summary "Upload badge image (PNG)"
                  :auth-rules access/authenticated
                  :current-user current-user
                  (ok (util/upload-image ctx current-user file kind)))


            (DELETE "/delete/:id" []
                    :return {:success s/Bool}
                    :summary "Delete space"
                    :path-params [id :- s/Str]
                    (ok (space/delete! ctx id))))))
