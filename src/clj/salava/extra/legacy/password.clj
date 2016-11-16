(ns salava.extra.legacy.password
  (:import (java.security MessageDigest))
  (:require [clojure.string :as s]))

(def a64-str "./0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz")

(def a64-array (vec (char-array a64-str)))

(defn- i->a64 [i]
  (get a64-array (bit-and i 0x3f)))

(defn- ord [coll i]
  (int (bit-and (get coll i) 0xff)))

(defn- B64-chunk [input i]
  (let [v-1 (ord input i)
        v-2 (bit-or v-1 (bit-shift-left (ord input (+ i 1)) 8))
        v-3 (bit-or v-2 (bit-shift-left (ord input (+ i 2)) 16))]
    (str (i->a64 v-1)
         (i->a64 (unsigned-bit-shift-right v-2 6))
         (i->a64 (unsigned-bit-shift-right v-3 12))
         (i->a64 (unsigned-bit-shift-right v-3 18)))))

(defn- hash->B64 [bytes]
  (apply str (map #(B64-chunk bytes %) (range 0 (- (count bytes) 3) 3))))

(defn- sha-512 [a b]
  (.digest (doto (MessageDigest/getInstance "SHA-512") (.update a) (.update b))))

(defn- pw-hash [rounds salt input]
  (let [input-bytes (.getBytes input "UTF-8")
        init  (sha-512 (.getBytes salt "UTF-8") input-bytes)
        final (reduce (fn [current _] (sha-512 current input-bytes)) init (range rounds))]
    (subs (hash->B64 (vec final)) 0 (- 55 12))))

(defn- hash-rounds [a]
  (.pow (BigInteger. "2") (s/index-of a64-str a)))

(defn- unpack [input]
  (if-not (re-find #"^\$S\$.{52}$" input)
    (throw (Exception. "unsupported password format")))
  [(hash-rounds (subs input 3 4)) (subs input 4 12) (subs input 12)])

(defn- compare [a-bytes b-bytes]
  (if (not= (count a-bytes) (count b-bytes))
    (throw (Exception. "string comparison length mismatch")))
  (= 0 (reduce bit-or (map bit-xor (.getBytes a-bytes) (.getBytes b-bytes)))))

(defn check-password [they-sent we-have]
  (if (or (s/blank? we-have) (s/blank? they-sent))
    (throw (Exception. "missing input strings")))
  (let [[rounds salt our-hash] (unpack we-have)]
    (compare our-hash (pw-hash rounds salt they-sent))))
