(ns salava.badge.routes
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [ring.swagger.upload :as upload]
            [schema.core :as s]
            [salava.badge.schemas :as schemas] ;cljc
            [salava.badge.main :as b]
            [salava.badge.importer :as i]
            [salava.core.layout :as layout]))

(defroutes* route-def
  (context* "/badge" []
            (layout/main "/")
            (layout/main "/mybadges")
            (layout/main "/info/:id")
            (layout/main "/import")
            (layout/main "/export")
            (layout/main "/upload")
            (layout/main "/stats"))

  (context* "/obpv1/badge" []
            (GET* "/:userid" []
                  :return [schemas/BadgeContent]
                  :path-params [userid :- Long]
                  :summary "Get the badges of a specified user"
                  :components [context]
                  (ok (b/user-badges-all context userid)))

            (GET* "/info/:badgeid" []
                  ;:return schemas/BadgeContent
                  :path-params [badgeid :- Long]
                  :summary "Get badge"
                  :components [context]
                  (ok (b/get-badge context badgeid)))

            (POST* "/set_visibility/:badgeid" []
                   :path-params [badgeid :- Long]
                   :body-params [visibility :- (s/enum "private" "public")]
                   :summary "Set badge visibility"
                   :components [context]
                   (ok (str (b/set-visibility! context badgeid visibility))))

            (POST* "/set_status/:badgeid" []
                   :path-params [badgeid :- Long]
                   :body-params [status :- (s/enum "accepted" "declined")]
                   :summary "Set badge status"
                   :components [context]
                   (ok (str (b/set-status! context badgeid status))))

            (POST* "/toggle_recipient_name/:badgeid" []
                   :path-params [badgeid :- Long]
                   :body-params [show_recipient_name :- (s/enum false true)]
                   :summary "Set recipient name visibility"
                   :components [context]
                   (ok (str (b/toggle-show-recipient-name! context badgeid show_recipient_name))))

            (GET* "/export/:userid" []
                  :return [schemas/BadgeContent]
                  :path-params [userid :- Long]
                  :summary "Get the badges of a specified user for export"
                  :components [context]
                  (ok (b/user-badges-to-export context userid)))

            (GET* "/import/:userid" []
                  :return schemas/Import
                  :path-params [userid :- Long]
                  :summary "Fetch badges from Mozilla Backpack to import"
                  :components [context]
                  (ok (i/badges-to-import context userid)))

            (POST* "/import_selected/:userid" []
                   ;:return {:errors (s/maybe s/Str)
                   :path-params [userid :- Long]
                   :body-params [keys :- [s/Str]]
                   :summary "Import selected badges from Mozilla Backpack"
                   :components [context]
                   (ok (i/do-import context userid keys)))

            (POST* "/upload/:userid" []
                   :return schemas/Upload
                   :path-params [userid :- Long]
                   :multipart-params [file :- upload/TempFileUpload]
                   :middlewares [upload/wrap-multipart-params]
                   :summary "Upload badge PNG-file"
                   :components [context]
                   (ok (i/upload-badge context file userid)))

            (GET* "/settings/:badgeid" []
                  ;return schemas/badgeContent
                  :path-params [badgeid :- Long]
                  :summary "Get badge settings"
                  :components [context]
                  (ok (b/badge-settings context badgeid)))

            (POST* "/save_settings/:badgeid" []
                   :path-params [badgeid :- Long]
                   :body-params [visibility :- (s/enum "private" "public" "internal")
                                 evidence-url :- (s/maybe s/Str)
                                 rating :- (s/maybe (s/enum 0.5 1 1.5 2 2.5 3 3.5 4 4.5 5))
                                 tags :- (s/maybe [s/Str])]
                   :summary "Save badge settings"
                   :components [context]
                   (ok (b/save-badge-settings! context badgeid visibility evidence-url rating tags)))

            (DELETE* "/:badgeid" []
                     :path-params [badgeid :- Long]
                     :summary "Delete badge"
                     :components [context]
                     (ok (str (b/delete-badge! context badgeid))))))
