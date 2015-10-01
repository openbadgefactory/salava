(ns salava.core.util
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))


(defn hex-digest [algo string]
  (let [digest (.digest (java.security.MessageDigest/getInstance algo) (.getBytes string))]
    (.toString (new java.math.BigInteger 1 digest) 16)))


(defn public-path [filename]
  "Calculate checksum for file and use it as a filename under public dir"
  (let [content (slurp filename)
        checksum (hex-digest "SHA-256" content)
        extension (last (str/split (str filename) #"\."))]
    (apply str (concat (list "file/")
                       (interpose "/" (take 4 (char-array checksum)))
                       (list "/" checksum "." extension)))))

