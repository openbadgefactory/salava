(ns salava.core.util
  (:require [clojure.tools.logging :as log]
            [clojure.java.io :as io]
            [clojure.string :as string]
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
            [salava.core.http :as http]
            [pantomime.mime :refer [extension-for-name mime-type-of]]
            [clojure.core.async :refer [>!!]])
  (:import (java.io StringReader)
           (java.net URLEncoder)
           (java.util Base64)))

(defn get-datasource
  "Get datasource in context or default db-spec for testing."
  [ctx]
  (if (:db ctx)
    {:datasource (:db ctx)}
    {:dbtype "mysql"
     :dbname "salava"
     :user "salava"
     :password "salava"}))

(defn get-db [ctx]
  {:connection (get-datasource ctx)})

(defn get-db-1 [ctx]
  {:connection (get-datasource ctx)
   :result-set-fn first})

(defn get-db-col [ctx kw]
  {:connection (get-datasource ctx)
   :row-fn kw})

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

(defn bytes->base64-url [input]
  (.encodeToString (Base64/getUrlEncoder) input))

(defn hex-digest [algo string]
  (case algo
    "sha1" (d/sha-1 string)
    "sha256" (d/sha-256 string)
    "sha512" (d/sha-512 string)
    "md5" (d/md5 string)
    (throw+ (str "Unknown algorithm: " algo))))

(defn digest [algo string]
  (->> string
    (hex-digest algo)
    (partition 2)
    (map (fn [[x y]] (Integer/parseInt (str x y) 16)))
    byte-array))

(defn random-token
  ([] (random-token ""))
  ([seed] (hex-digest "sha256" (str (System/currentTimeMillis) (java.util.UUID/randomUUID) seed))))

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

(defn extension-from-content [content]
  (let [extension (-> content mime-type-of extension-for-name)]
    (if (and (= ".txt" extension) (re-find #"<svg" (String. content))) ".svg" extension)))


(defn save-file-from-http-url
  [ctx url]
  (let [content (http/http-get url {:as :byte-array :max-redirects 5})
        extension (extension-from-content content)
        path (public-path-from-content content extension)]
    (save-file-data ctx content path)))



(defn save-file-from-data-url
  [ctx data-str comma-pos]
  (if (> comma-pos -1)
    (let [base64-data (subs data-str (inc comma-pos))
          content (base64->bytes base64-data)
          content-str (base64->str base64-data)
          ext (if (re-find #"^data:image/svg" data-str) ".svg" ".png")
          path (public-path-from-content content-str ext)]
      (save-file-data ctx content path))))

(defn file-from-url
  [ctx url]
  (cond
    (string/blank? url) (throw (Exception. "file-from-url: url parameter missing"))
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
    (string/blank? s) nil
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
             (map #(string/replace (plugin-str %) #"/" "."))
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
    (try (http/http-get md-url) (catch Exception _ ""))))

(defn url-encode [v]
  (some-> v str (URLEncoder/encode "UTF-8") (.replace "+" "%20")))


(defn publish [ctx topic data]
  (if-not (map? data)
    (throw (IllegalArgumentException. "Publish: Data must be a map")))
  (if-not  (keyword? topic)
    (throw (IllegalArgumentException. "Publish: Topic must be a keyword")))
  (>!! (:input-chan ctx) (assoc data :topic topic)))

(defn event [ctx subject verb object type]
  ;TODO VALIDATE DATA
  (publish ctx :event {:subject subject :verb verb :object object :type type}))

