(ns salava.page.routes
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [salava.core.layout :as layout]
            [salava.page.main :as p]
            [salava.page.themes :refer [themes]]
            [schema.core :as s]
            [salava.page.schemas :as schemas]))

(defroutes* route-def
  (context* "/page" []
            (layout/main "/")
            (layout/main "/mypages")
            (layout/main "/view/:id")
            (layout/main "/edit/:id")
            (layout/main "/edit_theme/:id")
            (layout/main "/settings/:id")
            (layout/main "/preview/:id"))

  (context* "/obpv1/page" []
            (GET* "/:userid" []
                  :return [schemas/Page]
                  :path-params [userid :- Long]
                  :summary "Get user pages"
                  :components [context]
                  (ok (p/user-pages-all context userid)))

            (POST* "/create" []
                   :return s/Str
                   :body-params [userid :- Long]
                   :summary "Create a new empty page"
                   :components [context]
                   (ok (str (p/create-empty-page! context userid))))

            (GET* "/view/:pageid" []
                  :return schemas/ViewPage
                  :path-params [pageid :- Long]
                  :summary "View page"
                  :components [context]
                  (ok (p/page-with-blocks context pageid)))

            (GET* "/edit/:pageid" []
                  :return schemas/EditPageContent
                  :path-params [pageid :- Long]
                  :summary "Edit page"
                  :components [context]
                  (ok (p/page-for-edit context pageid)))

            (POST* "/save_content/:id" []
                   :path-params [id :- Long]
                   :body [page-content schemas/SavePageContent]
                   :components [context]
                   (ok (p/save-page-content! context id page-content)))

            (GET* "/edit_theme/:pageid" []
                  ;:return schema/Page
                  :path-params [pageid :- Long]
                  :summary "Edit page theme"
                  :components [context]
                  (ok (p/page-with-blocks context pageid)))

            (POST* "/save_theme/:pageid" []
                   :path-params [pageid :- Long]
                   :body-params [theme :- Long
                                 border :- Long
                                 padding :- Long]
                   :summary "Save page theme"
                   :components [context]
                   (ok (str (p/set-theme context pageid theme border padding))))

            (GET* "/settings/:pageid" []
                  ;:return schema/Page
                  :path-params [pageid :- Long]
                  :summary "Edit page settings"
                  :components [context]
                  (ok (p/page-settings context pageid)))

            (POST* "/save_settings/:pageid" []
                   :path-params [pageid :- Long]
                   :body-params [tags :- [s/Str]
                                 visibility :- (s/enum "public" "password" "internal" "private")
                                 password :- (s/maybe s/Str)]
                   :summary "Save page settings"
                   :components [context]
                   (ok (p/save-page-settings! context pageid tags visibility password)))

            (DELETE* "/:pageid" []
                     :path-params [pageid :- Long]
                     :summary "Delete page"
                     :components [context]
                     (ok (str (p/delete-page-by-id! context pageid))))
            ))
