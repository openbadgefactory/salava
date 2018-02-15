(ns salava.badge.routes
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [ring.swagger.upload :as upload]
            [schema.core :as s]
            [salava.badge.schemas :as schemas] ;cljc
            [salava.badge.main :as b]
            [salava.badge.importer :as i]
            [salava.factory.db :as f]
            [salava.core.layout :as layout]
            [salava.core.access :as access]
            salava.core.restructure))

(defn route-def [ctx]
  (routes
    (context "/badge" []
             (layout/main ctx "/")
             (layout/main ctx "/mybadges")
             (layout/main-meta ctx "/info/:id" :badge)
             (layout/main-meta ctx "/info/:id/embed" :badge)
             (layout/main-meta ctx "/info/:id/pic/embed" :badge)
             (layout/main ctx "/import")
             (layout/main ctx "/export")
             (layout/main ctx "/upload")
             (layout/main ctx "/stats"))

    (context "/obpv1/badge" []
             :tags  ["badge"]
             (GET "/" []
                  :return [schemas/UserBadgeContent]
                  :summary "Get the badges of a current user"
                  :auth-rules access/signed
                  :current-user current-user
                  (do
                    ;(f/save-pending-assertions ctx (:id current-user))
                    (ok (b/user-badges-all ctx (:id current-user)))))

             (GET "/info/:badgeid" []
                  ;:return schemas/UserBadgeContent
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
                      (if (and (not user-id) (= visibility "internal"))
                        (unauthorized)
                        (not-found)))))

             (GET "/issuer/:issuerid" []
                  :return schemas/IssuerContent
                  :path-params [issuerid :- String]
                  :summary "Get issuer details"
                  :current-user current-user
                  (ok (b/get-issuer-endorsements ctx issuerid)))

             (GET "/endorsement/:badgeid" []
                  :return [schemas/Endorsement]
                  :path-params [badgeid :- Long]
                  :summary "Get badge endorsements"
                  :current-user current-user
                  (ok (b/get-endorsements ctx badgeid (:id current-user))))

             (GET "/info-embed/:badgeid" []
                  ;:return schemas/UserBadgeContent
                  :path-params [badgeid :- Long]
                  :summary "Get badge for embed view"
                  (let [user-id nil
                        badge (b/get-badge ctx badgeid user-id)
                        badge-owner-id (:owner badge)
                        visibility (:visibility badge)
                        owner? (= user-id badge-owner-id)]
                    (if (= visibility "public") 
                      (do
                        (if badge
                          (b/badge-viewed ctx badgeid user-id))
                        (ok (assoc badge :owner? owner?
                                   :user-logged-in? (boolean user-id))))
                      (not-found))))

             (POST "/set_visibility/:badgeid" []
                   :path-params [badgeid :- Long]
                   :body-params [visibility :- (s/enum "private" "public")]
                   :summary "Set badge visibility"
                   :auth-rules access/authenticated
                   :current-user current-user
                   (if (:private current-user)
                     (forbidden)
                     (ok (str (b/set-visibility! ctx badgeid visibility (:id current-user))))))

             (POST "/set_status/:user-badge-id" []
                   :path-params [user-badge-id :- Long]
                   :body-params [status :- (s/enum "accepted" "declined")]
                   :summary "Set badge status"
                   :auth-rules access/authenticated
                   :current-user current-user
                   (ok (str (b/set-status! ctx user-badge-id status (:id current-user)))))

             (POST "/toggle_recipient_name/:badgeid" []
                   :path-params [badgeid :- Long]
                   :body-params [show_recipient_name :- (s/enum false true)]
                   :summary "Set recipient name visibility"
                   :auth-rules access/authenticated
                   :current-user current-user
                   (ok (str (b/toggle-show-recipient-name! ctx badgeid show_recipient_name (:id current-user)))))

             (POST "/toggle_evidence/:badgeid" []
                   :path-params [badgeid :- Long]
                   :body-params [show_evidence :- (s/enum false true)]
                   :summary "Set evidence visibility"
                   :auth-rules access/authenticated
                   :current-user current-user
                   (ok (str (b/toggle-show-evidence! ctx badgeid show_evidence (:id current-user)))))

             (POST "/congratulate/:badgeid" []
                   :return {:status (s/enum "success" "error") :message (s/maybe s/Str)}
                   :path-params [badgeid :- Long]
                   :summary "Congratulate user who received a badge"
                   :auth-rules access/authenticated
                   :current-user current-user
                   (ok (b/congratulate! ctx badgeid (:id current-user))))

             (GET "/export" []
                  :return {:emails [s/Str] :badges [schemas/BadgesToExport]}
                  :summary "Get the badges of a specified user for export"
                  :auth-rules access/signed
                  :current-user current-user
                  (let [emails (i/user-backpack-emails ctx (:id current-user))]
                    (if (:private current-user)
                    (forbidden)
                    (ok {:emails emails :badges (b/user-badges-to-export ctx (:id current-user))}))
                    ))

             (GET "/import" []
                  :return schemas/Import
                  :summary "Fetch badges from Mozilla Backpack to import"
                  :auth-rules access/signed
                  :current-user current-user
                  (if (:private current-user)
                    (forbidden)
                    (ok (i/badges-to-import ctx (:id current-user)))))

             (POST "/import_selected" []
                   ;:return {:errors (s/maybe s/Str)
                   :body-params [keys :- [s/Str]]
                   :summary "Import selected badges from Mozilla Backpack"
                   :auth-rules access/authenticated
                   :current-user current-user
                   (if (:private current-user)
                     (forbidden)
                     (ok (i/do-import ctx (:id current-user) keys))))

             (POST "/upload" []
                   :return schemas/Upload
                   :multipart-params [file :- upload/TempFileUpload]
                   :middleware [upload/wrap-multipart-params]
                   :summary "Upload badge PNG or SVG file"
                   :auth-rules access/authenticated
                   :current-user current-user
                   (if (:private current-user)
                     (forbidden)
                     (ok (i/upload-badge ctx file (:id current-user)))))

             (GET "/settings/:user-badge-id" []
                  ;return schemas/UserBadgeContent
                  :path-params [user-badge-id :- Long]
                  :summary "Get badge settings"
                  :auth-rules access/authenticated
                  :current-user current-user
                  (ok (b/badge-settings ctx user-badge-id (:id current-user))))

             (POST "/save_settings/:badgeid" []
                   :return {:status (s/enum "success" "error")}
                   :path-params [badgeid :- Long]
                   :body-params [visibility :- (s/enum "private" "public" "internal")
                                 evidence-url :- (s/maybe s/Str)
                                 rating :- (s/maybe (s/enum 5 10 15 20 25 30 35 40 45 50))
                                 tags :- (s/maybe [s/Str])]
                   :summary "Save badge settings"
                   :auth-rules access/authenticated
                   :current-user current-user
                   (ok (b/save-badge-settings! ctx badgeid (:id current-user) visibility evidence-url rating tags)))
                   
             (POST "/save_raiting/:badgeid" []
                   :return {:status (s/enum "success" "error")}
                   :path-params [badgeid :- Long]
                   :body-params [rating :- (s/maybe (s/enum 5 10 15 20 25 30 35 40 45 50))]
                   :summary "Save badge raiting"
                   :auth-rules access/authenticated
                   :current-user current-user
                   (ok (b/save-badge-raiting! ctx badgeid (:id current-user) rating)))

             (DELETE "/:badgeid" []
                     :return {:status (s/enum "success" "error") :message (s/maybe s/Str)}
                     :path-params [badgeid :- Long]
                     :summary "Delete badge"
                     :auth-rules access/authenticated
                     :current-user current-user
                     (ok (b/delete-badge! ctx badgeid (:id current-user))))

             (GET "/stats" []
                  :return schemas/BadgeStats
                  :summary "Get badge statistics about badges, badge view counts, congratulations and issuers"
                  :auth-rules access/signed
                  :current-user current-user
                  (ok (b/badge-stats ctx (:id current-user)))))))
