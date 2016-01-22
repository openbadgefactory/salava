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

(defn route-def [ctx]
  (routes
    (context "/page" []
             (layout/main ctx "/")
             (layout/main ctx "/mypages")
             (layout/main ctx "/view/:id")
             (layout/main ctx "/edit/:id")
             (layout/main ctx "/edit_theme/:id")
             (layout/main ctx "/settings/:id")
             (layout/main ctx "/preview/:id"))

    (context "/obpv1/page" []
             :tags ["page"]
             (GET "/" []
                  :return [schemas/Page]
                  :summary "Get user pages"
                  :auth-rules access/authenticated
                  :current-user current-user
                  (ok (p/user-pages-all ctx (:id current-user))))

             (POST "/create" []
                   :return s/Str
                   :summary "Create a new empty page"
                   :auth-rules access/authenticated
                   :current-user current-user
                   (ok (str (p/create-empty-page! ctx (:id current-user)))))

             (GET "/view/:pageid" []
                  :return schemas/ViewPage
                  :path-params [pageid :- s/Int]
                  :summary "View page"
                  :auth-rules access/authenticated
                  (ok (p/page-with-blocks ctx pageid)))

             (GET "/edit/:pageid" []
                  :return schemas/EditPageContent
                  :path-params [pageid :- s/Int]
                  :summary "Edit page"
                  (ok (p/page-for-edit ctx pageid)))

             (POST "/save_content/:pageid" []
                   :return {:status (s/enum "error" "success")
                            :message (s/maybe s/Str)}
                   :path-params [pageid :- s/Int]
                   :body [page-content schemas/SavePageContent]
                   :auth-rules access/authenticated
                   :current-user current-user
                   (ok (p/save-page-content! ctx pageid page-content (:id current-user))))

             (GET "/edit_theme/:pageid" []
                  :return schemas/ViewPage
                  :path-params [pageid :- s/Int]
                  :summary "Edit page theme"
                  :auth-rules access/authenticated
                  (ok (p/page-with-blocks ctx pageid)))

             (POST "/save_theme/:pageid" []
                   :return s/Str
                   :path-params [pageid :- s/Int]
                   :body-params [theme :- s/Int
                                 border :- s/Int
                                 padding :- (s/constrained s/Int #(and (>= % 0) (<= % 50)))]
                   :summary "Save page theme"
                   :auth-rules access/authenticated
                   :current-user current-user
                   (ok (str (p/set-theme! ctx pageid theme border padding (:id current-user)))))

             (GET "/settings/:pageid" []
                  :return schemas/PageSettings
                  :path-params [pageid :- s/Int]
                  :summary "Edit page settings"
                  :auth-rules access/authenticated
                  (ok (p/page-settings ctx pageid)))

             (POST "/save_settings/:pageid" []
                   :path-params [pageid :- s/Int]
                   :body-params [tags :- [s/Str]
                                 visibility :- (s/enum "public" "password" "internal" "private")
                                 password :- (s/maybe (s/constrained s/Str #(and (>= (count %) 0)
                                                                                 (<= (count %) 255))))]
                   :summary "Save page settings"
                   :auth-rules access/authenticated
                   :current-user current-user
                   (ok (p/save-page-settings! ctx pageid tags visibility password (:id current-user))))

             (DELETE "/:pageid" []
                     :path-params [pageid :- s/Int]
                     :summary "Delete page"
                     :auth-rules access/authenticated
                     :current-user current-user
                     (ok (str (p/delete-page-by-id! ctx pageid (:id current-user))))))))
