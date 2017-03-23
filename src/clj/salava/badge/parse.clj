(ns salava.badge.parse
  (:require [buddy.sign.jws :as jws]
            [buddy.core.keys :as keys]
            [clojure.tools.logging :as log]
            [clojure.string :as string]
            [clojure.data.json :as json]
            [clojure.xml :as xml]
            [net.cgrand.enlive-html :as html]
            [salava.core.util :as u]
            [schema.core :as s])
  (:import (ar.com.hjg.pngj PngReader)
           (java.io StringReader)))

(s/defschema BadgeContent {:id    s/Str
                           :name  s/Str
                           :image_file  s/Str
                           :description s/Str
                           :alignment [(s/maybe {:name s/Str
                                                 :url  s/Str
                                                 :description s/Str})]
                           :tags      [(s/maybe s/Str)]})

(s/defschema IssuerContent {:id   s/Str
                            :name s/Str
                            :url  s/Str
                            :description (s/maybe s/Str)
                            :image_file (s/maybe s/Str)
                            :email (s/maybe s/Str)
                            :revocation_list_url (s/maybe s/Str)})

(s/defschema CreatorContent (-> IssuerContent
                                (dissoc :revocation_list_url)
                                (assoc :json_url s/Str)))

(s/defschema CriteriaContent {:id s/Str
                              :html_content s/Str
                              :markdown_content (s/maybe s/Str)})

(s/defschema Badge {:id (s/maybe s/Int)
                    :user_id s/Int
                    :email   s/Str
                    :assertion_url    (s/maybe s/Str)
                    :assertion_jws    (s/maybe s/Str)
                    :assertion_json   s/Str
                    :badge_url        (s/maybe s/Str)
                    :issuer_url       (s/maybe s/Str)
                    :creator_url      (s/maybe s/Str)
                    :criteria_url     s/Str
                    :badge_content    BadgeContent
                    :issuer_content   IssuerContent
                    :criteria_content CriteriaContent
                    :creator_content  (s/maybe CreatorContent)
                    :issued_on  s/Int
                    :expires_on (s/maybe s/Int)
                    :evidence_url (s/maybe s/Str)
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

(def valid-badge (s/validator Badge))

;;;

(defn- domain [url]
  (last (re-find #"^https?://([^/]+)" url)))

(defn- badge-image [input]
  (let [image (if (map? input) (get-in input [:badge :image] "") input)]
    (if-let [match (re-find #"(?s)^data.+,(.+)" image)]
      (u/base64->bytes (last match))
      (try
        (u/http-get image)
        (catch Throwable ex
          (if (contains? (meta input) :image)
            (:image (meta input))
            (throw (Exception. "image missing"))))))))


(defn- email-variations [emails]
  (mapcat #(list (string/upper-case %)
                 (string/lower-case %)
                 (string/capitalize %)) emails))

;;;

;; See https://github.com/mozilla/openbadges-backpack/wiki/Assertion-Specification-Changes
(defmulti badge-content (fn [_ assertion]
                          (cond
                            (map? (:badge assertion)) :v0.5.0
                            (and (contains? assertion :id) (contains? assertion :type)) :v1.1
                            :else :v1.0)))

(defmethod badge-content :v0.5.0 [initial assertion]
  (if (contains? initial :assertion_url)
    (if (not= (domain (get-in assertion [:badge :issuer :origin])) (domain (:assertion_url initial)))
      (throw (IllegalArgumentException. "invalid assertion, origin url mismatch"))))

  (let [q-url (fn [url]
                (if (re-find #"^/" (str url))
                  (str (get-in assertion [:badge :issuer :origin]) url) url))
        badge    (:badge assertion)
        issuer   (:issuer badge)
        criteria (u/http-get (q-url (:criteria badge)))]

    (merge initial
           {:badge_url    nil
            :issuer_url   nil
            :criteria_url (q-url (:criteria badge))
            :creator_url  nil
            :badge_content {:id ""
                            :name (:name badge)
                            :image_file (q-url (:image badge))
                            :description (:description badge)
                            :alignment []
                            :tags (get badge :tags [])}
            :issuer_content {:id ""
                             :name (str (:name issuer) ": " (:org issuer))
                             :description (:description issuer)
                             :url (q-url (:origin issuer))
                             :email (:contact issuer)
                             :image_file nil
                             :revocation_list_url nil}
            :criteria_content {:id ""
                               :html_content criteria
                               :markdown_content (u/alt-markdown criteria)}
            :creator_content  nil
            :issued_on  (u/str->epoch (or (:issued_on assertion) (:issuedOn assertion)))
            :expires_on (u/str->epoch (:expires assertion))
            :evidence_url (q-url (:evidence assertion))})))


(defmethod badge-content :default [initial assertion]
  (if (contains? initial :assertion_url)
    (if (not= (get-in assertion [:verify :url]) (:assertion_url initial))
      (throw (IllegalArgumentException. "invalid assertion, verify url mismatch"))))
  (if (not= (domain (get-in assertion [:verify :url])) (domain (:badge assertion)))
    (throw (IllegalArgumentException. "invalid assertion, verify url mismatch")))

  (let [badge  (u/json-get (:badge assertion))
        issuer (u/json-get (:issuer badge))
        criteria (u/http-get (:criteria badge))
        creator-url (:extensions:OriginalCreator badge)
        creator (if creator-url
                  (let [data (u/json-get creator-url)]
                    {:id ""
                     :name        (:name data)
                     :image_file  (:image data)
                     :description (:description data)
                     :url   (:url data)
                     :email (:email data)
                     :json_url creator-url}))]

    (merge initial
           {:badge_url    (:badge assertion)
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
                               :markdown_content (u/alt-markdown criteria)}
            :creator_content  creator
            :issued_on  (u/str->epoch (:issuedOn assertion))
            :expires_on (u/str->epoch (:expires assertion))
            :evidence_url (:evidence assertion)})))

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

(defn assertion->badge
  ([user assertion] (assertion->badge user assertion {}))
  ([user assertion initial]
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
  (let [content (json/read-str input :key-fn keyword)]
    (if (= ((get-in content [:verify :type])) "hosted")
      (str->badge user (get-in content [:verify :url])))))

(defmethod str->badge :url [user input]
  (let [body (u/http-get input)]
    (assertion->badge user
                      (json/read-str body :key-fn keyword)
                      {:assertion_url input :assertion_json body :assertion_jws nil})))

(defmethod str->badge :jws [user input]
  (let [[raw-header raw-payload raw-signature] (clojure.string/split input #"\.")
        header     (-> raw-header u/url-base64->str (json/read-str :key-fn keyword))
        payload    (-> raw-payload u/url-base64->str (json/read-str :key-fn keyword))
        public-key (-> (get-in payload [:verify :url]) u/http-get keys/str->public-key)
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
