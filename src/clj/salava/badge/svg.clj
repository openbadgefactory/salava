(ns salava.badge.svg
  (:require [slingshot.slingshot :refer :all]
            [salava.badge.signed :refer [signed-assertion]]
            [clojure.data.json :as json]
            [clj-xpath.core :as xp]
            [clojure.string :as string]))

(defn get-assertion [assertion]
  (cond
    (and (string? assertion) (re-matches #"^https?:\/\/.+" assertion)) assertion
    (not (map? assertion)) (signed-assertion assertion) ;signed-assertion
    :else (get-in assertion [:verify :url])))

(defn remove-doctype [xml]
  (string/replace xml #"<!DOCTYPE((.|\n|\r)*?)(\"|')>" ""))

(defn get-assertion-from-svg [svg-file]
  (try+
    (xp/with-namespace-context
      {"svg" "http://www.w3.org/2000/svg" "openbadges" "http://openbadges.org"}
      (let [data (->> svg-file slurp remove-doctype (xp/$x:text? "//svg:svg/openbadges:assertion"))]
        (if (empty? data)
          (throw+ "Empty metadata"))
        (get-assertion (json/read-str data :key-fn keyword))))
    (catch Exception _
      (throw+ (str "Error opening SVG-file: " _)))))
