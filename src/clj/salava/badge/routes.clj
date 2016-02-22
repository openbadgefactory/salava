(ns salava.badge.routes
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [ring.swagger.upload :as upload]
            [schema.core :as s]
            [salava.badge.schemas :as schemas] ;cljc
            [salava.badge.main :as b]
            [salava.badge.importer :as i]
            [salava.core.layout :as layout]
            [salava.core.access :as access]
            salava.core.restructure))

(defn route-def [ctx]
  (routes
    (context "/badge" []
             (layout/main ctx "/")
             (layout/main ctx "/mybadges")
             (layout/main ctx "/info/:id")
             (layout/main ctx "/import")
             (layout/main ctx "/export")
             (layout/main ctx "/upload")
             (layout/main ctx "/stats"))

    (context "/obpv1/badge" []
             :tags  ["badge"]
             (GET "/" []
                  :return [schemas/BadgeContent]
                  :summary "Get the badges of a current user"
                  :auth-rules access/authenticated
                  :current-user current-user
                  (ok (b/user-badges-all ctx (:id current-user))))

             (GET "/info/:badgeid" []
                  ;:return schemas/BadgeContent
                  :path-params [badgeid :- Long]
                  :summary "Get badge"
                  :current-user current-user
                  (let [user-id (:id current-user)
                        badge (b/get-badge ctx badgeid user-id)
                        badge-owner-id (:owner badge)
                        visibility (:visibility badge)
                        owner? (= user-id badge-owner-id)]
                    (if (or (and user-id badge-owner-id owner?)
                            (= visibility "public")
                            (and user-id
                                 (= visibility "internal")))
                      (do
                        (if (and badge (not owner?))
                          (b/badge-viewed ctx badgeid user-id))
                        (ok (assoc badge :owner? owner?
                                         :user-logged-in? (boolean user-id))))
                      (unauthorized))))

             (POST "/set_visibility/:badgeid" []
                   :path-params [badgeid :- Long]
                   :body-params [visibility :- (s/enum "private" "public")]
                   :summary "Set badge visibility"
                   :auth-rules access/authenticated
                   :current-user current-user
                   (ok (str (b/set-visibility! ctx badgeid visibility (:id current-user)))))

             (POST "/set_status/:badgeid" []
                   :path-params [badgeid :- Long]
                   :body-params [status :- (s/enum "accepted" "declined")]
                   :summary "Set badge status"
                   :auth-rules access/authenticated
                   :current-user current-user
                   (ok (str (b/set-status! ctx badgeid status (:id current-user)))))

             (POST "/toggle_recipient_name/:badgeid" []
                   :path-params [badgeid :- Long]
                   :body-params [show_recipient_name :- (s/enum false true)]
                   :summary "Set recipient name visibility"
                   :auth-rules access/authenticated
                   :current-user current-user
                   (ok (str (b/toggle-show-recipient-name! ctx badgeid show_recipient_name (:id current-user)))))

             (POST "/congratulate/:badgeid" []
                   :path-params [badgeid :- Long]
                   :summary "Congratulate user who received a badge"
                   :auth-rules access/authenticated
                   :current-user current-user
                   (ok (str (b/congratulate! ctx badgeid (:id current-user)))))

             (GET "/export" []
                  :return [schemas/BadgesToExport]
                  :summary "Get the badges of a specified user for export"
                  :auth-rules access/authenticated
                  :current-user current-user
                  (ok (b/user-badges-to-export ctx (:id current-user))))

             (GET "/import" []
                  :return schemas/Import
                  :summary "Fetch badges from Mozilla Backpack to import"
                  :auth-rules access/authenticated
                  :current-user current-user
                  (ok (i/badges-to-import ctx (:id current-user))))

             (POST "/import_selected" []
                   ;:return {:errors (s/maybe s/Str)
                   :body-params [keys :- [s/Str]]
                   :summary "Import selected badges from Mozilla Backpack"
                   :auth-rules access/authenticated
                   :current-user current-user
                   (ok (i/do-import ctx (:id current-user) keys)))

             (POST "/upload" []
                   :return schemas/Upload
                   :multipart-params [file :- upload/TempFileUpload]
                   :middleware [upload/wrap-multipart-params]
                   :summary "Upload badge PNG-file"
                   :auth-rules access/authenticated
                   :current-user current-user
                   (ok (i/upload-badge ctx file (:id current-user))))

             (GET "/settings/:badgeid" []
                  ;return schemas/badgeContent
                  :path-params [badgeid :- Long]
                  :summary "Get badge settings"
                  :auth-rules access/authenticated
                  :current-user current-user
                  (ok (b/badge-settings ctx badgeid (:id current-user))))

             (POST "/save_settings/:badgeid" []
                   :path-params [badgeid :- Long]
                   :body-params [visibility :- (s/enum "private" "public" "internal")
                                 evidence-url :- (s/maybe s/Str)
                                 rating :- (s/maybe (s/enum 0.5 1 1.5 2 2.5 3 3.5 4 4.5 5))
                                 tags :- (s/maybe [s/Str])]
                   :summary "Save badge settings"
                   :auth-rules access/authenticated
                   :current-user current-user
                   (ok (b/save-badge-settings! ctx badgeid (:id current-user) visibility evidence-url rating tags)))

             (DELETE "/:badgeid" []
                     :path-params [badgeid :- Long]
                     :summary "Delete badge"
                     :auth-rules access/authenticated
                     :current-user current-user
                     (ok (str (b/delete-badge! ctx badgeid (:id current-user)))))

             (GET "/stats" []
                  :return schemas/BadgeStats
                  :summary "Get badge statistics about badges, badge view counts, congratulations and issuers"
                  :auth-rules access/authenticated
                  :current-user current-user
                  (ok (b/badge-stats ctx (:id current-user)))))))
