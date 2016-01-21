(ns salava.badge.routes
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [ring.swagger.upload :as upload]
            [schema.core :as s]
            [salava.badge.schemas :as schemas] ;cljc
            [salava.badge.main :as b]
            [salava.badge.importer :as i]
            [salava.core.layout :as layout]))

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
             (GET "/:userid" []
                  :return [schemas/BadgeContent]
                  :path-params [userid :- Long]
                  :summary "Get the badges of a specified user"
                  (ok (b/user-badges-all ctx userid)))

             (GET "/info/:badgeid" []
                  ;:return schemas/BadgeContent
                  :path-params [badgeid :- Long]
                  :summary "Get badge"
                  (ok (b/get-badge ctx badgeid)))

             (POST "/set_visibility/:badgeid" []
                   :path-params [badgeid :- Long]
                   :body-params [visibility :- (s/enum "private" "public")]
                   :summary "Set badge visibility"
                   (ok (str (b/set-visibility! ctx badgeid visibility))))

             (POST "/set_status/:badgeid" []
                   :path-params [badgeid :- Long]
                   :body-params [status :- (s/enum "accepted" "declined")]
                   :summary "Set badge status"
                   (ok (str (b/set-status! ctx badgeid status))))

             (POST "/toggle_recipient_name/:badgeid" []
                   :path-params [badgeid :- Long]
                   :body-params [show_recipient_name :- (s/enum false true)]
                   :summary "Set recipient name visibility"
                   (ok (str (b/toggle-show-recipient-name! ctx badgeid show_recipient_name))))

             (GET "/export/:userid" []
                  :return [schemas/BadgeContent]
                  :path-params [userid :- Long]
                  :summary "Get the badges of a specified user for export"
                  (ok (b/user-badges-to-export ctx userid)))

             (GET "/import/:userid" []
                  :return schemas/Import
                  :path-params [userid :- Long]
                  :summary "Fetch badges from Mozilla Backpack to import"
                  (ok (i/badges-to-import ctx userid)))

             (POST "/import_selected/:userid" []
                   ;:return {:errors (s/maybe s/Str)
                   :path-params [userid :- Long]
                   :body-params [keys :- [s/Str]]
                   :summary "Import selected badges from Mozilla Backpack"
                   (ok (i/do-import ctx userid keys)))

             (POST "/upload/:userid" []
                   :return schemas/Upload
                   :path-params [userid :- Long]
                   :multipart-params [file :- upload/TempFileUpload]
                   :middlewares [upload/wrap-multipart-params]
                   :summary "Upload badge PNG-file"
                   (ok (i/upload-badge ctx file userid)))

             (GET "/settings/:badgeid" []
                  ;return schemas/badgeContent
                  :path-params [badgeid :- Long]
                  :summary "Get badge settings"
                  (ok (b/badge-settings ctx badgeid)))

             (POST "/save_settings/:badgeid" []
                   :path-params [badgeid :- Long]
                   :body-params [visibility :- (s/enum "private" "public" "internal")
                                 evidence-url :- (s/maybe s/Str)
                                 rating :- (s/maybe (s/enum 0.5 1 1.5 2 2.5 3 3.5 4 4.5 5))
                                 tags :- (s/maybe [s/Str])]
                   :summary "Save badge settings"
                   (ok (b/save-badge-settings! ctx badgeid visibility evidence-url rating tags)))

             (DELETE "/:badgeid" []
                     :path-params [badgeid :- Long]
                     :summary "Delete badge"
                     (ok (str (b/delete-badge! ctx badgeid)))))))
