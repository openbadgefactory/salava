(ns salava.oauth.routes
  (:require [clojure.pprint :refer [pprint]]
            [clojure.string :as string]
            [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [ring.util.response :refer [redirect]]
            [salava.core.layout :as layout]
            [schema.core :as s]
            [salava.core.util :refer [get-base-path get-full-path get-plugins]]
            [salava.oauth.db :as d]
            [salava.oauth.facebook :as f]
            [salava.oauth.google :as g]
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
             #_(layout/main ctx "/terms")


             (GET "/oauth2/authorize" req
                  :no-doc true
                  :summary ""
                  :query-params [response_type  :- s/Str
                                 client_id      :- s/Str
                                 redirect_uri   :- s/Str
                                 state          :- s/Str
                                 code_challenge :- s/Str
                                 code_challenge_method :- s/Str]
                  :current-user current-user
                  :flash-message flash-message
                  (let [client (get-in ctx [:config :oauth :client client_id])
                        redirect-to (str  "/user/oauth2/authorize?" (:query-string req))
                        login-redirect {:value (if (:id current-user) nil redirect-to) :max-age 600 :http-only true :path "/"}]
                    (if (and (= response_type "code") (= (:redirect_uri client) redirect_uri) (= code_challenge_method "S256"))
                      (-> (layout/main-response ctx current-user flash-message nil)
                          (assoc-in [:cookies "login_redirect"] login-redirect))
                      (bad-request {:message "400 Bad Request"}))))

             (POST "/oauth2/authorize" []
                   :no-doc true
                   :summary ""
                   :form-params [client_id      :- s/Str
                                 state          :- s/Str
                                 code_challenge :- s/Str]
                   :auth-rules access/signed
                   :current-user current-user
                  (if-let [client (get-in ctx [:config :oauth :client client_id])]
                     (-> (redirect (str (:redirect_uri client)
                                        "?code=" (d/authorization-code ctx client_id (:id current-user) code_challenge)
                                        "&host=" (get-full-path ctx)
                                        "&state=" state))
                         (assoc-in [:session :identity] nil))
                     (bad-request {:message "400 Bad Request"})))

             (POST "/oauth2/unauthorize" []
                   :no-doc true
                   :summary ""
                   :form-params [client_id :- s/Str]
                   :auth-rules access/signed
                   :current-user current-user
                   (ok (d/unauthorize-client ctx client_id (:id current-user))))

             (POST "/oauth2/token" []
                   :no-doc true
                   :summary ""
                   :form-params [grant_type   :- s/Str
                                 client_id    :- s/Str
                                 {redirect_uri  :- s/Str nil}
                                 {code          :- s/Str nil}
                                 {refresh_token :- s/Str nil}
                                 {code_verifier :- s/Str nil}]
                   (let [e400 (bad-request {:message "400 Bad Request"})
                         client (get-in ctx [:config :oauth :client client_id])]
                     (cond
                       (and (= grant_type "code")
                            (not (nil? code))
                            (= (:redirect_uri client) redirect_uri))
                       (if-let [out (d/new-access-token ctx client_id code code_verifier)] (ok out) e400)

                       (and (= grant_type "refresh_token")
                            (not (nil? refresh_token)))
                       (if-let [out (d/refresh-access-token ctx client_id (string/split refresh_token #"-" 2))] (ok out) e400)

                       :else e400)))

             (PUT "/oauth2/firebase_token" []
                  :no-doc true
                  :summary ""
                  :body-params [client_id :- s/Str
                                id    :- s/Int
                                token :- s/Str]
                  :auth-rules access/signed
                  :current-user current-user
                  (if (get-in ctx [:config :oauth :client client_id])
                    (ok (d/set-firebase-token ctx client_id (:id current-user) id token))
                    (bad-request {:message "400 Bad Request"})))
             )


   (context "/oauth" []
         :tags ["oauth"]
         (GET "/google" req
                 :no-doc true
                 :query-params [{code :- s/Str nil}
                                {state :- s/Str nil}
                                {error :- s/Str nil}]
                 :current-user current-user
                 (let [r (g/google-login ctx code (:id current-user) error)
                       {:keys [status user-id message new-user]} r
                       _ (if (= true (get-in req [:session :seen-terms])) (d/insert-user-terms ctx user-id "accepted"))
                       accepted-terms? (u/get-accepted-terms-by-id ctx user-id)
                       invitation (get-in req [:session :invitation] nil)]
                      (if (= status "success")

                        (if (and (not= accepted-terms? "accepted") (not= false accepted-terms?))
                         (if current-user
                           (redirect (str (get-base-path ctx) "/user/terms/" (:id current-user)))
                           (u/finalize-login ctx (assoc (redirect (str (get-base-path ctx) "/user/terms?service=google&new-user=" new-user)) :invitation invitation) user-id (get-in req [:session :pending :user-badge-id]) false))

                         (if current-user
                            (redirect (str (get-base-path ctx) "/user/oauth/google"))
                            (if new-user
                             (u/finalize-login ctx (assoc (redirect (str (get-base-path ctx) "/social?service=google&new-user=" new-user)) :invitation invitation) user-id (get-in req [:session :pending :user-badge-id]) false)
                             (u/finalize-login ctx (assoc (redirect (str (get-base-path ctx) "/social")) :invitation invitation) user-id (get-in req [:session :pending :user-badge-id]) false))))

                        (if current-user
                             (assoc (redirect (str (get-base-path ctx) "/user/oauth/google")) :flash message)
                             (assoc (redirect (str (get-base-path ctx) "/user/login")) :flash message)))))

         (GET "/google/deauthorize" []
              :no-doc true
              :query-params [code :- s/Str]
              :return {:status (s/enum "success" "error")
                       (s/optional-key :message) s/Str}
              :auth-rules access/signed
              :current-user current-user
              (let [{:keys [status message]} (g/google-deauthorize ctx code (:id current-user))]
               (if (= status "success")
                 (redirect (str (get-base-path ctx) "/user/oauth/google"))
                 (assoc (redirect (str (get-base-path ctx) "/user/oauth/google")) :flash message))))

         (GET "/facebook" req
              :no-doc true
              :query-params [{code :- s/Str nil}
                             {error :- s/Str nil}]
              :current-user current-user
              (let [{:keys [status user-id message role private new-user]} (f/facebook-login ctx code (:id current-user) error)
                    _ (if (= true (get-in req [:session :seen-terms])) (d/insert-user-terms ctx user-id "accepted"))
                    accepted-terms? (u/get-accepted-terms-by-id ctx user-id)
                    invitation (get-in req [:session :invitation] nil)]
                (if (= status "success")

                  (if (and (not= accepted-terms? "accepted") (not= false accepted-terms?))
                    (if current-user
                      (redirect (str (get-base-path ctx) "/user/terms/"))
                      (u/finalize-login ctx (assoc (redirect (str (get-base-path ctx) "/user/terms?service=facebook&new-user=" new-user)) :invitation invitation) user-id (get-in req [:session :pending :user-badge-id]) false))

                    (if current-user
                      (redirect (str (get-base-path ctx) "/user/oauth/facebook"))
                      (if new-user
                       (u/finalize-login ctx (assoc (redirect (str (get-base-path ctx) "/social?service=facebook&new-user=" new-user)) :invitation invitation) user-id (get-in req [:session :pending :user-badge-id]) false)
                       (u/finalize-login ctx (assoc (redirect (str (get-base-path ctx) "/social")) :invitation invitation) user-id (get-in req [:session :pending :user-badge-id]) false))))

                  (if current-user
                    (assoc (redirect (str (get-base-path ctx) "/user/oauth/facebook")) :flash message)
                    (assoc (redirect (str (get-base-path ctx) "/user/login")) :flash message)))))

         (GET "/facebook/deauthorize" []
              :no-doc true
              :query-params [code :- s/Str]
              :auth-rules access/signed
              :current-user current-user
              (let [{:keys [status message]} (f/facebook-deauthorize ctx code (:id current-user))]
                (if (= status "success")
                  (redirect (str (get-base-path ctx) "/user/oauth/facebook"))
                  (assoc (redirect (str (get-base-path ctx) "/user/oauth/facebook")) :flash message))))

         (GET "/linkedin" req
              :no-doc true
              :query-params [{code :- s/Str nil}
                             {state :- s/Str nil}
                             {error :- s/Str nil}]
              :current-user current-user
              (let [r (l/linkedin-login ctx code state (:id current-user) error)
                    {:keys [status user-id message new-user]} r
                    _ (if (= true (get-in req [:session :seen-terms])) (d/insert-user-terms ctx user-id "accepted"))
                    accepted-terms? (u/get-accepted-terms-by-id ctx user-id)
                    invitation (get-in req [:session :invitation] nil)]
                (if (= status "success")

                  (if (and (not= accepted-terms? "accepted") (not= false accepted-terms?))

                    (if current-user
                      (redirect (str (get-base-path ctx) "/user/terms/" (:id current-user)))
                      (u/finalize-login ctx (assoc (redirect (str (get-base-path ctx) "/user/terms?service=linkedin&new-user=" new-user)) :invitation invitation) user-id (get-in req [:session :pending :user-badge-id]) false))

                    (if current-user
                      (redirect (str (get-base-path ctx) "/user/oauth/linkedin"))
                      (if new-user
                       (u/finalize-login ctx (assoc (redirect (str (get-base-path ctx) "/social?service=linkedin&new-user=" new-user)) :invitation invitation) user-id (get-in req [:session :pending :user-badge-id]) false)
                       (u/finalize-login ctx (assoc (redirect (str (get-base-path ctx) "/social")) :invitation invitation) user-id (get-in req [:session :pending :user-badge-id]) false))))

                  (if current-user
                    (assoc (redirect (str (get-base-path ctx) "/user/oauth/linkedin")) :flash message)
                    (assoc (redirect (str (get-base-path ctx) "/user/login")) :flash message)))))


         (GET "/linkedin/deauthorize" []
              :no-doc true
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
                 :path-params [service :- (s/enum "facebook" "linkedin" "google")]
                 :auth-rules access/signed
                 :current-user current-user
                 (ok (d/login-status ctx (:id current-user) service))))))
