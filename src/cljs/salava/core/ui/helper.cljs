(ns salava.core.ui.helper
  (:require [reagent.session :as session]
            [schema.core :as s]
            [ajax.core :as ajax]))

(defn unique-values [key data]
  (->> data
       (map (keyword key))
       (filter some?)
       flatten
       distinct))

(defn base-path
  ([] (session/get :base-path ""))
  ([ctx] (:base-path ctx "")))

(defn current-path []
  (let [uri js/window.location.pathname]
    (str (if (and (not (= "/" uri)) (.endsWith uri "/"))
           (subs uri 0 (dec (count uri)))
           uri)
         js/window.location.search)))

(defn current-route-path []
  (if (base-path)
    (subs (current-path) (count (base-path)))
    (current-path)))

(defn base-url []
  (str (.-location.protocol js/window) "//" (.-location.host js/window)))

(defn path-for
  ([url] (path-for url false))
  ([url prevent-cache?]
   (let [time-param (if prevent-cache? (str "?_=" (.now js/Date)))
         url (if (and (not (empty? url)) (not= (subs (str url) 0 1) "/")) (str "/" url) url)]
     (str (base-path) url time-param))))

(defn navigate-to [url]
  (set! (.-location.href js/window) (path-for url)))

(defn input-valid? [schema input]
  (try
    (s/validate schema input)
    (catch :default e
      false)))

(defn hyperlink [text]
  (let [url (if (or (re-find #"^https?://" (str text)) (re-find #"^http?://" (str text))) text (str "http://" text))]
    [:a {:href url
         :target "_blank"} (str text)]))

(defn str-cat [a-seq]
  (if (empty? a-seq)
    ""
    (let [str-space (fn [str1 str2]
                      (str str1 ", " str2))]
      (reduce str-space a-seq))))

(defn private? []
  (if (session/get :user)
    (session/get-in [:user :private] false)
    (session/get :private false)))
