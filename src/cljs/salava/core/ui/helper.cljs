(ns salava.core.ui.helper
  (:require [reagent.session :as session]
            [clojure.string :as str]
            [schema.core :as s]
            [pushy.core :as pushy]
            [salava.core.helper :as h]
            [ajax.core :as ajax]
            [salava.core.helper :refer [dump]]))

(defn plugin-fun [plugins nspace name]
  (let [fun (fn [p]
              (try
                (apply aget (concat [js/window "salava"] (str/split (h/plugin-str p) #"/") ["ui" nspace name]))
                (catch js/Object _)))]
    (->> plugins
         (map fun)
         (filter #(not (nil? %))))))



(defn collect-plugin-modal-routes
  "A workaround for circular dependencies.
  This function negates the need to require all namespaces where modals are used.
  Modal function is exported from own namespace e.g (defn ^:export modalroute [] {:key fname})"
  [plugins namespaces]
  (let [exported-routes (reduce (fn [r route] (conj r (first (plugin-fun plugins route "modalroute")))) [] namespaces)
        modal-routes (into {} (reduce (fn [r mr] (conj r (mr))) [] exported-routes))]
    modal-routes))

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

(defn route-path [url]
  (subs url (count (base-path))))

(defn base-url []
  (str (.-location.protocol js/window) "//" (.-location.host js/window)))

(defn path-for
  ([url] (path-for url false))
  ([url prevent-cache?]
   (let [time-param (if prevent-cache? (str "?_=" (.now js/Date)))
         url (if (and (not (empty? url)) (not= (subs (str url) 0 1) "/")) (str "/" url) url)]
     (str (base-path) url time-param))))

(defn navigate-to [url]
  (let [history (session/get :history)]
    (pushy/replace-token! history (path-for url))))

(defn js-navigate-to [url]
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
  (if (session/get :private)
    (session/get :private false)
    (session/get-in [:user :private] false)))

(defn not-activated? []
  (not (session/get-in [:user :activated] false)))

(defn url? [s]
  "Pattern Source: https://mathiasbynens.be/demo/url-regex"
  "Pattern author: @diegoperini"
  (let [url-pattern #"(?i)^(?:(?:https?|ftp)://)(?:\S+(?::\S*)?@)?(?:(?!(?:10|127)(?:\.\d{1,3}){3})(?!(?:169\.254|192\.168)(?:\.\d{1,3}){2})(?!172\.(?:1[6-9]|2\d|3[0-1])(?:\.\d{1,3}){2})(?:[1-9]\d?|1\d\d|2[01]\d|22[0-3])(?:\.(?:1?\d{1,2}|2[0-4]\d|25[0-5])){2}(?:\.(?:[1-9]\d?|1\d\d|2[0-4]\d|25[0-4]))|(?:(?:[a-z\u00a1-\uffff0-9]-*)*[a-z\u00a1-\uffff0-9]+)(?:\.(?:[a-z\u00a1-\uffff0-9]-*)*[a-z\u00a1-\uffff0-9]+)*(?:\.(?:[a-z\u00a1-\uffff]{2,}))\.?)(?::\d{2,5})?(?:[/?#]\S*)?$"]
    (when-not (clojure.string/blank? s)
      (not (clojure.string/blank? (re-matches url-pattern s))))))



