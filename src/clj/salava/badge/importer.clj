(ns salava.badge.importer
  (:require [clojure.tools.logging :as log]
            [clojure.pprint :refer [pprint]]
            [salava.badge.main :as b]
            [salava.badge.db :as db]
            [salava.badge.parse :as p]
            [salava.core.util :as u]
            [salava.core.http :as http]
            [salava.user.db :as user]
            [salava.core.helper :refer [dump]]))

(defonce import-cache (atom {}))

(defn- put-import-cache [key data]
  (swap! import-cache assoc key {:ttl (+ (u/now) 200) :data data})
  data)

(defn- get-import-cache [key]
  (when-let [data (get @import-cache key)]
    (swap! import-cache dissoc key)
    (if (> (:ttl data) (u/now))
      (:data data))))

;;;

(defn- badge-checksum [user-badge]
  (u/map-sha256 (:badge user-badge)))

(defn- get-remote-id [email base-url]
  (try
    (-> (str base-url "/convert/email")
        (http/http-post {:as :json :form-params {:email email}})
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
      (p/file->badge user (http/http-req {:method :get :url imageUrl :as :stream})))
    (catch Throwable ex
      (let [info (ex-data ex)]
        (log/error "failed to parse assertion from" (or hostedUrl imageUrl))
        (if info
          (log/error (pr-str (dissoc info :value)))
          (log/error (.toString ex)))))))

(defn- public-group-badges [user base-url remote-id {:keys [groupId name]}]
  (->> (http/json-get (str base-url "/" remote-id "/group/" groupId ".json"))
       :badges
       (map (partial assertion user))
       (remove nil?)
       (map #(with-meta % {:tag name :checksum (badge-checksum %)}))))

(defn- public-badges [user base-url remote-id]
  (->> (http/json-get (str base-url "/" remote-id "/groups.json"))
       :groups
       (pmap (partial public-group-badges user base-url remote-id))
       flatten))

(defn- all-public-badges [user base-url cached?]
  (let [cache-key [(:id user) base-url]]
    (if-let [cached (and cached? (get-import-cache cache-key))]
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

(defn user-emails-for-badge-export [ctx user-id]
  (let [emails (user/email-addresses ctx user-id)]
    (map (fn [e]
           {:email (clojure.string/lower-case (:email e))
            :backpack_id (:backpack_id e)})
         emails)))

(defn badges-to-import [ctx user-id]
  (try
    (let [base-url "https://backpack.openbadges.org/displayer" ;TODO support other sources
          user {:id user-id :emails (user/verified-email-addresses ctx user-id)}]
      {:badges (->> (all-public-badges user base-url false)
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
          badges-to-save (filter #(key-set (:checksum (meta %))) (all-public-badges user base-url true))
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
    (->> (db/save-user-badge! ctx
                              (-> {:id user-id :emails (user/verified-email-addresses ctx user-id)}
                                  (p/file->badge uploaded-file)
                                  (assoc :status "accepted")))
         (b/update-recipient-count-and-connect ctx user-id) ;;update recipient count and create badge connection
         )
    {:status "success" :message "badge/Badgeuploaded" :reason "badge/Badgeuploaded"}
    (catch Throwable ex
      (log/error "upload-badge: upload failed")
      (log/error (.toString ex))
      {:status "error" :message "badge/Errorwhileuploading" :reason (.getMessage ex)})))

(defn upload-badge-via-assertion [ctx assertion user]
  (let [user-id (:id user)]
    (log/info "assertion-badge-upload: got new upload from user id " user-id)
    (try
      (->> (db/save-user-badge! ctx
                                (-> {:id user-id :emails (user/verified-email-addresses ctx user-id)}
                                    (p/str->badge assertion)
                                    (assoc :status "accepted")))
           (b/update-recipient-count-and-connect ctx user-id) ;;update recipient count and create badge connection
           )
      {:status "success" :message "badge/Badgeuploaded" :reason "badge/Badgeuploaded"}
      (catch Throwable ex
        (log/error "assertion-badge-upload: upload failed")
        (log/error (.toString ex))
        {:status "error" :message "badge/Errorwhileuploading" :reason (.getMessage ex)}))))
