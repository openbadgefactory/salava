(ns salava.extra.spaces.routes
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [schema.core :as s]
            [salava.core.helper :refer [dump]]
            [salava.core.layout :as layout]
            [salava.extra.spaces.space :as space]
            [salava.core.access :as access]
            [salava.extra.spaces.schemas :as schemas] ;cljc
            [clojure.string :refer [split]]))

(defn route-def [ctx]
  (routes
   (context "/obpv1/spaces" []
            :tags ["spaces"]
            (POST "/create" []
                  :return {:success s/Bool}
                  :body [space schemas/CreateSpace]
                  :summary "Create new space"
                  (ok (space/create! ctx space)))

            (POST "/suspend/:id" []
                  :return {:success s/Bool}
                  :summary "Suspend space"
                  :path-params [id :- s/Str]
                  (ok (space/suspend! ctx id)))

            (DELETE "/delete/:id" []
                    :return {:success s/Bool}
                    :summary "Delete space"
                    :path-params [id :- s/Str]
                    (ok (space/delete! ctx id))))))
