(ns salava.badge.assertion
  (:import [ar.com.hjg.pngj PngReader])
  (:require [clojure.tools.logging :as log]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.data.json :as json]
            [clojure.xml :as xml]
            [buddy.sign.jws :as jws]
            [buddy.core.keys :as keys]
            [slingshot.slingshot :refer :all]
            [net.cgrand.enlive-html :as html]
            [markdown.core :as md]
            [salava.badge.png :as p]
            [salava.core.util :as u]))


(defn fetch-json-data [url]
  (log/info "fetch-json-data: GET" url)
  (u/http-get url {:as :json :accept :json :throw-entire-message? true})

  #_(try+
    (catch :status e
      (let [{:keys [status body]} e
            body-map (try+
                       (json/read-str body :key-fn keyword)
                       (catch Object _
                         {:raw body}))]
        {:error "badge/Errorfetchingjson" :status status :body body-map}))

    (catch Throwable ex
      {:error "badge/Errorfetchingjson" :body (.toString ex)})))


(defn copy-to-file [output-file url]
  (with-open [in (io/input-stream url)
              out (io/output-stream output-file)]
    (io/copy in out)
    out))

(defn fetch-signed-badge-assertion [image-url]
  (try+
    (let [temp-file (java.io.File/createTempFile "file" ".png")
          _ (copy-to-file temp-file image-url)
          assertion (p/get-assertion-from-png temp-file)]
      (.delete temp-file)
      assertion)
    (catch Object _
      (log/error (str  "Failed to fetch signed assertion from " image-url))
      {:error "badge/Failedfetchsigned})))"})))

