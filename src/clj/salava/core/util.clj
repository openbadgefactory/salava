(ns salava.core.util
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))


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


(defn public-path [filename]
  "Calculate checksum for file and use it as a filename under public dir"
  (let [content (slurp filename)
        checksum (hex-digest "SHA-256" content)
        extension (last (str/split (str filename) #"\."))]
    (apply str (concat (list "file/")
                       (interpose "/" (take 4 (char-array checksum)))
                       (list "/" checksum "." extension)))))

