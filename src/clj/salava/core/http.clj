(ns salava.core.http
  (:require [clojure.tools.logging :as log]
            [clojure.string :as string]
            [clojure.data.json :as json]
            [clj-http.client :as client]
            [clojure.java.shell :refer [sh]]
            [net.cgrand.enlive-html :as html])
  (:import (java.io StringReader)))


(defn http-req [opt]
  (-> {:socket-timeout 60000
       :conn-timeout   60000
       :max-redirects  5
       :throw-exceptions true
       :throw-entire-message? true}
      (merge opt)
      (client/request)))


(defn- curl [url opt]
  (let [accept (case (:accept opt)
                 :json "-H'Accept: application/json, */*'"
                 nil   "-H'Accept: */*'")
        out-fn (case (:as opt)
                 :json #(json/read-str % :key-fn keyword)
                 :byte-array identity
                 nil         identity)
        out-enc (case (:as opt)
                  :byte-array :bytes
                  :json "UTF-8"
                  nil   "UTF-8")
        res (sh "/usr/bin/curl" "-f" "-s" "-L" "-m30" accept url :out-enc out-enc)]
    (if (= (:exit res) 0)
      (do
        (log/info "curl request ok")
        (out-fn (:out res)))
      (throw (Exception. (str "GET request to " url " failed"))))))


(defn http-get
  "Run simple HTTP GET request. Uses clj-http.client with curl as fallback. Returns body of the response as string."
  ([url] (http-get url {}))
  ([url opt]
   (if (string/blank? url)
     (throw (IllegalArgumentException. "http-get: missing url parameter")))
   (try
     (:body (http-req (assoc opt :method :get :url url)))
     (catch Exception ex
       (log/error "http-get: clj-http client request failed")
       (log/error "url:" url)
       (log/error (.toString ex))
       (log/error "falling back to curl")
       (curl url opt)))))

(defn json-get [url]
  ;(log/info "json-get: GET" url)
  (http-get url {:as :json :accept :json :throw-entire-message? true}))


(defn http-post [url opt]
  (http-req (assoc opt :method :post :url url)))


(defmulti alternate-get (fn [_ input]
                          (cond
                            (string/blank? input) :blank
                            (re-find #"^https?://" input) :url
                            :else :html)))

(defmethod alternate-get :url [type input]
  (try
    (alternate-get type (http-get input))
    (catch Exception _ "")))

(defmethod alternate-get :html [type ^String input]
  (let [link-tags (-> (StringReader. input) (html/html-resource) (html/select [:head :link]))
        md-url (some #(when (and (= (:rel %) "alternate") (= (:type %) type)) (:href %))
                     (map :attrs link-tags))]
    (try (http-get md-url) (catch Exception _ ""))))

(defmethod alternate-get :default [_ _] "")
