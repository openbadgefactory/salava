(ns salava.badge.parse
  (:require [buddy.sign.jws :as jws]
            [buddy.core.keys :as keys]
            [clojure.tools.logging :as log]
            [clojure.string :as string]
            [clojure.data.json :as json]
            [clojure.xml :as xml]
            [net.cgrand.enlive-html :as html]
            [salava.core.util :as u]
            [salava.core.http :as http]
            [schema.core :as s])
  (:import (ar.com.hjg.pngj PngReader)
           (java.io StringReader)))

(s/defschema BadgeContent {:id    s/Str
                           :language_code s/Str
                           :name  s/Str
                           :image_file  s/Str
                           :description s/Str
                           :alignment [(s/maybe {:name s/Str
                                                 :url  s/Str
                                                 :description s/Str})]
                           :tags      [(s/maybe s/Str)]})

(s/defschema IssuerContent {:id   s/Str
                            :language_code s/Str
                            :name s/Str
                            :url  s/Str
                            :description (s/maybe s/Str)
                            :image_file (s/maybe s/Str)
                            :email (s/maybe s/Str)
                            :revocation_list_url (s/maybe s/Str)})

(s/defschema CreatorContent (-> IssuerContent
                                (dissoc :revocation_list_url)
                                (assoc  :json_url s/Str)))

(s/defschema CriteriaContent {:id s/Str
                              :language_code s/Str
                              :url s/Str
                              :markdown_text (s/maybe s/Str)})

(s/defschema Badge {:id s/Str
                    :remote_url (s/maybe s/Str)
                    :remote_id (s/maybe s/Str)
                    :remote_issuer_id (s/maybe s/Str)
                    :issuer_verified (s/enum 0 1)
                    :default_language_code s/Str
                    :content [BadgeContent]
                    :criteria [CriteriaContent]
                    :issuer [IssuerContent]
                    :creator [(s/maybe CreatorContent)]
                    :published (s/enum 0 1)
                    :last_received s/Int
                    :recipient_count s/Int})

(s/defschema Evidence {:id (s/maybe s/Int)
                       :user_badge_id (s/maybe s/Int)
                       :url (s/maybe s/Str)
                       :narrative (s/maybe s/Str)
                       :name (s/maybe s/Str)
                       :description (s/maybe s/Str)
                       :genre (s/maybe s/Str)
                       :audience (s/maybe s/Str)
                       :ctime s/Int
                       :mtime s/Int})


(s/defschema UserBadge {:id (s/maybe s/Int)
                        :badge Badge
                        :user_id s/Int
                        :email   s/Str
                        :assertion_url  (s/maybe s/Str)
                        :assertion_jws  (s/maybe s/Str)
                        :assertion_json s/Str
                        :issued_on  s/Int
                        :expires_on (s/maybe s/Int)
                        :evidence [(s/maybe Evidence)]
                        :status     (s/enum "pending" "accepted" "declined")
                        :visibility (s/enum "private" "internal" "public")
                        :show_recipient_name (s/enum 0 1)
                        :rating (s/maybe (s/enum 0 1))
                        :ctime s/Int
                        :mtime s/Int
                        :deleted (s/enum 0 1)
                        :revoked (s/enum 0 1)
                        :issuer_verified (s/enum 0 1)
                        :show_evidence   (s/enum 0 1)
                        :last_checked (s/maybe s/Int)
                        :old_id (s/maybe s/Int)})

(def valid-badge (s/validator UserBadge))

;;;


