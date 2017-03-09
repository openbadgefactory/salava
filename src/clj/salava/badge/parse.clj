(ns salava.badge.parse
  (:require [clojure.tools.logging :as log]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.data.json :as json]
            [clojure.xml :as xml]
            [buddy.sign.jws :as jws]
            [buddy.core.keys :as keys]
            [net.cgrand.enlive-html :as html]
            [salava.core.util :as u])
  (:import [ar.com.hjg.pngj PngReader]))

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


(defmulti assertion (fn [input]
                      (cond
                        (map? (:badge input)) :v0.5.0
                        (and (contains? input :id) (contains? input :type)) :v1.1
                        :else :v1.0)))

;; See https://github.com/mozilla/openbadges-backpack/wiki/Assertion-Specification-Changes

(defmethod assertion :v0.5.0 [input]
  (let [q-url (fn [url]
                (if (re-find #"^/" (str url))
                  (str (get-in input [:badge :issuer :origin]) url) url))]
    {:recipient {:identity (:recipient input)
                 :type "email"
                 :salt (get input :salt "")
                 :hashed (not (boolean (re-find #"\@" (:recipient input))))}
     :badge {:name         (get-in input [:badge :name])
             :image        (badge-image (q-url (get-in input [:badge :image])))
             :description  (get-in input [:badge :description])
             :criteria     (q-url (get-in input [:badge :criteria]))
             :issuer {:name  (str (get-in input [:badge :issuer :name])
                                  ": "
                                  (get-in input [:badge :issuer :org]))
                      :url   (get-in input [:badge :issuer :origin])
                      :email (get-in input [:badge :issuer :contact])}}
     :evidence (q-url (:evidence input))
     :expires  (u/str->epoch (:expires input))
     :issuedOn (u/str->epoch (or (:issued_on input) (:issuedOn input)))
     :verify {:type "hosted"
              :url  (get-in input [:verify :url])}}))


(defmethod assertion :default [input]
  (if (contains? (meta input) :assertion_url)
    (if (not= (get-in input [:verify :url]) (:assertion_url (meta input)))
      (throw (Exception. "badge/VerifyURLMismatch"))))
  (if (not= (domain (get-in input [:verify :url])) (domain (:badge input)))
    (throw (Exception. "badge/VerifyURLMismatch")))
  (let [badge  (json/read-str (u/http-get (:badge input)))
        issuer (json/read-str (u/http-get (:issuer badge)))]
    (-> input
        (assoc :expires  (u/str->epoch (:expires input)))
        (assoc :issuedOn (u/str->epoch (:issuedOn input)))
        (assoc :badge badge)
        (assoc-in [:badge :image] (badge-image input))
        (assoc-in [:badge :issuer] issuer))))


(defmulti str->assertion(fn [input]
                          (cond
                            (string/blank? input) :blank
                            (and (string? input) (re-find #"^https?://" input)) :url
                            (and (string? input) (re-find #"\{" input))         :json
                            (and (string? input) (re-find #".+\..+\..+" input)) :jws)))

(defmethod str->assertion :url [input]
  (let [input-meta (or (meta input) {})]
    (with-meta (assertion (u/json-get input)) (assoc input-meta :assertion_url input))))

(defmethod str->assertion :json [input]
  (let [input-meta (or (meta input) {})]
    (assertion (with-meta (json/read-str input :key-fn keyword) (assoc input-meta :assertion_json input)))))

(defmethod str->assertion :jws [input]
  (let [input-meta (or (meta input) {})
        [raw-header raw-payload raw-signature] (clojure.string/split input #"\.")
        header (-> raw-header u/url-base64->str (json/read-str :key-fn keyword))
        payload (-> raw-payload u/url-base64->str (json/read-str :key-fn keyword))
        public-key (-> (get-in payload [:verify :url])
                       u/http-get
                       keys/str->public-key)
        asr (-> input
                (jws/unsign public-key {:alg (keyword (:alg header))})
                (String. "UTF-8")
                (json/read-str :key-fn keyword))]
    (assertion (with-meta asr (-> input-meta
                                  (assoc :assertion_jws  input)
                                  (assoc :assertion_json (u/url-base64->str raw-payload)))))))

(defmethod str->assertion :blank [_]
  (throw (IllegalArgumentException. "missing assertion string")))

(defmethod str->assertion :default [_]
  (log/error "str->assertion: got unsupported assertion data")
  (log/error (pr-str _))
  (throw (IllegalArgumentException. "invalid assertion string")))


(defmulti file->assertion :content-type)

(defmethod file->assertion "image/png" [upload]
  (let [m (.getMetadata (doto (PngReader. (:tempfile upload)) (.readSkippingAllRows)))
        content (string/trim (or (.getTxtForKey m "openbadges") (.getTxtForKey m "openbadge") ""))
        asr (str->assertion content)
        input-meta (or (meta asr) {})]
    (with-meta asr (assoc input-meta :image (slurp (:tempfile upload))))))

(defmethod file->assertion "image/svg+xml" [upload]
  (let [content (some->> (xml/parse (:tempfile upload))
                     :content
                     (filter #(= :openbadges:assertion (:tag %)))
                     first :content first string/trim)
        asr (str->assertion content)
        input-meta (or (meta asr) {})]
    (with-meta asr (assoc input-meta :image (slurp (:tempfile upload))))))

(defmethod file->assertion :default [_]
  (log/error "file->assertion: unsupported file type:" (:content-type _))
  (throw (IllegalArgumentException. "invalid file type")))
