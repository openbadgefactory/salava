(ns salava.badge.importer
  (:require [slingshot.slingshot :refer :all]
            [clj-http.client :as client]
            [salava.badge.main :as b]
            [salava.core.util :refer [map-sha256]]
            [salava.core.time :refer [unix-time]]
            [salava.core.helper :refer [dump]]
            [salava.core.i18n :refer [t]]
            [salava.user.db :as u]
            [salava.badge.assertion :as a]
            [salava.badge.png :as p]
            [salava.badge.svg :as s]))

(def api-root-url "https://backpack.openbadges.org/displayer")


(defn api-request
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
       (throw+ (t :badge/Errorconnecting))))))


(defn get-badge-type [badge]
  (if (= (get-in badge [:assertion :verify :type]) "hosted")
    "hosted"
    (if (and (= (get-in badge [:assertion :verify :type]) "signed")
             (not-empty (get-in badge [:imageUrl])))
      "signed"
      (if (:hostedUrl badge)
        "hostedUrl"))))


(defn get-assertion-and-key [type badge]
  "Get badge type and data.
  Return badge assertion and assertion key"
  (case type
    "hosted" [(:assertion badge) (:assertion badge)]
    "signed" [(a/fetch-signed-badge-assertion (:imageUrl badge)) (:imageUrl badge)]
    "hostedUrl" [(:hostedUrl badge) (:hostedUrl badge)]
    [{:error (t :badge/Invalidassertion)} nil]))


(defn add-assertion-and-key [badge]
  (let [badge-type (get-badge-type badge)
        [assertion assertion-key] (get-assertion-and-key badge-type badge)
        old-assertion (:assertion badge)]
    (assoc badge :assertion (a/create-assertion assertion old-assertion)
                 :assertion_key assertion-key)))


(defn collect-badges
  "Collect badges fetched from groups"
  [badge-colls]
  (let [badges (flatten badge-colls)]
    (map add-assertion-and-key badges)))


(defn fetch-badges-by-group
  "Get badges from public group in Backpack"
  [email backpack-id group]
  (let [response (api-request :get (str "/" backpack-id "/group/" (:id group)))
        badges (:badges response)]
    (->> badges
         (map #(assoc % :_group_name (:name group)
                        :_email email
                        :_status "accepted")))))


(defn fetch-badges-from-groups
  "Fetch and collect users badges in public groups."
  [email backpack-id]
  (let [response (api-request :get (str "/" backpack-id "/groups"))
        groups (map #(hash-map :id (:groupId %)
                               :name (:name %))
                    (:groups response))]
    (if (pos? (count groups))
      (collect-badges (map #(fetch-badges-by-group email backpack-id %) groups)))))


(defn fetch-badges-from-backpack
  "Get badges by backpack email address"
  [email]
  (let [response (api-request :post "/convert/email" {:email email})]
    (fetch-badges-from-groups email (:userId response))))


(defn fetch-all-user-badges [backpack-emails]
  (if (empty? backpack-emails)
    (throw+ (t :badge/Noemails)))
  (loop [badges []
         emails backpack-emails]
    (if (empty? emails)
      badges
      (recur (concat badges (fetch-badges-from-backpack (first emails)))
             (rest emails)))))


(defn badge-to-import [ctx user-id badge]
  (let [expires (re-find #"\d+" (str (get-in badge [:assertion :expires])))
        expires-int (if expires (Integer. expires))
        expired? (and expires-int (not= expires-int 0) (< expires-int (unix-time)))
        exists? (b/user-owns-badge? ctx (:assertion badge) user-id)
        error (get-in badge [:assertion :error])]
    {:status      (if (or expired? exists? error) "invalid" "ok")
     :message     (cond
                    exists? (t :badge/Alreadyowned)
                    expired? (t :badge/Badgeisexpired)
                    error error
                    :else (t :badge/Savethisbadge))
     :name        (get-in badge [:assertion :badge :name])
     :description (get-in badge [:assertion :badge :description])
     :image_file  (get-in badge [:assertion :badge :image])
     :key         (map-sha256 (get-in badge [:assertion_key]))}))


(defn badges-to-import [ctx user-id]
  (try+
    (let [backpack-emails (u/verified-email-addresses ctx user-id)
          badges (fetch-all-user-badges backpack-emails)]
      {:status "success"
       :badges (map #(badge-to-import ctx user-id %) badges)
       :error nil})
    (catch Object _
      {:status "error"
       :badges []
       :error _})))

(defn save-badge-data! [ctx emails user-id badge]
  (try+
    (let [badge-id (b/save-badge-from-assertion! ctx badge user-id emails)
          tags (list (:_group_name badge))]
      (if (and tags badge-id)
        (b/save-badge-tags! ctx tags badge-id))
      {:id badge-id})
    (catch Object _
      {:id nil})))

(defn do-import [ctx user-id keys]
  (try+
    (let [backpack-emails (u/verified-email-addresses ctx user-id)
          all-badges (fetch-all-user-badges backpack-emails)
          badges-with-keys (map #(assoc % :key
                                          (map-sha256 (get-in % [:assertion_key])))
                                all-badges)
          badges-to-save (filter (fn [b]
                                   (some #(= (:key b) %) keys)) badges-with-keys)
          saved-badges (for [b badges-to-save]
                         (save-badge-data! ctx backpack-emails user-id b))]
      {:status      "success"
       :message     (t :badge/Badgessaved)
       :saved-count (->> saved-badges
                         (filter #(:id %))
                         count)
       :error-count (->> saved-badges
                         (filter #(nil?(:id %)))
                         count)})
    (catch Object _
      {:status "error" :message _})))

(defn upload-badge [ctx uploaded-file user-id]
  (try+
    (if-not (some #(= (:content-type uploaded-file) %) ["image/png" "image/svg+xml"])
      (throw+ (t :badge/Invalidfiletype)))
    (let [content-type (:content-type uploaded-file)
          assertion-url (if (= content-type "image/png")
                          (p/get-assertion-from-png (:tempfile uploaded-file))
                          (s/get-assertion-from-svg (:tempfile uploaded-file)))
          assertion (a/create-assertion assertion-url {})
          emails (u/verified-email-addresses ctx user-id)
          data {:assertion assertion}
          badge-id (b/save-badge-from-assertion! ctx data user-id emails)]
      {:status "success" :message (t :badge/Badgeuploaded) :reason (t :badge/Badgeuploaded)})
    (catch Object _
      {:status "error" :message (t :badge/Errorwhileuploading) :reason _})))
