(ns salava.translator.routes
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [clojure.tools.logging :as log]
            [schema.core :as s]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojurewerkz.propertied.properties :refer [map->properties]]))


(defn prop-write [filename prop]
  (with-open [w (io/writer filename :encoding "UTF-8")]
      (.store prop w nil)))

(defn prop-file [lang plugin]
  (io/resource (str "i18n/" (name lang) "/" (name plugin) "_" (name lang) ".properties")))

(defn save-prop-file [data lang plugin ]
  (let [out-file (prop-file lang plugin)]
    (prop-write out-file (map->properties data))))

(defn save-dict [dict]
  (with-open [w (io/writer (io/resource "i18n/dict.clj") :encoding "UTF-8")]
    (.write w (pr-str dict))))

(defn get-dict []
  (-> (io/resource "i18n/dict.clj") slurp read-string))

(defn touch-i18n []
  (-> (io/resource "salava/core/i18n.cljc")
      (io/file)
      (.setLastModified (System/currentTimeMillis))))


(def update-guard (Object.))

(defn i18n-update [ctx lang plugin key value]
  (locking update-guard
    (let [lang-ok   (some #(= lang   %1) (get-in ctx [:config :core :languages]))
          plugin-ok (some #(= plugin %1) (cons :core (get-in ctx [:config :core :plugins])))
          dict      (assoc-in (get-dict) [lang plugin key] value)]
      (when (and lang-ok plugin-ok)
        (save-dict dict)
        (save-prop-file (get-in dict [lang plugin]) lang plugin)
        (touch-i18n)
        "ok"))))



(defn route-def [ctx]
  (routes
    (context "/obpv1/translator" []
             :tags ["translator"]
             (POST "/:lang/:plugin/:key" []
                   :summary "Update translation string"
                   :path-params [lang :- s/Str
                                 plugin :- s/Str
                                 key :- s/Str]
                   :body-params [value :- s/Str]
                   (ok (i18n-update ctx (keyword lang) (keyword plugin) (keyword key) value))))))
