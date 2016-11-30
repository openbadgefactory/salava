(ns salava.badge.importer
  (:require [clojure.tools.logging :as log]
            [slingshot.slingshot :refer :all]
            [clj-http.client :as client]
            [salava.badge.main :as b]
            [salava.core.util :refer [map-sha256]]
            [salava.core.time :refer [unix-time]]
            [salava.core.helper :refer [dump]]
            [salava.core.i18n :refer [t]]
            [salava.user.db :as u]
            [salava.badge.assertion :as a]))

(def api-root-url "https://backpack.openbadges.org/displayer")

(defn- api-request
  ([method path] (api-request method path {}))
  ([method path post-params]
   (try+
     (:body
       (client/request
         {:method      method
          :url         (str api-root-url path)
          :as          :json
          :form-params post-params}))
     (catch [:status 404] {:keys [request-time headers body]}
       ; (if (= path "/convert/email")
       ;  (throw+ (t :badge/Backpacknotfound))
       ;  (throw+ (t :badge/Errorconnecting)))
     )
     (catch Object _
       (throw+ "badge/Errorconnecting")))))

(defn- get-badge-type [badge]
  (if (= (get-in badge [:assertion :verify :type]) "hosted")
    "hosted"
    (if (and (= (get-in badge [:assertion :verify :type]) "signed")
             (not-empty (get-in badge [:imageUrl])))
      "signed"
      (if (:hostedUrl badge)
        "hostedUrl"))))

(defn- get-assertion-and-key [type badge]
  "Get badge type and data.
  Return badge assertion and assertion key"
  (case type
    "hosted" [(:assertion badge) (:assertion badge)]
    "signed" [(a/fetch-signed-badge-assertion (:imageUrl badge)) (:imageUrl badge)]
    "hostedUrl" [(:hostedUrl badge) (:hostedUrl badge)]
    [{:error "badge/Invalidassertion"} nil]))

(defn- add-assertion-and-key [badge]
  (let [badge-type (get-badge-type badge)
        [assertion assertion-key] (get-assertion-and-key badge-type badge)
        old-assertion (:assertion badge)]
    (assoc badge :assertion (a/create-assertion assertion old-assertion)
                 :assertion_key assertion-key)))

(defn- collect-badges
  "Collect badges fetched from groups"
  [badge-colls]
  (let [badges (flatten badge-colls)]
    (map add-assertion-and-key badges)))

(defn- fetch-badges-by-group
  "Get badges from public group in Backpack"
  [email backpack-id group]
  (let [response (api-request :get (str "/" backpack-id "/group/" (:id group)))
        badges (:badges response)]
    (->> badges
         (map #(assoc % :_group_name (:name group)
                        :_email email
                        :_status "accepted")))))

(defn- fetch-badges-from-groups
  "Fetch and collect users badges in public groups."
  [backpack]
  (if (and (:email backpack) (:userId backpack))
    (let [response (api-request :get (str "/" (:userId backpack) "/groups"))
          groups (map #(hash-map :id (:groupId %) :name (:name %)) (:groups response))]
      (if (pos? (count groups))
        (collect-badges (map #(fetch-badges-by-group (:email backpack) (:userId backpack) %) groups))))))

(defn- fetch-backpack-uid
  "Get Mozilla uid by email address"
  [ctx user-id email]
  (let [backpack-id (:userId (api-request :post "/convert/email" {:email email}))]
    (when backpack-id
      (u/set-email-backpack-id ctx user-id email backpack-id)
      {:email email :userId backpack-id})))

(defn- fetch-all-user-badges [ctx user-id backpack-emails]
  (if (empty? backpack-emails)
    (throw+ "badge/Noemails"))
  (reduce #(concat %1 (fetch-badges-from-groups %2)) [] (map #(fetch-backpack-uid ctx user-id %) backpack-emails)))

(defn- badge-to-import [ctx user-id badge]
  (let [expires (re-find #"\d+" (str (get-in badge [:assertion :expires])))
        expires-int (if expires (Integer. expires))
        expired? (and expires-int (not= expires-int 0) (< expires-int (unix-time)))
        exists? (b/user-owns-badge? ctx (:assertion badge) user-id)
        error (get-in badge [:assertion :error])]
    {:status      (if (or expired? exists? error) "invalid" "ok")
     :message     (cond
                    exists? "badge/Alreadyowned"
                    expired? "badge/Badgeisexpired"
                    error "badge/Invalidbadge"
                    :else "badge/Savethisbadge")
     :error       error
     :name        (get-in badge [:assertion :badge :name])
     :description (get-in badge [:assertion :badge :description])
     :image_file  (get-in badge [:assertion :badge :image])
     :issuer_content_name (get-in badge [:assertion :badge :issuer :name])
     :issuer_content_url (get-in badge [:assertion :badge :issuer :url])
     :id          (if exists? (b/user-owns-badge-id ctx (:assertion badge) user-id))
     :key         (map-sha256 (get-in badge [:assertion_key]))}))

(defn- save-badge-data! [ctx emails user-id badge]
  (try
    (let [badge-id (b/save-badge-from-assertion! ctx badge user-id emails)
          tags (list (:_group_name badge))]
      (if (and tags badge-id)
        (b/save-badge-tags! ctx tags badge-id))
      {:id badge-id})
    (catch Exception ex
      {:id nil})))

(defn user-backpack-emails
  "Get list of user's email addresses"
  [ctx user-id]
  (let [emails (u/verified-email-addresses ctx user-id)]
    (->> emails
         (map #(fetch-backpack-uid ctx user-id %))
         (filter :email)
         (map :email))))


(defn badges-to-import [ctx user-id]
  (try+
    (let [emails (u/verified-email-addresses ctx user-id)
          badges (fetch-all-user-badges ctx user-id emails)]
      {:status "success"
       :badges (sort-by :message #(compare %2 %1) (map #(badge-to-import ctx user-id %) badges))
       :error nil})
    (catch Object _
      {:status "error"
       :badges []
       :error _})))


(defn do-import [ctx user-id keys]
  (try+
    (let [backpack-emails (u/verified-email-addresses ctx user-id)
          all-badges (fetch-all-user-badges ctx user-id backpack-emails)
          badges-with-keys (map #(assoc % :key
                                          (map-sha256 (get-in % [:assertion_key])))
                                all-badges)
          badges-to-save (filter (fn [b]
                                   (some #(= (:key b) %) keys)) badges-with-keys)
          saved-badges (for [b badges-to-save]
                         (save-badge-data! ctx backpack-emails user-id b))]
      {:status      "success"
       :message     "badge/Badgessaved"
       :saved-count (->> saved-badges
                         (filter #(:id %))
                         count)
       :error-count (->> saved-badges
                         (filter #(nil?(:id %)))
                         count)})
    (catch Object _
      {:status "error" :message _})))

;;;

(defn upload-badge [ctx uploaded-file user-id]
  (try
    (log/info "upload-badge: got new upload from user id" user-id)
    (b/save-badge-from-assertion! ctx
                                  {:assertion (a/baked-image uploaded-file) :_status "accepted"}
                                  user-id
                                  (u/verified-email-addresses ctx user-id))
    {:status "success" :message "badge/Badgeuploaded" :reason "badge/Badgeuploaded"}
    (catch Throwable ex
      (log/error "upload-badge: upload failed")
      (log/error (.toString ex))
      {:status "error" :message "badge/Errorwhileuploading" :reason (.getMessage ex)})))
