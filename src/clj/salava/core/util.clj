(ns salava.core.util
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [slingshot.slingshot :refer :all]
            [clj-http.client :as client]))


(defn get-db [ctx]
  {:connection {:datasource (:db ctx)}})

(defn hex-digest [algo string]
  (let [digest (.digest (java.security.MessageDigest/getInstance algo) (.getBytes string))]
    (.toString (new java.math.BigInteger 1 digest) 16)))


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
  (hex-digest "SHA-256" (apply str (flat-coll coll))))

(defn file-extension [filename]
  (last (str/split (str filename) #"\.")))

(defn public-path
  "Calculate checksum for file and use it as a filename under public dir"
  ([filename] (public-path filename (file-extension filename)))
  ([filename extension]
   (let [content (slurp filename)
         checksum (hex-digest "SHA-256" content)]
     (apply str (concat (list "file/")
                        (interpose "/" (take 4 (char-array checksum)))
                        (list "/" checksum "." extension))))))

(defn fetch-file-content [url]
  "Fetch file content from url"
  (try+
    (:body
      (client/get url {:as :byte-array}))
    (catch Object _
      (throw+ (str "Error fetching file from: " url)))))

(defn trim-path [path]
  (if (re-find #"\?" path)
    (subs path 0 (.lastIndexOf path "?"))
    path))

(defn file-from-url
  ([url] (file-from-url url nil))
  ([url filename]
   (if (re-find #"https?" (str url))
     (let [content (fetch-file-content url)
           path (public-path url)
           fname (or filename
                     (trim-path path))
           fullpath (or filename
                        (str "resources/public/" fname))]
       (try+
         (do
           (io/make-parents fullpath)
           (with-open [w (io/output-stream fullpath)]
             (.write w content))
           fname)
         (catch Object _
           (throw+ "Error copying file"))))
     (throw+ "Error in file url"))))
