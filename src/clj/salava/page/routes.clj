(ns salava.page.routes
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [salava.core.layout :as layout]
            [salava.page.main :as p]))

(defroutes* route-def
  (context* "/page" []
            (layout/main "/")
            (layout/main "/mypages")
            (layout/main "/view/:id")
            (layout/main "/edit/:id")
            (layout/main "/edit_theme/:id")
            (layout/main "/settings/:id"))

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
                   (ok (str (p/create-empty-page! context userid))))

            (GET* "/view/:pageid" []
                  ;:return schema/Page
                  :path-params [pageid :- Long]
                  :summary "View page"
                  :components [context]
                  (ok (p/page-with-blocks context pageid)))

            (GET* "/edit/:pageid" []
                  ;:return schema/Page
                  :path-params [pageid :- Long]
                  :summary "Edit page"
                  :components [context]
                  (ok (p/page-for-edit context pageid)))

            (GET* "/edit_theme/:pageid" []
                  ;:return schema/Page
                  :path-params [pageid :- Long]
                  :summary "Edit page theme"
                  :components [context]
                  (ok {:page (p/page-with-blocks context pageid)
                       :themes (p/themes-available)}))

            (GET* "/settings/:pageid" []
                  ;:return schema/Page
                  :path-params [pageid :- Long]
                  :summary "Edit page settings"
                  :components [context]
                  (ok (p/page-settings context pageid)))))
