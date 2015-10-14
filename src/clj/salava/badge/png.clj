(ns salava.badge.png
  (:require [slingshot.slingshot :refer :all]
            [clojure.data.json :as json]
            [clojure.string :refer [blank?]])
  (:import [ar.com.hjg.pngj PngReader]))

(defn init-reader [file]
  (try+
    (PngReader. file)
    (catch Object _
      (throw+ (str "Error opening PNG-file: " _) ))))

(defn get-png-metadata [file]
  (let [reader (init-reader file)
        metadata (.getMetadata reader)
        openbadges-data (.getTxtForKey metadata "openbadges")]
    (if (blank? openbadges-data)
      (throw+ "Empty metadata"))
    openbadges-data))

(defn get-assertion-from-png [file]
  (let [data (get-png-metadata file)
        hosted? (re-find #"\{" data)]
    (if hosted?
      (get-in (json/read-str data :key-fn keyword) [:verify :url])
      ;TODO parse signed assertion
      )))