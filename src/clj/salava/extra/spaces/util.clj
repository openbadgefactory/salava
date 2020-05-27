(ns salava.extra.spaces.util
 (:require [salava.core.util :as u]
  [clojure.string :refer [blank?]]))


(defn save-image! [ctx url]
 (if-not (blank? url) (u/file-from-url-fix ctx url) nil))

(defn uuid [] (str (java.util.UUID/randomUUID)))