(defn get-criteria-markdown [criteria-url]
  "Get criteria markdown, if available."
  (try
    (let [html  (html/html-resource (io/input-stream (u/http-get criteria-url {:as :byte-array})))
          links (filter #(and (= (get-in % [:attrs :rel]) "alternate")
                              (= (get-in % [:attrs :type]) "text/x-markdown")) (html/select html [:head :link]))
          href (first (map #(get-in % [:attrs :href]) links))]
      (if href
        (u/http-get href)))
    (catch Exception ex
      (log/error "get-criteria-markdown: failed to fetch content")
      (log/error (.toString ex))
      "")))

(defn new-badge-assertion [badge-assertion-url]
  (let [badge-data (fetch-json-data badge-assertion-url)
        issuer-url (:issuer badge-data)
        issuer-data (if-not (empty? issuer-url)
                      (fetch-json-data issuer-url))
        criteria (:criteria badge-data)
        criteria-markdown (or (get-criteria-markdown criteria) "")
        criteria-html (md/md-to-html-string criteria-markdown)
        original-creator-url (get-in badge-data [:extensions:OriginalCreator :url])
        original-creator (if (not-empty original-creator-url)
                           (fetch-json-data original-creator-url))]
    (assoc badge-data
           :badge_url badge-assertion-url
           :issuer_url issuer-url
           :issuer issuer-data
           :criteria_url criteria
           :criteria_markdown criteria-markdown
           :criteria_html criteria-html
           :OriginalCreator (assoc original-creator :json-url original-creator-url))))

(defn old-badge-assertion [assertion issued-on]
  (let [image-path (:image assertion)
        image (if (= (get image-path 0) "/" )
                (str (get-in assertion [:issuer :origin]) image-path)
                image-path)
        criteria-path (:criteria assertion)
        criteria (if (= (get criteria-path 0) "/" )
                   (str (get-in assertion [:issuer :origin]) criteria-path)
                   criteria-path)
        criteria-markdown (or (get-criteria-markdown criteria) "")
        criteria-html (md/md-to-html-string criteria-markdown)
        badge-url (str "dummy://" (u/hex-digest "sha1"
                                                (str
                                                  issued-on
                                                  (:name assertion)
                                                  (:image assertion)
                                                  (:criteria assertion))))
        original-creator-url (get-in assertion [:extensions:OriginalCreator :url])
        original-creator (if (not-empty original-creator-url)
                           (fetch-json-data original-creator-url))]
    (merge assertion {:image             image
                      :criteria_url      criteria
                      :criteria_markdown criteria-markdown
                      :criteria_html     criteria-html
                      :badge_url         badge-url
                      :issuer_url        nil
                      :issuer            {:name  (if (get-in assertion [:issuer :org])
                                                   (str (get-in assertion [:issuer :name]) ": " (get-in assertion [:issuer :org]))
                                                   (get-in assertion [:issuer :name]))
                                          :url   (get-in assertion [:issuer :origin])
                                          :email (or (get-in assertion [:issuer :contact]) "")}
                      :OriginalCreator   (assoc original-creator :json-url original-creator-url)})))

(defn create-assertion [a old-assertion]
  (let [assertion-url (if (string? a) a nil)
        assertion (if assertion-url (fetch-json-data assertion-url) a)
        error (or (:error assertion) nil)
        assertion-json (json/write-str assertion)
        uid (or (:uid assertion) nil)
        evidence (or (:evidence assertion) nil)
        verify (or (:verify assertion) {:type "hosted"
                                        :url assertion-url})
        issued-on (u/str->epoch (or (:issuedOn assertion) (:issued-on assertion)))
        expires   (u/str->epoch (:expires assertion))

        recipient (if (string? (:recipient assertion))
                    (merge
                      {:identity (:recipient assertion)
                       :type "email"
                       :hashed false}
                      (if-not (re-find #"@" (:recipient assertion))
                        {:hashed true
                         :salt (or (:salt assertion) "")}))
                    (:recipient assertion))
        badge (if (string? (:badge assertion))
                (new-badge-assertion (:badge assertion))
                (old-badge-assertion (or (:badge assertion) (:badge old-assertion)) issued-on))]
    {:uid              uid
     :assertion_json   assertion-json
     :evidence         evidence
     :verify           verify
     :issuedOn         issued-on
     :expires          expires
     :recipient        recipient
     :badge            badge
     :error            error}))

(defn domain [url]
  (last (re-find #"^https?://([^/]+)" url)))

(defn badge-image [input]
  (let [image (get-in input [:badge :image] "")]
    (if-let [match (re-find #"(?s)^data.+,(.+)" image)]
      (u/base64->bytes (last match))
      (try
        (u/http-get image)
        (catch Throwable ex
          (if (contains? (meta input) :image)
            (:image (meta input))
            (throw (Exception. "badge/Missingimage"))))))))


(defmulti assertion (fn [input]
                      :in-progress

                      #_(cond
                          (map? (:badge input)) :v0.5.0
                          (and (contains? input :id) (contains? input :type)) :v1.1
                          :else :v1.0)))


; Refactored assertion parsing functions are not yet ready.
; Use previous implementation for now.
(defmethod assertion :in-progress [input]
  (create-assertion input {}))


; See https://github.com/mozilla/openbadges-backpack/wiki/Assertion-Specification-Changes

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

;;;

(defmulti assertion-map (fn [input]
                          (cond
                            (string/blank? input) :blank
                            (and (string? input) (re-find #"^https?://" input)) :url
                            (and (string? input) (re-find #"\{" input))         :json
                            (and (string? input) (re-find #".+\..+\..+" input)) :jws)))


(defmethod assertion-map :json [input]
  (let [meta (or (meta input) {})]
    (with-meta (assertion (json/read-str input :key-fn keyword)) (assoc meta :assertion_json input))))

(defmethod assertion-map :url [input]
  (let [meta (or (meta input) {})]
    (with-meta (assertion-map (u/http-get input)) (assoc meta :assertion_url input))))

(defmethod assertion-map :jws [input]
  (let [[raw-header raw-payload raw-signature] (clojure.string/split input #"\.")
        header (-> raw-header u/url-base64->str (json/read-str :key-fn keyword))
        payload (-> raw-payload u/url-base64->str (json/read-str :key-fn keyword))
        public-key (-> (get-in payload [:verify :url])
                       u/http-get
                       keys/str->public-key)
        asr (-> input
                (jws/unsign public-key {:alg (keyword (:alg header))})
                (String. "UTF-8")
                (json/read-str :key-fn keyword))
        meta (or (meta input) {})]
    (with-meta (assertion asr) (-> meta
                                   (assoc :assertion_jws  input)
                                   (assoc :assertion_json (u/url-base64->str raw-payload))))))

(defmethod assertion-map :blank [_]
  (throw (Exception. "badge/MissingAssertion")))

(defmethod assertion-map :default [_]
  (log/error "assertion-map: got unsupported assertion data")
  (log/error (pr-str _))
  (throw (Exception. "badge/Invalidassertion")))

;;;

(defmulti baked-image :content-type)

(defmethod baked-image "image/png" [upload]
  (let [m (.getMetadata (doto (PngReader. (:tempfile upload)) (.readSkippingAllRows)))
        asr (string/trim (or (.getTxtForKey m "openbadges") (.getTxtForKey m "openbadge") ""))]
    (with-meta (assertion-map asr) {:image (slurp (:tempfile upload))})))

(defmethod baked-image "image/svg+xml" [upload]
  (let [asr (some->> (xml/parse (:tempfile upload))
                     :content
                     (filter #(= :openbadges:assertion (:tag %)))
                     first :content first string/trim)]
    (with-meta (assertion-map asr) {:image (slurp (:tempfile upload))})))

(defmethod baked-image :default [_]
  (log/error "unsupported badge file:" (:content-type _))
  (throw (Exception. "badge/Invalidfiletype")))
