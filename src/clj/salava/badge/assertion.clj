(ns salava.badge.assertion
  (:require [clj-http.client :as client]
            [clojure.java.io :as io]
            [clojure.data.json :as json]
            [slingshot.slingshot :refer :all]
            [net.cgrand.enlive-html :as html]
            [markdown.core :as md]
            [salava.core.util :refer [hex-digest]]
            [salava.core.time :refer [iso8601-to-unix-time]] ;cljc
            [salava.badge.png :as p]))

(defn fetch-json-data [url]
  (try+
    (:body
      (client/request
        {:method      :get
         :url         url
         :as          :json}))
    (catch Object _
      {:error (str "Error fetching json" url)})))

(defn copy-to-file [output-file url]
  (with-open [in (io/input-stream url)
              out (io/output-stream output-file)]
    (io/copy in out)
    out))

(defn fetch-signed-badge-assertion [image-url]
  (try+
    (let [temp-file (java.io.File/createTempFile "file" ".png")
          output (copy-to-file temp-file image-url)
          assertion (p/get-assertion-from-png temp-file)]
      (.delete temp-file)
      assertion)
    (catch Object _
      ;TODO: log an error
      {:error (str "Failed to fetch signed badge assertion from " image-url)})))

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
        criteria-html (md/md-to-html-string criteria-markdown)]
    (assoc badge-data
      :issuer_url issuer-url
      :issuer issuer-data
      :criteria_url criteria
      :criteria_markdown criteria-markdown
      :criteria_html criteria-html)))

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
                                                (:criteria assertion))))]
    (merge assertion {:image image
                      :criteria_url criteria
                      :criteria_markdown criteria-markdown
                      :criteria_html criteria-html
                      :badge_url badge-url
                      :issuer_url nil
                      :issuer {:name (str (get-in assertion [:issuer :name])
                                          ": "
                                          (get-in assertion [:issuer :org]))
                               :url (get-in assertion [:issuer :origin])
                               :email (or (get-in assertion [:issuer :contact]) "")}})))

(defn create-assertion [a old-assertion]
  (let [assertion-url (if (string? a) a nil)
        assertion (if assertion-url
                    (fetch-json-data assertion-url)
                    a)
        error (or (:error assertion) nil)
        assertion-json (json/write-str assertion)
        uid (or (:uid assertion) nil)
        evidence (or (:evidence assertion) nil)
        verify (or (:verify assertion) {:type "hosted"
                                        :url assertion-url})
        issued-on-raw (or (:issuedOn assertion) (:issued-on assertion) "0")
        issued-on (if (re-find #"\D" issued-on-raw)
                    (iso8601-to-unix-time issued-on-raw)
                    issued-on-raw)
        expires-raw (or (:expires assertion) "0")
        expires (if (re-find #"\D" expires-raw)
                  (iso8601-to-unix-time expires-raw)
                  expires-raw)
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
    {:uid uid
     :assertion_json assertion-json
     :evidence evidence
     :verify verify
     :issuedOn issued-on
     :expires expires
     :recipient recipient
     :badge badge
     :error error}))