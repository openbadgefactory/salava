(ns salava.oauth.routes
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [ring.util.response :refer [redirect]]
            [salava.core.layout :as layout]
            [schema.core :as s]
            [salava.core.util :refer [get-base-path]]
            [salava.oauth.db :as d]
            [salava.oauth.facebook :as f]
            [salava.oauth.facebook :as g]
            [salava.oauth.linkedin :as l]
            [salava.user.db :as u]
            [salava.core.helper :refer [dump private?]]
            [salava.core.access :as access]
            salava.core.restructure))

(defn route-def [ctx]
  (routes
    (context "/user" []
             (layout/main ctx "/oauth/facebook")
             (layout/main ctx "/oauth/google")
             (layout/main ctx "/oauth/linkedin")
             #_(layout/main ctx "/terms"))


    (context "/oauth" []
             (GET "/google" []
                  :query-params [{code :- s/Str nil}
                                 {error :- s/Str nil}]
                  :current-user current-user
                  (let [{:keys [status user-id message role private]} (f/facebook-login ctx code (:id current-user) error)]
                    (if (= status "success")
                      (if current-user
                        (redirect (str (get-base-path ctx) "/user/oauth/facebook"))
                        (assoc-in (redirect (str (get-base-path ctx) "/social/stream"))[:session :identity] {:id user-id :role role :private private} ))
                      (if current-user
                        (assoc (redirect (str (get-base-path ctx) "/user/oauth/facebook")) :flash message)
                        (assoc (redirect (str (get-base-path ctx) "/user/login")) :flash message)))))


             (GET "/facebook" req
                  :query-params [{code :- s/Str nil}
                                 {error :- s/Str nil}]
                  :current-user current-user
                  (let [{:keys [status user-id message role private]} (f/facebook-login ctx code (:id current-user) error)
                        _ (if (= true (get-in req [:session :seen-terms])) (d/insert-user-terms ctx user-id "accepted"))
                        accepted-terms? (u/get-accepted-terms-by-id ctx user-id)]

                    (if (= status "success")

                      (if-not (= (:status accepted-terms?) "accepted")
                        (if current-user
                          (redirect (str (get-base-path ctx) "/user/terms/"))
                          ;(u/set-session ctx (found (str (get-base-path ctx) "/user/terms/")) user-id)
                          (u/finalize-login ctx (redirect (str (get-base-path ctx) "/user/terms/")) user-id (get-in req [:session :pending :user-badge-id]) false))

                        (if current-user
                          (redirect (str (get-base-path ctx) "/user/oauth/facebook"))
                          ;(u/set-session ctx (redirect (str (get-base-path ctx) "/social/stream")) user-id)
                          (u/finalize-login ctx (redirect (str (get-base-path ctx) "/social/stream")) user-id (get-in req [:session :pending :user-badge-id]) false)))

                        (if current-user
                          (assoc (redirect (str (get-base-path ctx) "/user/oauth/facebook")) :flash message)
                          (assoc (redirect (str (get-base-path ctx) "/user/login")) :flash message)))))

             (GET "/facebook/deauthorize" []
                  :query-params [code :- s/Str]
                  :auth-rules access/signed
                  :current-user current-user
                  (let [{:keys [status message]} (f/facebook-deauthorize ctx code (:id current-user))]
                    (if (= status "success")
                      (redirect (str (get-base-path ctx) "/user/oauth/facebook"))
                      (assoc (redirect (str (get-base-path ctx) "/user/oauth/facebook")) :flash message))))

             (GET "/linkedin" req
                  :query-params [{code :- s/Str nil}
                                 {state :- s/Str nil}
                                 {error :- s/Str nil}]
                  :current-user current-user
                  (let [r (l/linkedin-login ctx code state (:id current-user) error)
                        {:keys [status user-id message]} r
                        _ (if (= true (get-in req [:session :seen-terms])) (d/insert-user-terms ctx user-id "accepted"))
                        accepted-terms? (u/get-accepted-terms-by-id ctx user-id)]

                    (if (= status "success")

                      (if (not= (:status accepted-terms?) "accepted")

                        (if current-user
                          (redirect (str (get-base-path ctx) "/user/terms/" (:id current-user)))
                          ;(u/set-session ctx (found (str (get-base-path ctx) "/user/terms/" user-id)) user-id)
                          (u/finalize-login ctx (redirect (str (get-base-path ctx) "/user/terms/")) user-id (get-in req [:session :pending :user-badge-id]) false))

                        (if current-user
                          (redirect (str (get-base-path ctx) "/user/oauth/linkedin"))
                          ;(u/set-session ctx (found (str (get-base-path ctx) "/social/stream/")) user-id)
                          (u/finalize-login ctx (redirect (str (get-base-path ctx) "/social/stream")) user-id (get-in req [:session :pending :user-badge-id]) false)))

                      (if current-user
                        (assoc (redirect (str (get-base-path ctx) "/user/oauth/linkedin")) :flash message)
                        (assoc (redirect (str (get-base-path ctx) "/user/login")) :flash message)))))


             (GET "/linkedin/deauthorize" []
                  :return {:status (s/enum "success" "error")
                           (s/optional-key :message) s/Str}
                  :auth-rules access/signed
                  :current-user current-user
                  (ok (l/linkedin-deauthorize ctx (:id current-user)))))

    (context "/obpv1/oauth" []
             :tags ["oauth"]
             (GET "/status/:service" []
                  :return {:active s/Bool :no-password? s/Bool}
                  :summary "Get user's remote service login status"
                  :path-params [service :- (s/enum "facebook" "linkedin")]
                  :auth-rules access/signed
                  :current-user current-user
                  (ok (d/login-status ctx (:id current-user) service))))))
