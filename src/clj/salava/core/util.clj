(ns salava.core.util
  (:require [clojure.tools.logging :as log]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [digest :as d]
            [slingshot.slingshot :refer :all]
            [clj-http.client :as client]
            [clojure.java.shell :refer [sh]]
            [clj.qrgen :as q]
            [clj-time.coerce :as tc]
            [pantomime.mime :refer [extension-for-name mime-type-of]]
            [buddy.core.codecs :refer [base64->bytes base64->str bytes->base64]]))

(defn get-db [ctx]
  {:connection {:datasource (:db ctx)}})

(defn get-datasource [ctx]
  {:datasource (:db ctx)})

(defn get-data-dir [ctx]
  (get-in ctx [:config :core :data-dir]))

(defn get-site-url [ctx]
  (get-in ctx [:config :core :site-url] ""))

(defn get-site-name [ctx]
  (get-in ctx [:config :core :site-name] ""))

(defn get-base-path [ctx]
  (get-in ctx [:config :core :base-path] ""))

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


(defn http-get
  "Run simple HTTP GET request. Uses clj-http.client with curl as fallback. Returns body of the response as string."
  ([url] (http-get url {}))
  ([url opt]
   (try
     (:body (client/get url (dissoc opt :as)))
     (catch Exception ex
       (log/error "clj-http client request failed, falling back to curl")
       (log/error "URL:" url)
       (let [res (sh "/usr/bin/curl" "-f" "-s" "-L" "-m30" url)]
         (if (= (:exit res) 0)
           (:out res)
           (throw ex)))))))

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
  (let [content   (.getBytes (http-get url))
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
    (re-find #"^https?" (str url)) (save-file-from-http-url ctx url)
    (re-find #"^data"   (str url)) (save-file-from-data-url ctx url (.lastIndexOf url ","))
    :else (throw+ (str "Error in file url: " url))))

(defn str->qr-base64 [text]
  (try
    (-> text
        str
        (q/from)
        (q/as-bytes)
        (bytes->base64))
    (catch Exception ex
      (log/error "str->qr->base64: failed to generate QR-code")
      (log/error (.getMessage ex)))))

(defn str->epoch
  "Convert string to unix epoch timestamp"
  [s]
  (cond
    (integer? s) s
    (str/blank? s) nil
    (re-find #"^[0-9]+$" (str/trim s)) (Long. s)
    :else (tc/to-long s)))
