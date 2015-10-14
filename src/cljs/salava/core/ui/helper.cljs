(ns salava.core.ui.helper)

(defn unique-values [key data]
  (distinct (flatten (map (keyword key) data))))

(defn current-path []
  (let [uri js/window.location.pathname]
    (str (if (and (not (= "/" uri)) (.endsWith uri "/"))
           (subs uri 0 (dec (count uri)))
           uri)
         js/window.location.search)))
