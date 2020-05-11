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
            [salava.badge.evidence :as evidence]
            [salava.badge.endorsement-schemas :as endoschemas]
            [salava.badge.pending :as p]
            [salava.badge.ext-endorsement :as ext]
            salava.core.restructure))

(defn route-def [ctx]
  (routes
   (context "/badge" []
            (layout/main ctx "/")
            (layout/main ctx "/mybadges")
            (layout/main-meta ctx "/info/:id" :badge)
            (layout/main-meta ctx "/info/:id/embed" :badge)
            (layout/main-meta ctx "/info/:id/pic/embed" :badge)
            (layout/main-meta ctx "/info/:id/full/embed" :badge)
            (layout/main ctx "/import")
            #_(layout/main ctx "/export")
            (layout/main ctx "/receive/:id")
            (layout/main ctx "/application")
            (layout/main ctx "/user/endorsements"))

   (context "/obpv1/p/badge" []
            :tags ["badge"]

            (GET "/" []
                 :return schemas/user-badges-p
                 :summary "Get the badges of a current user"
                 :auth-rules access/signed
                 :current-user current-user
                 (ok (b/user-badges-all-p ctx (:id current-user))))

            (GET "/info/:user-badge-id" []
                 :return schemas/user-badge-content-p
                 :path-params [user-badge-id :- Long]
                 :summary "Get badge content"
                 :current-user current-user
                 (let [user-id (:id current-user)
                       badge (b/get-badge-p ctx user-badge-id user-id)
                       badge-owner-id (:owner badge)
                       visibility (:visibility badge)
                       owner? (= user-id badge-owner-id)]
                   (if (or (and user-id badge-owner-id owner?)
                           (= visibility "public")
                           (and user-id
                                (= visibility "internal")))
                     (do
                       (if (and badge (not owner?))
                         (b/badge-viewed ctx user-badge-id user-id))
                       (ok badge))
                     (if (and (not user-id) (= visibility "internal"))
                       (unauthorized)
                       (not-found))))))

   (context "/obpv1/badge" []
            :tags  ["badge"]
            (GET "/" []
                 :return schemas/user-badges
                 :summary "Get the badges of a current user. Includes additional information used internally"
                 :auth-rules access/signed
                 :current-user current-user
                 (ok (b/user-badges-all ctx (:id current-user))))

            (GET "/info/:user-badge-id" []
                 :return schemas/user-badge-content
                 :path-params [user-badge-id :- Long]
                 :summary "Get badge content. Includes additional information used internally"
                 :current-user current-user
                 (let [user-id (:id current-user)
                       badge (b/get-badge ctx user-badge-id user-id)
                       badge-owner-id (:owner badge)
                       visibility (:visibility badge)
                       owner? (= user-id badge-owner-id)]
                   (if (or (and user-id badge-owner-id owner?)
                           (= visibility "public")
                           (and user-id
                                (= visibility "internal")))
                     (do
                       (if (and badge (not owner?))
                         (b/badge-viewed ctx user-badge-id user-id))
                       (ok (assoc badge :owner? owner?
                                  :user-logged-in? (boolean user-id))))
                     (if (and (not user-id) (= visibility "internal"))
                       (unauthorized)
                       (not-found)))))

            (GET "/verify/:user-badge-id" []
                 :return schemas/verify-badge
                 :path-params [user-badge-id :- Long]
                 :summary "verify badge"
                 :current-user current-user
                 (ok (v/verify-badge ctx user-badge-id)))

            (GET "/pending/:badgeid" req
                 :no-doc true
                 :path-params [badgeid :- Long]
                 :summary "Get pending badge content"
                 (if (= badgeid (get-in req [:session :pending :user-badge-id]))
                   (ok (p/pending-badge-content ctx req))
                   (not-found)))

            (GET "/pending_badges" []
                 :no-doc true
                 :summary "Check and return user's pending badges"
                 :auth-rules access/signed
                 :current-user current-user
                 (ok (p/pending-badges ctx (:id current-user))))

            (GET "/issuer/:issuerid" []
                 :return schemas/IssuerContent
                 :path-params [issuerid :- String]
                 :summary "Get issuer details"
                 :current-user current-user
                 (ok (b/get-issuer-endorsements ctx issuerid)))

            (GET "/creator/:creatorid" []
                 :return schemas/CreatorContent
                 :path-params [creatorid :- String]
                 :summary "Get badge creator details"
                 :current-user current-user
                 (ok (b/get-creator ctx creatorid)))

            (GET "/endorsement/:badgeid" []
                 :return schemas/badge-endorsements
                 :path-params [badgeid :- String]
                 :summary "Get badge endorsements"
                 :current-user current-user
                 (ok {:endorsements (b/get-endorsements ctx badgeid)}))

            (GET "/export-to-pdf" [id lang-option]
                 :no-doc true
                 :summary "Export badges to PDF"
                 :auth-rules access/authenticated
                 :current-user current-user
                 (let [];badge-ids (map #(Integer/parseInt %)  (vals badges))
                       ;h (if-not (empty? (rest badge-ids)) (str "attachment; filename=\"badge-collection_"lang-option".pdf\"") (str "attachment; filename=\"badge_"(first badge-ids)"_" lang-option ".pdf\""))]
                   (-> (io/piped-input-stream (pdf/generatePDF ctx (:id current-user) id lang-option))
                       ok
                       (header "Content-Disposition" (str "attachment; filename=\"badge_" id "_" lang-option ".pdf\""))
                       (header "Content-Type" "application/pdf"))))

            (GET "/info-embed/:user-badge-id" []
                 :no-doc true
                 :return (select-keys schemas/user-badge-content-p [:id :content])
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
                       (ok (select-keys badge [:content :id]) #_(assoc badge :owner? owner?
                                                                       :user-logged-in? (boolean user-id))))
                     (not-found))))

            (GET "/settings/:user-badge-id" []
                 :no-doc true
                 :return schemas/user-badge-settings
                 :path-params [user-badge-id :- Long]
                 :summary "Get badge settings"
                 :auth-rules access/authenticated
                 :current-user current-user
                 (ok (b/badge-settings ctx user-badge-id (:id current-user))))

            (GET "/stats" []
                 :return schemas/user-badges-statistics
                 :summary "Get statistics about user's badges, badge view counts, badge congratulations and badge issuers"
                 :auth-rules access/signed
                 :current-user current-user
                 (ok (b/badge-stats ctx (:id current-user))))

            (GET "/stats/:user-badge-id" []
                 :return schemas/user-badge-stats
                 :path-params [user-badge-id :- Long]
                 :summary "Get user-badge view statistics"
                 :auth-rules access/signed
                 :current-user current-user
                 (ok (b/badge-view-stats ctx user-badge-id)))

            (PUT "/settings/:user-badge-id" []
                 :return {:status (s/enum "success" "error") (s/optional-key :message) (s/maybe s/Str)}
                 :summary "update badge settings and badge tags"
                 :path-params [user-badge-id :- Long]
                 :body [data schemas/update-badge-settings]
                 :auth-rules access/authenticated
                 :current-user current-user
                 (if (:private current-user)
                   (forbidden)
                   (ok (b/save-badge-settings! ctx user-badge-id (:id current-user) data))))

            (POST "/set_visibility/:user-badge-id" []
                  :no-doc true
                  :return {:status (s/enum "success" "error")}
                  :path-params [user-badge-id :- Long]
                  :body-params [visibility :- (s/enum "private" "public" "internal")]
                  :summary "Set badge visibility"
                  :auth-rules access/authenticated
                  :current-user current-user
                  (if (:private current-user)
                    (forbidden)
                    (ok (b/set-visibility! ctx user-badge-id visibility (:id current-user)))))

            (POST "/set_status/:user-badge-id" []
                  :return {:status (s/enum "success" "error") :id s/Int (s/optional-key :message) (s/maybe s/Str)}
                  :path-params [user-badge-id :- Long]
                  :body-params [status :- (s/enum "accepted" "declined")]
                  :summary "Set badge status. Accept or decline badge"
                  :auth-rules access/authenticated
                  :current-user current-user
                  (ok (b/set-status! ctx user-badge-id status (:id current-user))))

            #_(POST "/toggle_recipient_name/:badgeid" []
                    :no-doc true
                    :path-params [badgeid :- Long]
                    :body-params [show_recipient_name :- (s/enum false true)]
                    :summary "Set recipient name visibility"
                    :auth-rules access/authenticated
                    :current-user current-user
                    (ok (str (b/toggle-show-recipient-name! ctx badgeid show_recipient_name (:id current-user)))))

            #_(POST "/toggle_evidences_all/:badgeid" []
                    :no-doc true
                    :path-params [badgeid :- Long]
                    :body-params [show_evidence :- (s/enum false true)]
                    :summary "disable or enable all badge evidences"
                    :auth-rules access/authenticated
                    :current-user current-user
                    (ok (str (b/toggle-show-all-evidences! ctx badgeid show_evidence (:id current-user)))))

            (POST "/congratulate/:user-badge-id" []
                  :return {:status (s/enum "success" "error") :message (s/maybe s/Str)}
                  :path-params [user-badge-id :- Long]
                  :summary "Congratulate user who received a badge"
                  :auth-rules access/authenticated
                  :current-user current-user
                  (ok (b/congratulate! ctx user-badge-id (:id current-user))))

            #_(GET "/export" []
                   :return {:emails [schemas/UserBackpackEmail] :badges [schemas/BadgesToExport]}
                   :summary "Get the badges of a specified user for export"
                   :auth-rules access/signed
                   :current-user current-user
                   (let [emails (i/user-emails-for-badge-export ctx (:id current-user)) #_(i/user-backpack-emails ctx (:id current-user))]
                     (if (:private current-user)
                       (forbidden)
                       (ok {:emails emails :badges (b/user-badges-to-export ctx (:id current-user))}))))

            #_(GET "/import" []
                   :return schemas/Import
                   :summary "Fetch badges from Mozilla Backpack to import"
                   :auth-rules access/signed
                   :current-user current-user
                   (if (:private current-user)
                     (forbidden)
                     (ok (i/badges-to-import ctx (:id current-user)))))

            #_(POST "/import_selected" []
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

            #_(POST "/save_settings/:badgeid" []
                    :return {:status (s/enum "success" "error")}
                    :path-params [badgeid :- Long]
                    :body-params [visibility :- (s/enum "private" "public" "internal")
                                  rating :- (s/maybe (s/enum 5 10 15 20 25 30 35 40 45 50))
                                  tags :- (s/maybe [s/Str])]
                    :summary "Save badge settings"
                    :auth-rules access/authenticated
                    :current-user current-user
                    (ok (b/save-badge-settings! ctx badgeid (:id current-user) visibility rating tags)))

            (POST "/save_rating/:badgeid" []
                  :return {:status (s/enum "success" "error")}
                  :path-params [badgeid :- Long]
                  :body-params [rating :- (s/maybe (s/enum 5 10 15 20 25 30 35 40 45 50))]
                  :summary "Save badge rating"
                  :auth-rules access/authenticated
                  :current-user current-user
                  (ok (b/save-badge-rating! ctx badgeid (:id current-user) rating)))

            (DELETE "/:badgeid" []
                    :return {:status (s/enum "success" "error") :message (s/maybe s/Str)}
                    :path-params [badgeid :- Long]
                    :summary "Delete badge"
                    :auth-rules access/authenticated
                    :current-user current-user
                    (ok (b/delete-badge! ctx badgeid (:id current-user)))))

   (context "/obpv1/badge/evidence" []
            :tags  ["badge_evidence"]

            (GET "/:user-badge-id" []
                 :return schemas/badge-evidence
                 :path-params [user-badge-id :- Long]
                 :summary "Get badge evidence"
                 :current-user current-user
                 (ok {:evidence (evidence/badge-evidence ctx user-badge-id (:id current-user))}))

            (POST "/:user-badge-id" []
                  :return {:status (s/enum "success" "error")}
                  :path-params [user-badge-id :- Long]
                  :body-params [evidence :- schemas/save-badge-evidence]
                  :summary "Save badge evidence"
                  :auth-rules access/authenticated
                  :current-user current-user
                  (ok (evidence/save-badge-evidence ctx (:id current-user) user-badge-id evidence)))

            (POST "/toggle_evidence/:evidenceid" []
                  :return {:status (s/enum "success" "error")}
                  :path-params [evidenceid :- Long]
                  :body-params [hide_evidence :- (s/enum false true)
                                user_badge_id :- Long]
                  :summary "Set evidence visibility"
                  :auth-rules access/authenticated
                  :current-user current-user
                  (ok (evidence/toggle-show-evidence! ctx user_badge_id evidenceid hide_evidence (:id current-user))))

            (DELETE "/:user_badge_id/:evidenceid" [user_badge_id]
                    :return {:status (s/enum "success" "error")}
                    :path-params [evidenceid :- Long
                                  user_badge_id :- Long]
                    :summary "Delete evidence"
                    :auth-rules access/authenticated
                    :current-user current-user
                    (ok (evidence/delete-evidence! ctx evidenceid user_badge_id (:id current-user)))))

   (context "/obpv1/badge/user_endorsement" []
            :tags ["badge_user_endorsements"]

            (GET "/:user-badge-id" []
                 :no-doc true
                 :return endoschemas/user-badge-endorsement
                 :path-params [user-badge-id :- Long]
                 :summary "Get user badge endorsements"
                 :current-user current-user
                 (ok {:endorsements (e/user-badge-endorsements ctx user-badge-id true)}))

            (GET "/p/:user-badge-id" []
                 :return endoschemas/user-badge-endorsements-p
                 :path-params [user-badge-id :- Long]
                 :summary "Get user badge endorsements"
                 :current-user current-user
                 (ok {:endorsements (e/user-badge-endorsements-p ctx user-badge-id)}))

            (GET "/count/:user-badge-id" []
                 :no-doc true
                 :return {:user_endorsement_count s/Int}
                 :path-params [user-badge-id :- Long]
                 :summary "Get accepted user badge endorsements count"
                 :current-user current-user
                 (ok (e/accepted-endorsement-count ctx user-badge-id (:id current-user))))

            (GET "/pending_count/:user-badge-id" []
                 :no-doc true
                 :return s/Int
                 :path-params [user-badge-id :- Long]
                 :auth-rules access/authenticated
                 :summary "Get user-badge pending endorsement count"
                 :current-user current-user
                 (ok (e/pending-endorsement-count ctx user-badge-id (:id current-user))))

            (GET "/_/pending" []
                 :no-doc true
                 :return endoschemas/pending-user-endorsements #_schemas/pending-user-endorsements
                 :auth-rules access/authenticated
                 :summary "Get pending badge endorsements"
                 :current-user current-user
                 (ok {:endorsements (e/received-pending-endorsements ctx (:id current-user))}))

            (GET "/_/all" []
                 :no-doc true
                 :return endoschemas/all-endorsements #_schemas/AllEndorsements
                 :auth-rules access/signed
                 :summary "Get all user's endorsements"
                 :current-user current-user
                 (ok (e/all-user-endorsements ctx (:id current-user))))

            (GET "/_p/all" []
                 :return endoschemas/all-endorsements-p
                 :auth-rules access/signed
                 :summary "Get all user's endorsements and endorsement requests"
                 :current-user current-user
                 (ok (e/all-user-endorsements-p ctx (:id current-user))))

            (GET "/request/pending" []
                 :no-doc true
                 :return endoschemas/pending-requests #_[schemas/EndorsementRequest]
                 :auth-rules access/authenticated
                 :summary "Get pending badge endorsement requests"
                 :current-user current-user
                 (ok (e/endorsement-requests-pending ctx (:id current-user))))

            (GET "/request/pending/:user-badge-id" []
                 :no-doc true
                 :return endoschemas/pending-sent-requests #_[schemas/EndorsementRequest]
                 :auth-rules access/authenticated
                 :path-params [user-badge-id :- Long]
                 :summary "Return user badge's sent pending requests"
                 :current-user current-user
                 (ok (e/user-badge-pending-requests ctx user-badge-id (:id current-user))))

            (GET "/ext_request/pending/:user-badge-id" []
                  :no-doc true
                  :return endoschemas/pending-ext-requests
                  :auth-rules access/authenticated
                  :path-params [user-badge-id :- Long]
                  :summary "Return user's externally sent pending requests"
                  :current-user current-user
                  (ok (ext/ext-pending-requests ctx user-badge-id)))

            (GET "/ext_request/info/:user-badge-id/:issuer-id" []
                  :no-doc true
                  :return {(s/optional-key :id) s/Int (s/optional-key :status) (s/enum "pending" "endorsed" "declined")}
                  :path-params [user-badge-id :- Long
                                issuer-id :- s/Str]
                  :summary "Return endorsement request id and current status"
                  (ok (ext/external-request-by-issuerid ctx user-badge-id issuer-id)))

            (GET "/ext_request/endorser/:id" []
                  :no-doc true
                  :return endoschemas/ext-endorser
                  :path-params [id :- s/Str]
                  :summary "Return external endorser's information if found"
                  (ok (ext/ext-endorser ctx id)))

            (GET "/ext/:user-badge-id/:issuer-id" []
                  :no-doc true
                  :return endoschemas/issued-ext-endorsement
                  :path-params [user-badge-id :- Long
                                issuer-id :- s/Str]
                  :summary "Return badge endorsement issued by external user"
                  (ok (ext/given-user-badge-endorsement ctx user-badge-id issuer-id)))

            (POST "/ext/all/:issuer-id" []
                  :no-doc true
                  :return endoschemas/issued-ext-endorsement-all
                  :path-params [issuer-id :- s/Str]
                  :summary "Return all endorsements issued by external user"
                  (ok (ext/all-endorsements ctx issuer-id)))

            (POST "/ext_request/all/:issuer-id" []
                  :no-doc true
                  :return endoschemas/endorsement-requests-all
                  :path-params [issuer-id :- s/Str]
                  :summary "Return all external user's endorsement requests"
                  (ok (ext/all-requests ctx issuer-id)))

            (POST "/ext_request/endorser/upload_image" []
                  :no-doc true
                  :return {:status (s/enum "success" "error") :url s/Str (s/optional-key :message) (s/maybe s/Str)}
                  :multipart-params [file :- upload/TempFileUpload]
                  :summary "Upload external endorser image (PNG)"
                  (ok (ext/upload-image ctx file)))

            (POST "/ext_request/update_status/:user-badge-id" []
                  :no-doc true
                  :return {:status (s/enum "error" "success")}
                  :path-params [user-badge-id :- s/Int]
                  :body-params [status :- s/Str
                                email :- s/Str]
                  :summary "Update external request's status"
                  (ok (ext/update-request-status ctx user-badge-id email status)))

            (POST "/edit/:endorsement-id" []
                  :return {:status (s/enum "success" "error")}
                  :path-params [endorsement-id :- Long]
                  :body-params [content :- s/Str
                                user_badge_id :- s/Int]
                  :summary "Edit endorsement"
                  :auth-rules access/authenticated
                  :current-user current-user
                  (ok (e/edit! ctx user_badge_id endorsement-id content (:id current-user))))

            (POST "/update_status/:endorsement-id" []
                  :return {:status (s/enum "success" "error")}
                  :path-params [endorsement-id :- Long]
                  :body-params [status :- (s/enum "accepted" "declined")
                                user_badge_id :- s/Int]
                  :summary "Update endorsement status"
                  :auth-rules access/authenticated
                  :current-user current-user
                  (ok (e/update-status! ctx (:id current-user) user_badge_id endorsement-id status)))

            (POST "/request/:user-badge-id" []
                  :return {:status (s/enum "success" "error")}
                  :path-params [user-badge-id :- Long]
                  :body [request endoschemas/request-endorsement] #_[content :- s/Str
                                                                     user-ids :- [s/Int]]
                  :auth-rules access/authenticated
                  :summary "Send endorsement request"
                  :current-user current-user
                  (ok (e/request-endorsement! ctx user-badge-id (:id current-user) request))) ;user-ids emails content)))

            (POST "/request/update_status/:request_id" []
                  :return {:status (s/enum "success" "error")}
                  :path-params [request_id :- Long]
                  :body-params [status :- s/Str]
                  :auth-rules access/authenticated
                  :summary "Update endorsement request status"
                  :current-user current-user
                  (ok (e/update-request-status! ctx request_id status (:id current-user))))

            (POST "/:user-badge-id" []
                  :return {(s/optional-key :id) s/Int :status (s/enum "success" "error") (s/optional-key :message) (s/maybe s/Str)}
                  :path-params [user-badge-id :- Long]
                  :body-params [content :- endoschemas/content]
                  :summary "Endorse user badge"
                  :auth-rules access/authenticated
                  :current-user current-user
                  (ok (e/endorse! ctx user-badge-id (:id current-user) content)))

            (POST "/ext/endorse/:user-badge-id" []
                   :no-doc true
                   :return {(s/optional-key :id) s/Int :status (s/enum "success" "error") (s/optional-key :message) (s/maybe s/Str)}
                   :path-params [user-badge-id :- Long]
                   :body [data endoschemas/save-ext-endorsement]
                   :summary "Endorse user badge externally"
                   (ok (ext/endorse! ctx user-badge-id data)))

            (POST "/ext/edit/:id/:user-badge-id" []
                   :no-doc true
                   :return {(s/optional-key :id) s/Int :status (s/enum "success" "error") (s/optional-key :message) (s/maybe s/Str)}
                   :path-params [user-badge-id :- Long
                                 id :- Long]
                   :body [data endoschemas/save-ext-endorsement]
                   :summary "Endorse user badge externally"
                   (ok (ext/update-endorsement! ctx id user-badge-id data)))

            (DELETE "/ext/endorsement/:id" []
                    :no-doc true
                    :return {:status (s/enum "error" "success")}
                    :path-params [id :- Long]
                    :summary "Delete endorsement"
                    (ok (ext/delete-endorsement! ctx id)))

            (DELETE "/:user-badge-id/:endorsement-id" []
                    :return {:status (s/enum "success" "error")}
                    :path-params [user-badge-id :- Long
                                  endorsement-id :- Long]
                    :summary "Delete endorsement"
                    :auth-rules access/authenticated
                    :current-user current-user
                    (ok (e/delete! ctx user-badge-id endorsement-id (:id current-user)))))))
