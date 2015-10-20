(ns salava.page.routes
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [salava.core.layout :as layout]
            [salava.page.main :as p]))

(defroutes* route-def
  (context* "/page" []
            (layout/main "/")
            (layout/main "/mypages"))

  (context* "/obpv1/page" []
            (GET* "/:userid" []
                  ;:return [schema/Page]
                  :path-params [userid :- Long]
                  :summary "Get user pages"
                  :components [context]
                  (ok (p/user-pages-all context userid)))
            (POST* "/create" []
                   ;:return schema/Page
                   :body-params [userid :- Long]
                   :summary "Create a new empty page"
                   :components [context]
                   (ok (str (p/create-empty-page! context userid))))))
