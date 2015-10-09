(ns salava.badge.routes
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [schema.core :as s]
            [salava.badge.schemas :as schemas] ;cljc
            [salava.badge.main :as b]
            [salava.core.layout :as layout]))

(defroutes* route-def
  (context* "/badge" []
            (layout/main "/")
            (layout/main "/show/:id")
            (layout/main "/import")
            (layout/main "/upload")
            (layout/main "/stats"))

  (context* "/obfv1/badge" []
            (GET* "/:userid" [userid]
                  :return [schemas/BadgeContent]
                  ;:path-params [userid :- s/Int]
                  :summary "Get the badges of a specified user"
                  :components [context]
                  (ok (b/userbadges context userid)))
            (GET* "/info/:badgeid" [badgeid]
                  ;:return schemas/BadgeContent
                  ;:path-params [badgeid :- s/Int]
                  :summary "Get badge"
                  :components [context]
                  (ok (b/badge context badgeid)))
            (POST* "/set_visibility/:badgeid" []
                   :path-params [badgeid :- s/Int]
                   :body-params [visibility :- (s/enum "private" "public")]
                   :summary "Set badge visibility"
                   :components [context]
                   (ok (b/set-visibility context badgeid visibility)))
            (POST* "/toggle_recipient_name/:badgeid" []
                   :path-params [badgeid :- s/Int]
                   :body-params [show-recipient-name :- (s/enum false true)]
                   :summary "Set recipient name visibility"
                   :components [context]
                   (ok (b/toggle-show-recipient-name context badgeid show-recipient-name)))))
