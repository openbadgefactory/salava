(ns salava.badge.signed
  (:require [slingshot.slingshot :refer :all]
            [clojure.data.json :as json]
            [salava.core.util :as u]
            [salava.core.http :as http]
            [buddy.sign.jws :as jws]
            [buddy.core.keys :as keys]))

(def algorighms ["rs256" "rs384" "rs512"])

(defn verify-signature [data public-key alg]
  (try+
    (jws/unsign data (keys/str->public-key public-key) {:alg (keyword alg)})
    (catch Object _)))

(defn fetch-public-key [url]
  (try+
    (:body (http/http-get url))
    (catch Object _)))

(defn signed-assertion [metadata]
  (let [[raw-header raw-payload raw-signature] (clojure.string/split metadata #"\.")
        header (-> raw-header u/url-base64->str (json/read-str :key-fn keyword))
        alg (:alg header)
        payload (-> raw-payload u/url-base64->str (json/read-str :key-fn keyword))
        public-key (fetch-public-key (get-in payload [:verify :url]))]
    (if-not (and (map? header) (contains? header :alg))
      (throw+ "Invalid signed badge header"))
    (if-not (some #(= alg %) algorighms)
      (throw+ (str "Invalid signed assertion: unknown signature algorithm " alg)))
    (if-not (map? payload)
      (throw+ "Failed to decode signed assertion payload "))
    (if-not public-key
      (throw+ "Invalid signed assertion: missing public key"))
    (if-not (verify-signature metadata public-key alg)
      (throw+ "Invalid signed assertion: signature verification failed"))
    payload))
