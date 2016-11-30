(ns salava.oauth.routes
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [ring.util.response :refer [redirect]]
            [salava.core.layout :as layout]
            [schema.core :as s]
            [salava.core.util :refer [get-base-path]]
            [salava.oauth.db :as d]
            [salava.oauth.facebook :as f]
            [salava.oauth.linkedin :as l]
            [salava.core.access :as access]
            salava.core.restructure))

(defn route-def [ctx]
  (routes
    (context "/user" []
             (layout/main ctx "/oauth/facebook")
             (layout/main ctx "/oauth/linkedin"))


    (context "/oauth" []
             (GET "/facebook" []
                  :query-params [{code :- s/Str nil}
                                 {error :- s/Str nil}]
                  :current-user current-user
                  (let [{:keys [status user-id message role private]} (f/facebook-login ctx code (:id current-user) error)]
                    (if (= status "success")
                      (if current-user
                        (redirect (str (get-base-path ctx) "/user/oauth/facebook"))
                        (assoc-in (redirect (str (get-base-path ctx) "/social/stream"))[:session :identity] {:id user-id :role role :private role} ))
                      (if current-user
                        (assoc (redirect (str (get-base-path ctx) "/user/oauth/facebook")) :flash message)
                        (assoc (redirect (str (get-base-path ctx) "/user/login")) :flash message)))))

             (GET "/facebook/deauthorize" []
                  :query-params [code :- s/Str]
                  :auth-rules access/authenticated
                  :current-user current-user
                  (let [{:keys [status message]} (f/facebook-deauthorize ctx code (:id current-user))]
                    (if (= status "success")
                      (redirect (str (get-base-path ctx) "/user/oauth/facebook"))
                      (assoc (redirect (str (get-base-path ctx) "/user/oauth/facebook")) :flash message))))

             (GET "/linkedin" []
                  :query-params [{code :- s/Str nil}
                                 {state :- s/Str nil}
                                 {error :- s/Str nil}]
                  :current-user current-user
                  (let [r (l/linkedin-login ctx code state (:id current-user) error)
                        {:keys [status user-id message role]} r]
                    (if (= status "success")
                      (if current-user
                        (redirect (str (get-base-path ctx) "/user/oauth/linkedin"))
                        (assoc-in (redirect (str (get-base-path ctx) "/social/stream")) [:session :identity] {:id user-id :role role}))
                      (if current-user
                        (assoc (redirect (str (get-base-path ctx) "/user/oauth/linkedin")) :flash message)
                        (assoc (redirect (str (get-base-path ctx) "/user/login")) :flash message)))))

             (GET "/linkedin/deauthorize" []
                  :return {:status (s/enum "success" "error")
                           (s/optional-key :message) s/Str}
                  :auth-rules access/authenticated
                  :current-user current-user
                  (ok (l/linkedin-deauthorize ctx (:id current-user)))))

    (context "/obpv1/oauth" []
             :tags ["oauth"]
             (GET "/status/:service" []
                  :return {:active s/Bool :no-password? s/Bool}
                  :summary "Get user's remote service login status"
                  :path-params [service :- (s/enum "facebook" "linkedin")]
                  :auth-rules access/authenticated
                  :current-user current-user
                  (ok (d/login-status ctx (:id current-user) service))))))
