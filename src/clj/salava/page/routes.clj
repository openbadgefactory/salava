(ns salava.page.routes
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [salava.core.layout :as layout]
            [salava.page.main :as p]
            [salava.page.themes :refer [themes]]
            [schema.core :as s]
            [salava.page.schemas :as schemas]
            [salava.core.access :as access]
            salava.core.restructure))

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
            :tags ["page"]
            (GET* "/:userid" []
                  :return [schemas/Page]
                  :path-params [userid :- s/Int]
                  :summary "Get user pages"
                  :components [context]
                  :auth-rules access/authenticated
                  (ok (p/user-pages-all context userid)))

            (POST* "/create" []
                   :return s/Str
                   :body-params [userid :- s/Int]
                   :summary "Create a new empty page"
                   :components [context]
                   :auth-rules access/authenticated
                   (ok (str (p/create-empty-page! context userid))))

            (GET* "/view/:pageid" []
                  :return schemas/ViewPage
                  :path-params [pageid :- s/Int]
                  :summary "View page"
                  :components [context]
                  :auth-rules access/authenticated
                  (ok (p/page-with-blocks context pageid)))

            (GET* "/edit/:pageid" []
                  :return schemas/EditPageContent
                  :path-params [pageid :- s/Int]
                  :summary "Edit page"
                  :components [context]
                  (ok (p/page-for-edit context pageid)))

            (POST* "/save_content/:pageid" []
                   :path-params [pageid :- s/Int]
                   :body [page-content schemas/SavePageContent]
                   :components [context]
                   :auth-rules access/authenticated
                   (ok (p/save-page-content! context pageid page-content)))

            (GET* "/edit_theme/:pageid" []
                  :return schemas/ViewPage
                  :path-params [pageid :- s/Int]
                  :summary "Edit page theme"
                  :components [context]
                  :auth-rules access/authenticated
                  (ok (p/page-with-blocks context pageid)))

            (POST* "/save_theme/:pageid" []
                   :return s/Str
                   :path-params [pageid :- s/Int]
                   :body-params [theme :- s/Int
                                 border :- s/Int
                                 padding :- (s/constrained s/Int #(and (>= % 0) (<= % 50)))]
                   :summary "Save page theme"
                   :components [context]
                   :auth-rules access/authenticated
                   (ok (str (p/set-theme! context pageid theme border padding))))

            (GET* "/settings/:pageid" []
                  :return schemas/PageSettings
                  :path-params [pageid :- s/Int]
                  :summary "Edit page settings"
                  :components [context]
                  :auth-rules access/authenticated
                  (ok (p/page-settings context pageid)))

            (POST* "/save_settings/:pageid" []
                   :path-params [pageid :- s/Int]
                   :body-params [tags :- [s/Str]
                                 visibility :- (s/enum "public" "password" "internal" "private")
                                 password :- (s/maybe (s/constrained s/Str #(and (>= (count %) 0)
                                                                         (<= (count %) 255))))]
                   :summary "Save page settings"
                   :components [context]
                   :auth-rules access/authenticated
                   (ok (p/save-page-settings! context pageid tags visibility password)))

            (DELETE* "/:pageid" []
                     :path-params [pageid :- s/Int]
                     :summary "Delete page"
                     :components [context]
                     :auth-rules access/authenticated
                     (ok (str (p/delete-page-by-id! context pageid))))
            ))
