(ns salava.badge.assertion
  (:require [clj-http.client :as client]
            [clojure.tools.logging :as log]
            [clojure.java.io :as io]
            [clojure.data.json :as json]
            [slingshot.slingshot :refer :all]
            [net.cgrand.enlive-html :as html]
            [markdown.core :as md]
            [salava.core.util :refer [hex-digest]]
            [salava.core.time :refer [iso8601-to-unix-time]] ;cljc
            [salava.core.i18n :refer [t]]                    ;cljc
            [salava.badge.png :as p]))

(defn fetch-json-data [url]
  (try+
    (:body
      (client/request
        {:method         :get
         :url            url
         :as             :json
         :socket-timeout 30000
         :conn-timeout   30000}))
    (catch Object _
      {:error "badge/Errorfetchingjson"})))

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
      {:error (t :badge/Failedfetchsigned)})))

(defn get-criteria-markdown [criteria-url]
  "Get criteria markdown."
  (try+
    (let [html (html/html-resource (java.net.URL. criteria-url))
          links (filter #(and (= (get-in % [:attrs :rel]) "alternate")
                              (= (get-in % [:attrs :type]) "text/x-markdown")) (html/select html [:head :link]))
          href (first (map #(get-in % [:attrs :href]) links))]
      (if href
        (slurp href)))
    (catch Object _
      ; TODO: log error
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
        badge-url (str "dummy://" (hex-digest "sha1"
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
        issued-on-raw (or (:issuedOn assertion) (:issued-on assertion) nil)
        issued-on (cond
                    (re-find #"\D" (str issued-on-raw)) (iso8601-to-unix-time issued-on-raw)
                    (string? issued-on-raw) (read-string issued-on-raw)
                    :else issued-on-raw)
        expires-raw (:expires assertion)
        expires (cond
                  (re-find #"\D" (str expires-raw)) (iso8601-to-unix-time expires-raw)
                  (string? expires-raw) (read-string expires-raw)
                  :else expires-raw)
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
