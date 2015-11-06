(ns salava.page.routes
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [salava.core.layout :as layout]
            [salava.page.main :as p]
            [salava.page.themes :refer [themes]]
            [schema.core :as s]))

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

            (POST* "/save_content/:id" []
                  :path-params [id :- Long]
                  :body-params [name :- s/Str
                                description :- (s/maybe s/Str)
                                blocks :- [{:type                      (s/enum "heading" "badge" "html" "file" "tag")
                                            (s/optional-key :id)       Long
                                            (s/optional-key :content)  (s/maybe s/Str)
                                            (s/optional-key :size)     (s/enum "h1" "h2")
                                            (s/optional-key :badge_id) (s/maybe Long)
                                            (s/optional-key :format)   (s/enum "short" "long")
                                            (s/optional-key :tag)      (s/maybe s/Str)
                                            (s/optional-key :sort)     (s/enum "name" "modified")}]]
                  :components [context]
                  (ok (p/save-page-content! context id name description blocks)))

            (GET* "/edit_theme/:pageid" []
                  ;:return schema/Page
                  :path-params [pageid :- Long]
                  :summary "Edit page theme"
                  :components [context]
                  (ok (p/page-with-blocks context pageid)))

            (POST* "/save_theme/:pageid" []
                   :path-params [pageid :- Long]
                   :body-params [theme :- Long]
                   :summary "Save page theme"
                   :components [context]
                   (ok (str (p/set-theme context pageid theme))))

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
            ))
