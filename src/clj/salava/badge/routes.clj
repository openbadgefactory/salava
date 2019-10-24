(ns salava.badge.routes
  (:require [clojure.pprint :refer [pprint]]
            [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [ring.util.io :as io]
            [ring.swagger.upload :as upload]
            [schema.core :as s]
            [salava.badge.schemas :as schemas] ;cljc
            [salava.badge.main :as b]
            [salava.badge.importer :as i]
            [salava.factory.db :as f]
            [salava.core.layout :as layout]
            [salava.core.access :as access]
            [salava.badge.pdf :as pdf]
            [salava.core.helper :refer [dump]]
            [salava.user.db :as u]
            [salava.badge.verify :as v]
            [salava.badge.endorsement :as e]
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
             #_(layout/main ctx "/export")
             (layout/main ctx "/receive/:id")
             #_(layout/main ctx "/application")
             (layout/main ctx "/user/endorsements"))

    (context "/obpv1/badge" []
             :tags  ["badge"]
             (GET "/" []
                  :return [schemas/UserBadgeContent]
                  :summary "Get the badges of a current user"
                  :auth-rules access/signed
                  :current-user current-user
                  (ok (b/user-badges-all ctx (:id current-user))))

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

             (GET "/verify/:badgeid" []
                  :path-params [badgeid :- Long]
                  :summary "verify badge"
                  :current-user current-user
                  (ok (v/verify-badge ctx badgeid)))

             (GET "/pending/:badgeid" req
                  :path-params [badgeid :- Long]
                  :summary "Get pending badge content"
                  (if (= badgeid (get-in req [:session :pending :user-badge-id]))
                    (ok (assoc (->> badgeid
                                    (b/fetch-badge ctx)
                                    (b/badge-issued-and-verified-by-obf ctx))
                          :user_exists? (u/email-exists? ctx (get-in req [:session :pending :email]))))

                    (not-found)))

             (GET "/issuer/:issuerid" []
                  :return schemas/IssuerContent
                  :path-params [issuerid :- String]
                  :summary "Get issuer details"
                  :current-user current-user
                  (ok (b/get-issuer-endorsements ctx issuerid)))

             (GET "/creator/:creatorid" []
                  :return schemas/CreatorContent
                  :path-params [creatorid :- String]
                  :summary "Get creator details"
                  :current-user current-user
                  (ok (b/get-creator ctx creatorid)))

             (GET "/endorsement/:badgeid" []
                  :return [schemas/Endorsement]
                  :path-params [badgeid :- String]
                  :summary "Get badge endorsements"
                  :current-user current-user
                  (ok (b/get-endorsements ctx badgeid)))

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
                   :body-params [visibility :- (s/enum "private" "public" "internal")]
                   :summary "Set badge visibility"
                   :auth-rules access/authenticated
                   :current-user current-user
                   (if (:private current-user)
                     (forbidden)
                     (ok (b/set-visibility! ctx badgeid visibility (:id current-user)))))

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

             (POST "/toggle_evidences_all/:badgeid" []
                   :path-params [badgeid :- Long]
                   :body-params [show_evidence :- (s/enum false true)]
                   :summary "disable or enable all badge evidences"
                   :auth-rules access/authenticated
                   :current-user current-user
                   (ok (str (b/toggle-show-all-evidences! ctx badgeid show_evidence (:id current-user)))))

             (POST "/congratulate/:badgeid" []
                   :return {:status (s/enum "success" "error") :message (s/maybe s/Str)}
                   :path-params [badgeid :- Long]
                   :summary "Congratulate user who received a badge"
                   :auth-rules access/authenticated
                   :current-user current-user
                   (ok (b/congratulate! ctx badgeid (:id current-user))))


             (GET "/export-to-pdf" [badges lang-option]
                  :summary "Export badges to PDF"
                  :auth-rules access/authenticated
                  :current-user current-user
                  (let [badge-ids (map #(Integer/parseInt %)  (vals badges))
                        h (if-not (empty? (rest badge-ids)) (str "attachment; filename=\"badge-collection_"lang-option".pdf\"") (str "attachment; filename=\"badge_"(first badge-ids)"_" lang-option ".pdf\""))]
                    (-> (io/piped-input-stream (pdf/generatePDF ctx (:id current-user) badge-ids lang-option))
                        ok
                        (header "Content-Disposition" h)
                        (header "Content-Type" "application/pdf"))))


             (GET "/export" []
                  :return {:emails [schemas/UserBackpackEmail] :badges [schemas/BadgesToExport]}
                  :summary "Get the badges of a specified user for export"
                  :auth-rules access/signed
                  :current-user current-user
                  (let [emails (i/user-emails-for-badge-export ctx (:id current-user)) #_(i/user-backpack-emails ctx (:id current-user))]
                    (if (:private current-user)
                      (forbidden)
                      (ok {:emails emails :badges (b/user-badges-to-export ctx (:id current-user))}))))


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

             (POST "/import_badge_with_assertion" []
                   :return schemas/Upload
                   :body-params [assertion :- s/Str]
                   :summary "Import badge with assertion url"
                   :auth-rules access/authenticated
                   :current-user current-user
                   (if (:private current-user)
                     (forbidden)
                     (ok (i/upload-badge-via-assertion ctx assertion current-user))))


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
                                 rating :- (s/maybe (s/enum 5 10 15 20 25 30 35 40 45 50))
                                 tags :- (s/maybe [s/Str])]
                   :summary "Save badge settings"
                   :auth-rules access/authenticated
                   :current-user current-user
                   (ok (b/save-badge-settings! ctx badgeid (:id current-user) visibility rating tags)))

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
                  :summary "Get user's badge statistics about badges, badge view counts, congratulations and issuers"
                  :auth-rules access/signed
                  :current-user current-user
                  (ok (b/badge-stats ctx (:id current-user))))

             (GET "/stats/views/:id" []
                  :path-params [id :- Long]
                  :summary "Get user-badge view statistics"
                  :auth-rules access/signed
                  :current-user current-user
                  (ok (b/badge-view-stats ctx id)))

             (POST "/evidence/:user-badge-id" []
                   :return {:status (s/enum "success" "error")}
                   :path-params [user-badge-id :- Long]
                   :body-params [evidence :- schemas/Evidence]
                   :summary "Save badge evidence"
                   :auth-rules access/authenticated
                   :current-user current-user
                   (ok (b/save-badge-evidence ctx (:id current-user) user-badge-id evidence)))

             (DELETE "/evidence/:evidenceid" [user_badge_id]
                     :return {:status (s/enum "success" "error")}
                     :path-params [evidenceid :- Long]
                     :summary "Delete evidence"
                     :auth-rules access/authenticated
                     :current-user current-user
                     (ok (b/delete-evidence! ctx evidenceid user_badge_id (:id current-user))))


             (POST "/toggle_evidence/:evidenceid" []
                   :return {:status (s/enum "success" "error")}
                   :path-params [evidenceid :- Long]
                   :body-params [show_evidence :- (s/enum false true)
                                 user_badge_id :- Long]
                   :summary "Set evidence visibility"
                   :auth-rules access/authenticated
                   :current-user current-user
                   (ok (b/toggle-show-evidence! ctx user_badge_id evidenceid show_evidence (:id current-user))))

             (POST "/endorsement/:user-badge-id" []
                   :return {:id s/Int :status (s/enum "success" "error")}
                   :path-params [user-badge-id :- Long]
                   :body-params [content :- s/Str]
                   :summary "Endorse user badge"
                   :auth-rules access/authenticated
                   :current-user current-user
                   (ok (e/endorse! ctx user-badge-id (:id current-user) content)))

             (GET "/user/endorsement/:user-badge-id" []
                  :return [schemas/UserEndorsement]
                  :path-params [user-badge-id :- Long]
                  :summary "Get user badge endorsements"
                 ; :auth-rules access/authenticated
                  :current-user current-user
                  (ok (e/user-badge-endorsements ctx user-badge-id true)))

             (DELETE "/endorsement/:user-badge-id/:endorsement-id" []
                     :return {:status (s/enum "success" "error")}
                     :path-params [user-badge-id :- Long
                                   endorsement-id :- Long]
                     :summary "Delete endorsement"
                     :auth-rules access/authenticated
                     :current-user current-user
                     (ok (e/delete! ctx user-badge-id endorsement-id (:id current-user))))

             (POST "/endorsement/update_status/:endorsement-id" []
                   :return {:status (s/enum "success" "error")}
                   :path-params [endorsement-id :- Long]
                   :body-params [status :- (s/enum "accepted" "declined")
                                 user_badge_id :- s/Int]
                   :summary "Update endorsement status"
                   :auth-rules access/authenticated
                   :current-user current-user
                   (ok (e/update-status! ctx (:id current-user) user_badge_id endorsement-id status)))

             (POST "/endorsement/edit/:endorsement-id" []
                   :return {:status (s/enum "success" "error")}
                   :path-params [endorsement-id :- Long]
                   :body-params [content :- s/Str
                                 user_badge_id :- s/Int]
                   :summary "Edit endorsement"
                   :auth-rules access/authenticated
                   :current-user current-user
                   (ok (e/edit! ctx user_badge_id endorsement-id content (:id current-user))))

             (GET "/user/pending_endorsement" []
                  :return [schemas/UserEndorsement]
                  :auth-rules access/authenticated
                  :summary "Get pending badge endorsements"
                  :current-user current-user
                  (ok (e/received-pending-endorsements ctx (:id current-user))))

             (GET "/user/endorsements" []
                  :return schemas/AllEndorsements
                  :auth-rules access/signed
                  :summary "Get all user's endorsements"
                  :current-user current-user
                  (ok (e/all-user-endorsements ctx (:id current-user))))

             (POST "/endorsement/request/:user-badge-id" []
                  :return {:status (s/enum "success" "error")}
                  :path-params [user-badge-id :- Long]
                  :body-params [content :- s/Str
                                user-ids :- [s/Int]]
                  :auth-rules access/authenticated
                  :summary "Send endorsement request"
                  :current-user current-user
                  (ok (e/request-endorsement! ctx user-badge-id (:id current-user) user-ids content)))

             (POST "/endorsement/request/update_status/:request_id" []
                   :return {:status (s/enum "success" "error")}
                   :path-params [request_id :- Long]
                   :body-params [status :- s/Str]
                   :auth-rules access/authenticated
                   :summary "Update endorsement request status"
                   :current-user current-user
                   (ok (e/update-request-status! ctx request_id status (:id current-user))))

             (GET "/endorsement/pending_count/:user-badge-id" []
                  :return s/Int
                  :path-params [user-badge-id :- Long]
                  :auth-rules access/authenticated
                  :summary "Get user-badge pending endorsement count"
                  :current-user current-user
                  (ok (e/pending-endorsement-count ctx user-badge-id (:id current-user))))

             (GET "/user/pending_endorsement_request" []
                  :return [schemas/EndorsementRequest]
                  :auth-rules access/authenticated
                  :summary "Get pending badge endorsments"
                  :current-user current-user
                  (ok (e/endorsement-requests-pending ctx (:id current-user))))

             (GET "/endorsement/request/pending/:user-badge-id" []
                  :return [schemas/EndorsementRequest]
                  :auth-rules access/authenticated
                  :path-params [user-badge-id :- Long]
                  :summary "Return user badge's sent pending requests"
                  :current-user current-user
                  (ok (e/user-badge-pending-requests ctx user-badge-id (:id current-user)))))))
