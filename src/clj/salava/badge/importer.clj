(ns salava.badge.importer
  (:require [clojure.tools.logging :as log]
            [clj-http.client :as client]
            [salava.badge.main :as b]
            [salava.badge.db :as db]
            [salava.badge.parse :as p]
            [salava.core.util :as u]
            [salava.user.db :as user]))

(defonce import-cache (atom {}))

(defn- put-import-cache [key data]
  (swap! import-cache assoc key {:ttl (+ (u/now) 1600) :data data})
  data)

(defn- get-import-cache [key]
  (when-let [data (get @import-cache key)]
    #_(swap! import-cache dissoc key)
    (if (> (:ttl data) (u/now))
      (:data data))))

;;;

(defn- badge-checksum [user-badge]
  (u/map-sha256 (:badge user-badge)))

(defn- get-remote-id [email base-url]
  (try
    (-> (str base-url "/convert/email")
        (client/post {:as :json :form-params {:email email}})
        (get-in [:body :userId] 0))
    (catch Exception _ 0)))

(defn- remote-ids [user base-url]
  (->> (:emails user)
      (map #(get-remote-id % base-url))
      (filter pos?)))

(defn- assertion [user {:keys [hostedUrl imageUrl]}]
  (try
    (if hostedUrl
      (p/str->badge  user hostedUrl)
      (p/file->badge user (client/get imageUrl {:as :stream})))
    (catch Throwable ex
      (let [info (ex-data ex)]
        (log/error "failed to parse assertion from" (or hostedUrl imageUrl))
        (if info
          (log/error (pr-str (dissoc info :value)))
          (log/error (.toString ex)))))))

(defn- public-group-badges [user base-url remote-id {:keys [groupId name]}]
  (->> (u/json-get (str base-url "/" remote-id "/group/" groupId ".json"))
       :badges
       (map (partial assertion user))
       (remove nil?)
       (map #(with-meta % {:tag name :checksum (badge-checksum %)}))))

(defn- public-badges [user base-url remote-id]
  (->> (u/json-get (str base-url "/" remote-id "/groups.json"))
       :groups
       (pmap (partial public-group-badges user base-url remote-id))
       flatten))

(defn- all-public-badges [user base-url]
  (let [cache-key [(:id user) base-url]]
    (if-let [cached (get-import-cache cache-key)]
      cached
      (->> (remote-ids user base-url)
           (mapcat (partial public-badges user base-url))
           (put-import-cache cache-key)))))

(defn- import-info [ctx user badge]
  (let [previous-id (b/user-owns-badge-id ctx (assoc badge :user_id (:id user)))
        expired? (if (nil? (:expires_on badge)) false (< (:expires_on badge) (u/now)))
        exists? (not (nil? previous-id))
        error (:error (meta badge))
        content (first (get-in badge [:badge :content])) ;TODO check default language
        issuer (first (get-in badge [:badge :issuer]))  ;TODO check default language
        ] 
    {:status              (if (or expired? exists? error) "invalid" "ok")
     :message             (cond
                            exists?  "badge/Alreadyowned"
                            expired? "badge/Badgeisexpired"
                            error    "badge/Invalidbadge"
                            :else    "badge/Savethisbadge")
     :error               error
     :name                (:name content)
     :description         (:description content) 
     :image_file          (:image_file content) 
     :issuer_content_name (:name issuer) 
     :issuer_content_url  (:url issuer) 
     :previous-id         previous-id
     :import-key          (:checksum (meta badge))}))

(defn user-backpack-emails
  "Get list of user's email addresses"
  [ctx user-id]
  (let [base-url "https://backpack.openbadges.org/displayer"] ;TODO support other sources
    (->> (user/verified-email-addresses ctx user-id)
         (filter #(pos? (get-remote-id % base-url))))))

(defn badges-to-import [ctx user-id]
  (try
    (let [base-url "https://backpack.openbadges.org/displayer" ;TODO support other sources
          user {:id user-id :emails (user/verified-email-addresses ctx user-id)}]
      {:badges (->> (all-public-badges user base-url)
                    (map (partial import-info ctx user))
                    (sort-by :message #(compare %2 %1)))
       :status "success"
       :error nil})
    (catch Throwable ex
      (log/error "badges-to-import: failed to fetch badges")
      (log/error (.toString ex))
      #_(doseq [line (.getStackTrace ex)]
        (log/error (str line)))
      {:badges []
       :status "error"
       :error ex})))

(defn do-import [ctx user-id keys]
  (try
    (let [base-url "https://backpack.openbadges.org/displayer" ;TODO support other sources
          user {:id user-id :emails (user/verified-email-addresses ctx user-id)}
          key-set (set keys)
          badges-to-save (filter #(key-set (:checksum (meta %))) (all-public-badges user base-url))
          badge-ids (remove nil? (map #(db/save-user-badge! ctx (assoc % :status "accepted")) badges-to-save))]
      {:status      "success"
       :message     "badge/Badgessaved"
       :saved-count (count badge-ids)
       :error-count (- (count badges-to-save) (count badge-ids))})
    (catch Throwable ex
      (log/error "do-import failed to save badges")
      (log/error (.toString ex))
      {:status "error" :message (.getMessage ex)})))


(defn upload-badge [ctx uploaded-file user-id]
  (log/info "upload-badge: got new upload from user id" user-id)
  (try
    (db/save-user-badge! ctx
                         (-> {:id user-id :emails (user/verified-email-addresses ctx user-id)}
                             (p/file->badge uploaded-file)
                             (assoc :status "accepted")))
    {:status "success" :message "badge/Badgeuploaded" :reason "badge/Badgeuploaded"}
    (catch Throwable ex
      (log/error "upload-badge: upload failed")
      (log/error (.toString ex))
      {:status "error" :message "badge/Errorwhileuploading" :reason (.getMessage ex)})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


#_(defn- api-request
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

#_(defn- get-badge-type [badge]
  (if (= (get-in badge [:assertion :verify :type]) "hosted")
    "hosted"
    (if (and (= (get-in badge [:assertion :verify :type]) "signed")
             (not-empty (get-in badge [:imageUrl])))
      "signed"
      (if (:hostedUrl badge)
        "hostedUrl"))))

#_(defn- get-assertion-and-key [type badge]
  "Get badge type and data.
  Return badge assertion and assertion key"
  (case type
    "hosted" [(:assertion badge) (:assertion badge)]
    "signed" [(a/fetch-signed-badge-assertion (:imageUrl badge)) (:imageUrl badge)]
    "hostedUrl" [(:hostedUrl badge) (:hostedUrl badge)]
    [{:error "badge/Invalidassertion"} nil]))

#_(defn- add-assertion-and-key [badge]
  (try
    (let [badge-type (get-badge-type badge)
          [assertion assertion-key] (get-assertion-and-key badge-type badge)
          old-assertion (:assertion badge)]
      (assoc badge :assertion (a/create-assertion assertion old-assertion)
             :assertion_key assertion-key))
    (catch Throwable ex
      (log/error "add-assertion-and-key: failed to get assertion")
      (log/error (.toString ex)))))

#_(defn- collect-badges
  "Collect badges fetched from groups"
  [badge-colls]
  (let [badges (flatten badge-colls)]
    (filter #(not (nil? %)) (pmap add-assertion-and-key badges))))

#_(defn- fetch-badges-by-group
  "Get badges from public group in Backpack"
  [email backpack-id group]
  (let [response (api-request :get (str "/" backpack-id "/group/" (:id group)))
        badges (:badges response)]
    (->> badges
         (map #(assoc % :_group_name (:name group)
                        :_email email
                        :_status "accepted")))))

#_(defn- fetch-badges-from-groups
  "Fetch and collect users badges in public groups."
  [backpack]
  (if (and (:email backpack) (:userId backpack))
    (let [response (api-request :get (str "/" (:userId backpack) "/groups"))
          groups (map #(hash-map :id (:groupId %) :name (:name %)) (:groups response))]
      (if (pos? (count groups))
        (collect-badges (map #(fetch-badges-by-group (:email backpack) (:userId backpack) %) groups))))))

#_(defn- fetch-backpack-uid
  "Get Mozilla uid by email address"
  [ctx user-id email]
  (let [backpack-id (:userId (api-request :post "/convert/email" {:email email}))]
    (when backpack-id
      (u/set-email-backpack-id ctx user-id email backpack-id)
      {:email email :userId backpack-id})))

#_(defn- fetch-all-user-badges [ctx user-id backpack-emails]
  (if (empty? backpack-emails)
    (throw+ "badge/Noemails"))
  (reduce #(concat %1 (fetch-badges-from-groups %2)) [] (map #(fetch-backpack-uid ctx user-id %) backpack-emails)))

#_(defn- badge-to-import [ctx user-id badge]
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

#_(defn- save-badge-data! [ctx emails user-id badge]
  (try
    (let [badge-id (b/save-badge-from-assertion! ctx badge user-id emails)
          tags (list (:_group_name badge))]
      (if (and tags badge-id)
        (b/save-badge-tags! ctx tags badge-id))
      {:id badge-id})
    (catch Exception ex
      (log/error "save-badge-data!: failed to save badge")
      (log/error (.toString ex))
      {:id nil})))



#_(defn badges-to-import [ctx user-id]
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


#_(defn do-import [ctx user-id keys]
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

#_(defn upload-badge [ctx uploaded-file user-id]
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
