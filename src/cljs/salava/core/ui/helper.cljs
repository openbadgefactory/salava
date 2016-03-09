(ns salava.core.ui.helper
  (:require [schema.core :as s]
            [ajax.core :as ajax]))

(defn unique-values [key data]
  (->> data
       (map (keyword key))
       (filter some?)
       flatten
       distinct))

(defn current-path []
  (let [uri js/window.location.pathname]
    (str (if (and (not (= "/" uri)) (.endsWith uri "/"))
           (subs uri 0 (dec (count uri)))
           uri)
         js/window.location.search)))

(defn base-url []
  (str (.-location.protocol js/window) "//" (.-location.host js/window)))

(defn navigate-to [url]
  (set! (.-location.href js/window) url))

(defn input-valid? [schema input]
  (try
    (s/validate schema input)
    (catch js/Error e
      false)))

(defn set-meta-tags [title description image]
  (-> (js/$ "meta[property=\"og:title\"]") (.attr "content" title))
  (-> (js/$ "meta[property=\"og:description\"]") (.attr "content" description))
  (-> (js/$ "meta[name=\"description\"]") (.attr "content" description))
  (-> (js/$ "meta[property=\"og:image\"]") (.attr "content" image)))