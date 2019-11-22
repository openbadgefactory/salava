(ns salava.oauth.routes
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [ring.util.response :refer [redirect]]
            [salava.core.layout :as layout]
            [schema.core :as s]
            [salava.core.util :refer [get-base-path get-plugins]]
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
             #_(layout/main ctx "/terms"))


   (context "/oauth" []
         :tags ["oauth"]
         (GET "/google" req
                 :query-params [{code :- s/Str nil}
                                {state :- s/Str nil}
                                {error :- s/Str nil}]
                 :current-user current-user
                 (let [r (g/google-login ctx code (:id current-user) error)
                       {:keys [status user-id message new-user]} r
                       _ (if (= true (get-in req [:session :seen-terms])) (d/insert-user-terms ctx user-id "accepted"))
                       accepted-terms? (u/get-accepted-terms-by-id ctx user-id)]
                      (if (= status "success")

                        (if (and (not= accepted-terms? "accepted") (not= false accepted-terms?))
                         (if current-user
                           (redirect (str (get-base-path ctx) "/user/terms/" (:id current-user)))
                           (u/finalize-login ctx (redirect (str (get-base-path ctx) "/user/terms?service=google&new-user=" new-user)) user-id (get-in req [:session :pending :user-badge-id]) false))

                         (if current-user
                            (redirect (str (get-base-path ctx) "/user/oauth/google"))
                            (if new-user
                             (u/finalize-login ctx (redirect (str (get-base-path ctx) "/social?service=google&new-user=" new-user)) user-id (get-in req [:session :pending :user-badge-id]) false)
                             (u/finalize-login ctx (redirect (str (get-base-path ctx) "/social")) user-id (get-in req [:session :pending :user-badge-id]) false))))

                        (if current-user
                             (assoc (redirect (str (get-base-path ctx) "/user/oauth/google")) :flash message)
                             (assoc (redirect (str (get-base-path ctx) "/user/login")) :flash message)))))

         (GET "/google/deauthorize" []
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
              :query-params [{code :- s/Str nil}
                             {error :- s/Str nil}]
              :current-user current-user
              (let [{:keys [status user-id message role private new-user]} (f/facebook-login ctx code (:id current-user) error)
                    _ (if (= true (get-in req [:session :seen-terms])) (d/insert-user-terms ctx user-id "accepted"))
                    accepted-terms? (u/get-accepted-terms-by-id ctx user-id)]
                (if (= status "success")

                  (if (and (not= accepted-terms? "accepted") (not= false accepted-terms?))
                    (if current-user
                      (redirect (str (get-base-path ctx) "/user/terms/"))
                      ;(u/set-session ctx (found (str (get-base-path ctx) "/user/terms/")) user-id)
                      (u/finalize-login ctx (redirect (str (get-base-path ctx) "/user/terms?service=facebook&new-user=" new-user)) user-id (get-in req [:session :pending :user-badge-id]) false))

                    (if current-user
                      (redirect (str (get-base-path ctx) "/user/oauth/facebook"))
                      ;(u/set-session ctx (redirect (str (get-base-path ctx) "/social/stream")) user-id)
                      (if new-user
                       (u/finalize-login ctx (redirect (str (get-base-path ctx) "/social?service=facebook&new-user=" new-user)) user-id (get-in req [:session :pending :user-badge-id]) false)
                       (u/finalize-login ctx (redirect (str (get-base-path ctx) "/social")) user-id (get-in req [:session :pending :user-badge-id]) false))
                      #_(u/finalize-login ctx (redirect (str (get-base-path ctx) "/social")) user-id (get-in req [:session :pending :user-badge-id]) false)))

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
                    {:keys [status user-id message new-user]} r
                    _ (if (= true (get-in req [:session :seen-terms])) (d/insert-user-terms ctx user-id "accepted"))
                    accepted-terms? (u/get-accepted-terms-by-id ctx user-id)]
                (if (= status "success")

                  (if (and (not= accepted-terms? "accepted") (not= false accepted-terms?))

                    (if current-user
                      (redirect (str (get-base-path ctx) "/user/terms/" (:id current-user)))
                      ;(u/set-session ctx (found (str (get-base-path ctx) "/user/terms/" user-id)) user-id)
                      (u/finalize-login ctx (redirect (str (get-base-path ctx) "/user/terms?service=linkedin&new-user=" new-user)) user-id (get-in req [:session :pending :user-badge-id]) false))

                    (if current-user
                      (redirect (str (get-base-path ctx) "/user/oauth/linkedin"))
                      ;(u/set-session ctx (found (str (get-base-path ctx) "/social/stream/")) user-id)
                      (if new-user
                       (u/finalize-login ctx (redirect (str (get-base-path ctx) "/social?service=linkedin&new-user=" new-user)) user-id (get-in req [:session :pending :user-badge-id]) false)
                       (u/finalize-login ctx (redirect (str (get-base-path ctx) "/social")) user-id (get-in req [:session :pending :user-badge-id]) false))))

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
                 :path-params [service :- (s/enum "facebook" "linkedin" "google")]
                 :auth-rules access/signed
                 :current-user current-user
                 (ok (d/login-status ctx (:id current-user) service))))))
