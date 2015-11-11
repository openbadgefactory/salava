(ns salava.translator.ui.main
  (:require [ajax.core :as ajax]))


(defn post-url [lang key]
  (let [lang-str (subs (str lang) 1)
        key-str  (subs (str key)  1)]
    (str "/obpv1/translator/" lang-str "/" key-str)))


(defn send-new [lang key value]
  (if-not (nil? value)
    (ajax/POST (post-url lang key) {:params {:value value}})))


(defn show-prompt [e lang key value]
  (do (send-new lang key (js/prompt key value))
      (.preventDefault e)))


(defn get-editable-elem [lang key s]
  (let [out-str (if-not (= s "") s (str "[" key "]"))]
    [:span {:on-context-menu #(show-prompt %1 lang key out-str)} out-str]))



(defn map-editable [get-t lang]
  (fn [k] (if (keyword? k) (get-editable-elem lang k (get-t lang k)) [:span k])))


(defn get-editable[get-t lang keylist]
  (let [elements (mapv (map-editable get-t lang) keylist)]
    (with-meta (vec (cons :span elements))
               {:content (apply str (map last elements))})))