(defn- domain [url]
  (last (re-find #"^https?://([^/]+)" (str url))))

(defn- badge-image [input]
  (let [image (if (map? input) (get-in input [:badge :image] "") input)]
    (if-let [match (re-find #"(?s)^data.+,(.+)" image)]
      (u/base64->bytes (last match))
      (try
        (http/http-get image)
        (catch Throwable ex
          (if (contains? (meta input) :image)
            (:image (meta input))
            (throw (Exception. "image missing"))))))))

(defn- capitalize-name [email]
  (when-let [m (re-matches #"([^\.@]+)\.([^\.@]+)@(.+)" (string/lower-case email))]
    (str (-> m rest first string/capitalize) "." (-> m rest second string/capitalize) "@" (last m))))

(defn- email-variations [emails]
  (remove nil? (mapcat #(list %
                 (string/upper-case %)
                 (string/lower-case %)
                 (string/capitalize %)
                 (capitalize-name %)) emails)))

;;;

(defn assertion-version [_ asr]
  (cond
    (and (contains? asr :id)
         (= ((keyword "@context") asr) "https://w3id.org/openbadges/v2")) :v2.0
    (map? (:badge asr)) :v0.5.0
    (and (contains? asr :id) (contains? asr :type)) :v1.1
    :else :v1.0))

;; See https://github.com/mozilla/openbadges-backpack/wiki/Assertion-Specification-Changes
(defmulti badge-content assertion-version)

(defmethod badge-content :v0.5.0 [initial assertion]
  (let [now (u/now)
        q-url (fn [url]
                (if (re-find #"^/" (str url))
                  (str (get-in assertion [:badge :issuer :origin]) url) url))
        badge    (:badge assertion)
        issuer   (:issuer badge)
        criteria (http/http-get (q-url (:criteria badge)))
        evidence_url (q-url (:evidence assertion))]

    (assoc initial
           :badge {:id ""
                   :remote_url nil
                   :remote_id nil
                   :remote_issuer_id nil
                   :issuer_verified 0
                   :default_language_code ""
                   :content [{:id ""
                              :language_code ""
                              :name (:name badge)
                              :image_file (q-url (:image badge))
                              :description (:description badge)
                              :alignment []
                              :tags (get badge :tags [])}]
                   :criteria [{:id ""
                               :language_code ""
                               :url (q-url (:criteria badge))
                               :markdown_text (http/alternate-get "text/x-markdown" criteria)}]
                   :issuer [{:id ""
                             :language_code ""
                             :name (str (:name issuer) ": " (:org issuer))
                             :description (:description issuer)
                             :url (q-url (:origin issuer))
                             :email (:contact issuer)
                             :image_file nil
                             :revocation_list_url nil}]
                   :creator nil
                   :published 0
                   :last_received 0
                   :recipient_count 0}
           :issued_on  (u/str->epoch (or (:issued_on assertion) (:issuedOn assertion)))
           :expires_on (u/str->epoch (:expires assertion))
           :evidence [(when-not (nil? evidence_url)
                       {:id nil
                         :user_badge_id nil
                         :url evidence_url
                         :narrative nil
                         :name nil
                         :description nil
                         :genre nil
                         :audience nil
                         :ctime now
                         :mtime now})])))


; old v0.5.0 badge content
#_{:badge_url    nil
  :issuer_url   nil
  :criteria_url (q-url (:criteria badge))
  :creator_url  nil
  :badge_content {}
  :issuer_content {:id ""
                   :name (str (:name issuer) ": " (:org issuer))
                   :description (:description issuer)
                   :url (q-url (:origin issuer))
                   :email (:contact issuer)
                   :image_file nil
                   :revocation_list_url nil}
  :criteria_content {:id ""
                     :html_content criteria
                     :markdown_text (http/alternate-get "text/x-markdown" criteria)}
  :creator_content  nil
  :issued_on  (u/str->epoch (or (:issued_on assertion) (:issuedOn assertion)))
  :expires_on (u/str->epoch (:expires assertion))
  :evidence_url (q-url (:evidence assertion))}

;;

(defmethod badge-content :v2.0 [initial assertion]
  (let [parser (fn [badge]
                 (let [language (get badge (keyword "@language") "")
                       issuer (if (map? (:issuer badge)) (:issuer badge) (http/json-get (:issuer badge)))
                       criteria-url  (if (map? (:criteria badge)) (get-in badge [:criteria :id]) (:criteria badge))
                       criteria-text (if (map? (:criteria badge))
                                       (get-in badge [:criteria :narrative]
                                               (http/alternate-get "text/x-markdown" criteria-url))
                                       (http/alternate-get "text/x-markdown" (:criteria badge)))
                       creator-url (get-in badge [:extensions:OriginalCreator :url])]

                   {:content  [{:id ""
                                :language_code language
                                :name (:name badge)
                                :image_file (:image badge)
                                :description (:description badge)
                                :alignment (get badge :alignment [])
                                :tags (get badge :tags [])}]
                    :criteria [{:id ""
                                :language_code language
                                :url (str criteria-url)
                                :markdown_text criteria-text}]
                    :issuer   [{:id ""
                                :language_code language
                                :name (str (:name issuer) ": " (:org issuer))
                                :description (:description issuer)
                                :url (:url issuer)
                                :email (:contact issuer)
                                :image_file nil
                                :revocation_list_url nil}]
                    :creator (when creator-url
                               (let [data (http/json-get creator-url)]
                                 [{:id ""
                                  :language_code language
                                  :name        (:name data)
                                  :image_file  (:image data)
                                  :description (:description data)
                                  :url   (:url data)
                                  :email (:email data)
                                  :json_url creator-url}]))}))
        now (u/now)
        evidence (cond
                   ;; inline narrative
                   (string? (:narrative assertion)) [{:id nil
                                                      :user_badge_id nil
                                                      :url nil
                                                      :narrative (:narrative assertion)
                                                      :name nil
                                                      :description nil
                                                      :genre nil
                                                      :audience nil
                                                      :ctime now
                                                      :mtime now}]
                   ;; url
                   (string? (:evidence assertion)) [{:id nil
                                                     :user_badge_id nil
                                                     :url (:evidence assertion)
                                                     :narrative nil
                                                     :name nil
                                                     :description nil
                                                     :genre nil
                                                     :audience nil
                                                     :ctime now
                                                     :mtime now}]
                   ;; inline evidence object
                   (map? (:evidence assertion)) [{:id nil
                                                  :user_badge_id nil
                                                  :url (get-in assertion [:evidence :id])
                                                  :narrative (get-in assertion [:evidence :narrative])
                                                  :name (get-in assertion [:evidence :name])
                                                  :description (get-in assertion [:evidence :description])
                                                  :genre (get-in assertion [:evidence :genre])
                                                  :audience (get-in assertion [:evidence :audience])
                                                  :ctime now
                                                  :mtime now}]
                   ;; list of evidences
                   (coll? (:evidence assertion)) (mapv #({:id nil
                                                            :user_badge_id nil
                                                            :url (:id %)
                                                            :narrative (:narrative %)
                                                            :name (:name %)
                                                            :description (:description %)
                                                            :genre (:genre %)
                                                            :audience (:audience %)
                                                            :ctime now
                                                            :mtime now}) (:evidence assertion))
                   :else [])
        badge  (if (map? (:badge assertion)) (:badge assertion) (http/json-get (:badge assertion)))
        related (->> (get badge :related [])
                     (map #(http/json-get (:id %)))
                     (remove #(nil? ((keyword "@language") %))))
        default-language (get badge (keyword "@language") "")]
    (assoc initial
           :badge (merge {:id ""
                          :remote_url nil
                          :remote_id nil
                          :remote_issuer_id nil
                          :issuer_verified 0
                          :default_language_code default-language
                          :published 0
                          :last_received 0
                          :recipient_count 0}
                          (apply merge-with (cons concat (map parser (cons badge related)))))
           :issued_on  (u/str->epoch (:issuedOn assertion))
           :expires_on (u/str->epoch (:expires assertion))
           :evidence evidence)))

;;;

(defmethod badge-content :default [initial assertion]
  (let [now (u/now)
        badge  (http/json-get (:badge assertion))
        issuer (http/json-get (:issuer badge))
        criteria (http/http-get (:criteria badge))
        evidence_url (:evidence assertion)
        creator-url (get-in badge [:extensions:OriginalCreator :url])
        creator (if-not (nil? creator-url)
                  (let [data (http/json-get creator-url)]
                    [{:id ""
                      :language_code ""
                      :name        (:name data)
                      :image_file  (:image data)
                      :description (:description data)
                      :url   (:url data)
                      :email (:email data)
                      :json_url creator-url}]))]

    (assoc initial
           :badge {:id ""
                   :remote_url nil
                   :remote_id nil
                   :remote_issuer_id nil
                   :issuer_verified 0
                   :default_language_code ""
                   :content [{:id ""
                              :language_code ""
                              :name (:name badge)
                              :image_file (:image badge)
                              :description (:description badge)
                              :alignment (get badge :alignment [])
                              :tags (get badge :tags [])}]
                   :criteria [{:id ""
                               :language_code ""
                               :url (:criteria badge)
                               :markdown_text (http/alternate-get "text/x-markdown" criteria)}]
                   :issuer [{:id ""
                             :language_code ""
                             :name (str (:name issuer) ": " (:org issuer))
                             :description (:description issuer)
                             :url (:url issuer)
                             :email (:contact issuer)
                             :image_file nil
                             :revocation_list_url nil}]
                   :creator creator
                   :published 0
                   :last_received 0
                   :recipient_count 0}
           :issued_on  (u/str->epoch (:issuedOn assertion))
           :expires_on (u/str->epoch (:expires assertion))
           :evidence [(when-not (nil? evidence_url)
                        {:id nil
                         :user_badge_id nil
                         :url evidence_url
                         :narrative nil
                         :name nil
                         :description nil
                         :genre nil
                         :audience nil
                         :ctime now
                         :mtime now})])))

; old v1.0/v1.1 badge content
#_{:badge_url    (:badge assertion)
            :issuer_url   (:issuer badge)
            :criteria_url (:criteria badge)
            :creator_url  creator-url
            :badge_content {:id ""
                            :name (:name badge)
                            :image_file (:image badge)
                            :description (:description badge)
                            :alignment (get badge :alignment [])
                            :tags (get badge :tags [])}
            :issuer_content {:id ""
                             :name (:name issuer)
                             :description (:description issuer)
                             :url (:url issuer)
                             :email (:email issuer)
                             :image_file (:image issuer)
                             :revocation_list_url (:revocationList issuer)}
            :criteria_content {:id ""
                               :html_content criteria
                               :markdown_text (http/alternate-get "text/x-markdown" criteria)}
            :creator_content  creator
            :issued_on  (u/str->epoch (:issuedOn assertion))
            :expires_on (u/str->epoch (:expires assertion))
            :evidence_url (:evidence assertion)}
;;;

(defmulti recipient (fn [_ asr] (:recipient asr)))

(defmethod recipient String [emails asr]
  (recipient emails (assoc asr :recipient {:salt (:salt asr)
                                           :hashed (not (re-find #"@" (:recipient asr)))
                                           :identity (:recipient asr)
                                           :type "email"})))

(defmethod recipient :default [emails asr]
  (let [hashed  (get-in asr [:recipient :hashed])
        salt    (get-in asr [:recipient :salt])
        we-have (get-in asr [:recipient :identity])
        [algo hash] (string/split we-have #"\$")
        check-fn (fn [they-sent]
                   (if hashed
                     (= hash (u/hex-digest algo (str they-sent salt)))
                     (= we-have they-sent)))]
    (if-let [found (first (filter check-fn (email-variations emails)))]
      found
      (throw (Exception. "badge/Userdoesnotownthisbadge")))))

;;;

(defmulti verify-assertion assertion-version)

(defmethod verify-assertion :v0.5.0 [url asr]
  (if-not (nil? url)
    (if (not= (domain (get-in asr [:badge :issuer :origin])) (domain url))
      (throw (IllegalArgumentException. "invalid assertion, origin url mismatch")))))

(defmethod verify-assertion :v2.0 [url asr]
  (let [kind (get-in asr [:verify :type] (get-in asr [:verification :type]))]
    (when (and (not (nil? url)) (or (= kind "hosted") (= kind "HostedBadge")))
      (if (not= (:id asr) url)
        (throw (IllegalArgumentException. "invalid assertion, verify url mismatch")))
      (if (map? (:badge asr))
        (if (not= (domain (get-in asr [:badge :id])) (domain (:id asr)))
          (throw (IllegalArgumentException. "invalid assertion, verify url mismatch")))
        (if (not= (domain (:badge asr)) (domain (:id asr)))
          (throw (IllegalArgumentException. "invalid assertion, verify url mismatch")))))))

(defmethod verify-assertion :default [url asr]
  (if-not (nil? url)
    (if (not= (get-in asr [:verify :url]) url)
      (throw (IllegalArgumentException. "invalid assertion, verify url mismatch"))))
  (if (not= (domain (get-in asr [:verify :url])) (domain (:badge asr)))
    (throw (IllegalArgumentException. "invalid assertion, verify url mismatch"))))

(defn assertion->badge
  ([user assertion] (assertion->badge user assertion {}))
  ([user assertion initial]
   (verify-assertion (:assertion_url initial) assertion)
   (let [now (u/now)]
     (-> initial
         (assoc :id nil
                :user_id (:id user)
                :email   (recipient (:emails user) assertion)
                :status "pending"
                :visibility "private"
                :show_recipient_name 0
                :rating nil
                :ctime now
                :mtime now
                :deleted 0
                :revoked 0
                :issuer_verified 0
                :show_evidence 0
                :last_checked nil
                :old_id nil)
         (badge-content assertion)
         valid-badge))))

;;;

(defmulti str->badge (fn [_ input]
                       (cond
                         (string/blank? input) :blank
                         (and (string? input) (re-find #"^https?://" input)) :url
                         (and (string? input) (re-find #"\{" input))         :json
                         (and (string? input) (re-find #".+\..+\..+" input)) :jws)))

(defmethod str->badge :json [user input]
  (let [content (json/read-str input :key-fn keyword)
        kind (get-in content [:verify :type] (get-in content [:verification :type]))]
    (if (or (= kind "hosted") (= kind "HostedBadge"))
      (str->badge user (get-in content [:verify :url] (:id content))))))

(defmethod str->badge :url [user input]
  (let [body (http/http-get input)]
    (assertion->badge user
                      (json/read-str body :key-fn keyword)
                      {:assertion_url input :assertion_json body :assertion_jws nil})))

(defmethod str->badge :jws [user input]
  (let [[raw-header raw-payload raw-signature] (clojure.string/split input #"\.")
        header     (-> raw-header u/url-base64->str (json/read-str :key-fn keyword))
        payload    (-> raw-payload u/url-base64->str (json/read-str :key-fn keyword))
        public-key (-> (get-in payload [:verify :url]) http/http-get keys/str->public-key)
        body       (-> input (jws/unsign public-key {:alg (keyword (:alg header))}) (String. "UTF-8"))]
    (assertion->badge user (json/read-str body :key-fn keyword) {:assertion_url nil :assertion_jws input :assertion_json body})))

(defmethod str->badge :blank [_ _]
  (throw (IllegalArgumentException. "missing assertion string")))

(defmethod str->badge :default [_ _]
  (log/error "str->badge: got unsupported assertion data")
  (log/error (pr-str _))
  (throw (IllegalArgumentException. "invalid assertion string")))

;;;

(defmulti file->badge (fn [_ upload]
                        (or (:content-type upload)
                            (get-in upload [:headers "Content-Type"]))))

(defmethod file->badge "image/png" [user upload]
  (some->> (doto (PngReader. (or (:tempfile upload) (:body upload))) (.readSkippingAllRows))
           .getMetadata
           #(or (.getTxtForKey % "openbadges") (.getTxtForKey % "openbadge"))
           string/trim
           (str->badge user)))

(defmethod file->badge "image/svg+xml" [user upload]
  (some->> (xml/parse (or (:tempfile upload) (:body upload)))
           :content
           (filter #(= :openbadges:assertion (:tag %)))
           first
           :content
           first
           string/trim
           (str->badge user)))

(defmethod file->badge :default [_ upload]
  (log/error "file->assertion: unsupported file type:" (:content-type upload))
  (throw (IllegalArgumentException. "invalid file type")))
