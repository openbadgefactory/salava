(ns salava.core.ui.helper)

(defn unique-values [key data]
  (distinct (flatten (map (keyword key) data))))
