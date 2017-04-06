(ns salava.core.util
  (:require [clojure.tools.logging :as log]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.data.json :as json]
            [digest :as d]
            [slingshot.slingshot :refer :all]
            [clj-http.client :as client]
            [clojure.java.shell :refer [sh]]
            [clj.qrgen :as q]
            [clj-time.coerce :as tc]
            [autoclave.core :refer [markdown-processor markdown-to-html]]
            [net.cgrand.enlive-html :as html]
            [salava.core.helper :refer [plugin-str]]
            [pantomime.mime :refer [extension-for-name mime-type-of]])
  (:import (java.io StringReader)
           (java.util Base64)))

(defn get-db [ctx]
  {:connection {:datasource (:db ctx)}})

(defn get-datasource [ctx]
  {:datasource (:db ctx)})

(defn get-db-1 [ctx]
  {:connection {:datasource (:db ctx)}
   :result-set-fn first})

(defn get-data-dir [ctx]
  (get-in ctx [:config :core :data-dir]))

(defn get-site-url [ctx]
  (get-in ctx [:config :core :site-url] ""))

(defn get-email-notifications [ctx]
  (get-in ctx [:config :user :email-notifications] false))

(defn get-site-name [ctx]
  (get-in ctx [:config :core :site-name] ""))

(defn get-base-path [ctx]
  (get-in ctx [:config :core :base-path] ""))

(defn get-full-path [ctx]
  (str (get-site-url ctx) (get-base-path ctx)))

(defn get-plugins [ctx]
  (get-in ctx [:config :core :plugins] []))


(defn base64->bytes [input]
  (.decode (Base64/getDecoder) input))

(defn url-base64->bytes [input]
  (.decode (Base64/getUrlDecoder) input))

(defn base64->str [input]
  (String. (base64->bytes input) "UTF-8"))

(defn url-base64->str [input]
  (String. (url-base64->bytes input) "UTF-8"))

(defn bytes->base64 [input]
  (.encodeToString (Base64/getEncoder) input))


(defn hex-digest [algo string]
  (case algo
    "sha1" (d/sha-1 string)
    "sha256" (d/sha-256 string)
    "sha512" (d/sha-512 string)
    "md5" (d/md5 string)
    (throw+ (str "Unknown algorithm: " algo))))

(defn ordered-map-values
  "Returns a flat list of keys and values in a map, sorted by keys."
  [coll]
  (flatten (seq (into (sorted-map) coll))))

(defn flat-coll [item]
  "Returns a sorted and flattened collection"
  (if-not (coll? item)
    (list item)
    (flatten (map flat-coll (if-not (map? item) item (ordered-map-values item))))))

(defn map-sha256
  "Calculates SHA-256 hex digest of (ordered) content in a collection"
  [coll]
  (hex-digest "sha256" (apply str (flat-coll coll))))

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
   (if (str/blank? url)
     (throw (IllegalArgumentException. "http-get: missing url parameter")))
   (try
     (:body (client/get url opt))
     (catch Exception ex
       (log/error "http-get: clj-http client request failed")
       (log/error "url:" url)
       (log/error (.toString ex))
       (log/error "falling back to curl")
       (curl url opt)))))

(defn json-get [url]
  (log/info "json-get: GET" url)
  (http-get url {:as :json :accept :json :throw-entire-message? true}))

(defn- file-extension [filename]
  (try+
    (let [file (if (re-find #"https?" (str filename)) (java.net.URL. filename) filename)]
      (-> file
          mime-type-of
          extension-for-name))
    (catch Object _
      (throw+ (str "Could not get extension for file: " filename)))))

(defn public-path-from-content
  [content extension]
  (let [checksum (hex-digest "sha256" content)]
    (apply str (concat (list "file/")
                       (interpose "/" (take 4 (char-array checksum)))
                       (list "/" checksum extension)))))

(defn public-path
  "Calculate checksum for file and use it as a filename under public dir"
  ([filename] (public-path filename (file-extension filename)))
  ([filename extension]
   (let [content (slurp filename)]
     (public-path-from-content content extension))))


(defn- save-file-data
  [ctx content filename]
  (let [data-dir (get-data-dir ctx)
        fullpath (str data-dir "/" filename)]
    (try+
      (if-not (.exists (io/as-file data-dir))
        (throw+ "Data directry does not exist"))
      (do
        (io/make-parents fullpath)
        (with-open [w (io/output-stream fullpath)]
          (.write w content))
        filename)
      (catch Object _
        (throw+ (str "Error copying file: " _))))))


(defn save-file-from-http-url
  [ctx url]
  (let [content   (http-get url {:as :byte-array})
        extension (-> content mime-type-of extension-for-name)
        path (public-path-from-content content extension)]
    (save-file-data ctx content path)))


(defn save-file-from-data-url
  [ctx data-str comma-pos]
  (if (> comma-pos -1)
    (let [base64-data (subs data-str (inc comma-pos))
          content (base64->bytes base64-data)
          content-str (base64->str base64-data)
          ext (-> content mime-type-of extension-for-name)
          path (public-path-from-content content-str ext)]
      (save-file-data ctx content path))))

(defn file-from-url
  [ctx url]
  (cond
    (str/blank? url) (throw (Exception. "file-from-url: url parameter missing"))
    (re-find #"^https?"  (str url)) (save-file-from-http-url ctx url)
    (re-find #"^data"    (str url)) (save-file-from-data-url ctx url (.lastIndexOf url ","))
    :else (throw (Exception. (str "Error in file url: " url)))))

(defn str->qr-base64 [text]
  (try
    (-> text
        str
        (q/from)
        (q/as-bytes)
        (bytes->base64))
    (catch Exception ex
      (log/error "str->qr->base64: failed to generate QR-code")
      (log/error (.toString ex)))))

(defn now []
  (int (/ (System/currentTimeMillis) 1000)))

(defn str->epoch
  "Convert string to unix epoch timestamp"
  [s]
  (cond
    (integer? s) s
    (str/blank? s) nil
    (re-find #"^[0-9]+$" s) (Long. s)
    (re-find #"^\d{4}-\d\d-\d\d" s) (some-> (tc/to-long s) (quot 1000))))

(def plugin-fun
  (memoize
    (fn [plugins nspace name]
      (let [fun (fn [p]
                  (try
                    (let [sym (symbol (str "salava." p "." nspace "/" name))]
                      (require (symbol (namespace sym)))
                      (resolve sym))
                    (catch Throwable _)))]
        (->> plugins
             (map #(str/replace (plugin-str %) #"/" "."))
             (map fun)
             (filter #(not (nil? %))))))))

(def default-md-processor (markdown-processor :quotes :suppress-all-html))

(defn md->html
  "convert markdown to html and sanitize output"
  ([md] (if (nil? md) "" (md->html md default-md-processor)))
  ([md processor] (if (nil? md) "" (markdown-to-html processor md))))

(defn alt-markdown
  "Try to find alternate markdown version from input html"
  [^String input]
  (let [link-tags (-> (StringReader. input) (html/html-resource) (html/select [:head :link]))
        md-url (some #(when (and (= (:rel %) "alternate") (= (:type %) "text/x-markdown")) (:href %))
                     (map :attrs link-tags))]
    (try (http-get md-url) (catch Exception _ ""))))
