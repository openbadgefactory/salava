(ns salava.core.i18n
  (:require [clojure.tools.logging :as log]
            [clojure.java.io :as io]
            [salava.core.util :as util]))


(def config (-> (io/resource "config/core.edn") slurp read-string))

(def plugins (cons :core (:plugins config)))

(def languages (:languages config))

(def dict-file (io/resource "i18n/dict.clj"))

;;;

(defn prop-file [lang plugin]
  (io/resource (str "i18n/" lang "/" (name plugin) ".properties")))




;;;


(defn dict-to-prop [& args]
  (log/info "converting dictionary map to properties")
  (System/exit 0))

(defn prop-to-dict [& args]
  (log/info "parsing properties files to dictionary map")
  (System/exit 0))
