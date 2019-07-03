(ns salava.page.routes
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [salava.core.layout :as layout]
            [salava.page.main :as p]
            [salava.page.themes :refer [themes]]
            [schema.core :as s]
            [salava.page.schemas :as schemas]
            [salava.core.access :as access]
            [ring.util.io :as io]
            [salava.page.pdf :as pdf]
            [salava.core.helper :refer [dump]]
            salava.core.restructure))

(defn route-def [ctx]
  (routes
    (context "/profile/page" []
             (layout/main ctx "/")
             (layout/main ctx "/mypages")
             (layout/main-meta ctx "/view/:id" :page)
             (layout/main-meta ctx "/view/:id/embed" :page)
             (layout/main ctx "/edit/:id")
             (layout/main ctx "/edit_theme/:id")
             (layout/main ctx "/settings/:id")
             (layout/main ctx "/preview/:id"))

    (context "/obpv1/page" []
             :tags ["page"]
             (GET "/" []
                  :return [schemas/Page]
                  :summary "Get user pages"
                  :auth-rules access/signed
                  :current-user current-user
                  (ok (p/user-pages-all ctx (:id current-user))))

             (POST "/create" []
                   :return s/Str
                   :summary "Create a new empty page"
                   :auth-rules access/signed
                   :current-user current-user
                   (ok (str (p/create-empty-page! ctx (:id current-user)))))

             (GET "/:pageid" []
                  :return schemas/ViewPage
                  :path-params [pageid :- s/Int]
                  :summary "Edit page theme"
                  :auth-rules access/signed
                  :current-user current-user
                  (let [page (p/page-with-blocks-for-owner ctx pageid (:id current-user))]
                    (ok (assoc page :owner? (= (:id current-user) (:user_id page))))))

             (GET "/view/:pageid" []
                  :return {:page         (s/maybe schemas/ViewPage)
                           :ask-password s/Bool}
                  :path-params [pageid :- s/Int]
                  :summary "View page"
                  :current-user current-user
                  (let [user-id (:id current-user)
                        page (p/page-with-blocks ctx pageid)
                        page-owner-id (:user_id page)
                        visibility (:visibility page)
                        password-protected? (and (= visibility "password")
                                                 (not= user-id page-owner-id))]
                    (if (or (and (= user-id page-owner-id) user-id page-owner-id)
                            (= visibility "public")
                            password-protected?
                            (and user-id
                                 (= visibility "internal")))
                      (ok {:page (if password-protected?
                                   nil
                                   (assoc page :owner? (= user-id page-owner-id)))
                           :ask-password password-protected?})
                      (if (and (not user-id) (= visibility "internal"))
                        (unauthorized)
                        (not-found)))))

             (GET "/export-to-pdf/:pageid/:page-name/:opt-header" []
                  :path-params [pageid :- s/Int
                                opt-header :- s/Str
                                page-name :- s/Str]
                  :summary "Export page to pdf"
                  :current-user current-user
                  (-> (io/piped-input-stream (pdf/generate-pdf ctx pageid (:id current-user) opt-header))
                      ok
                      (header  "Content-Disposition" (str "attachment; filename=\""page-name".pdf\""))
                      (header "Content-Type" "application/pdf")))


             (POST "/password/:pageid" []
                   :return schemas/ViewPage
                   :path-params [pageid :- s/Int]
                   :body-params [password :- s/Str]
                   :summary "View password protected page"
                   :current-user current-user
                   (let [page (p/page-with-blocks ctx pageid)
                         user-id (:id current-user)
                         page-owner-id (:user_id page)]
                     (if (= (:password page) password)
                       (ok (assoc page :owner? (= user-id page-owner-id)))
                       (unauthorized))))

             (GET "/edit/:pageid" []
                  :return schemas/EditPageContent
                  :path-params [pageid :- s/Int]
                  :summary "Edit page"
                  :auth-rules access/authenticated
                  :current-user current-user
                  (ok (p/page-for-edit ctx pageid (:id current-user))))

             (POST "/save_content/:pageid" []
                   :return {:status (s/enum "error" "success")
                            :message (s/maybe s/Str)}
                   :path-params [pageid :- s/Int]
                   :body [page-content schemas/SavePageContent]
                   :auth-rules access/authenticated
                   :current-user current-user
                   (ok (p/save-page-content! ctx pageid page-content (:id current-user))))

             (POST "/save_theme/:pageid" []
                   :return {:status (s/enum "error" "success")
                            :message (s/maybe s/Str)}
                   :path-params [pageid :- s/Int]
                   :body-params [theme :- s/Int
                                 border :- s/Int
                                 padding :- (s/constrained s/Int #(and (>= % 0) (<= % 50)))]
                   :summary "Save page theme"
                   :auth-rules access/authenticated
                   :current-user current-user
                   (ok (p/set-theme! ctx pageid theme border padding (:id current-user))))

             (GET "/settings/:pageid" []
                  :return schemas/PageSettings
                  :path-params [pageid :- s/Int]
                  :summary "Edit page settings"
                  :auth-rules access/authenticated
                  :current-user current-user
                  (ok (p/page-settings ctx pageid (:id current-user))))

             (POST "/save_settings/:pageid" []
                   :return {:status (s/enum "error" "success")
                            :message (s/maybe s/Str)}
                   :path-params [pageid :- s/Int]
                   :body-params [tags :- [s/Str]
                                 visibility :- (s/enum "public" "password" "internal" "private")
                                 password :- (s/maybe (s/constrained s/Str #(and (>= (count %) 0)
                                                                                 (<= (count %) 255))))]
                   :summary "Save page settings"
                   :auth-rules access/authenticated
                   :current-user current-user
                   (ok (p/save-page-settings! ctx pageid tags visibility password (:id current-user))))

             (POST "/toggle_visibility/:pageid" []
                   :return (s/conditional string? (s/enum "private" "public")
                                          :else {:status (s/enum "error" "success") :message (s/maybe s/Str)})
                   :path-params [pageid :- s/Int]
                   :body-params [visibility :- (s/enum "private" "public")]
                   :summary "Toggle page visibility"
                   :auth-rules access/authenticated
                   :current-user current-user
                   (ok (p/toggle-visibility! ctx pageid visibility (:id current-user))))

             (DELETE "/:pageid" []
                     :path-params [pageid :- s/Int]
                     :summary "Delete page"
                     :auth-rules access/authenticated
                     :current-user current-user
                     (ok (str (p/delete-page-by-id! ctx pageid (:id current-user))))))))
