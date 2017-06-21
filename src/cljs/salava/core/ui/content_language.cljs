(ns salava.core.ui.content-language
  (:require [reagent.session :as session]
            [salava.core.i18n :refer [lang-lookup]]
            [clojure.string :refer [capitalize]]
            [salava.core.helper :refer [dump]]))

(defn init-content-language
  "chose language between user-lng and default lng"
  [contents]
  (let [user-lng (session/get-in [:user :language])]
    (if (some #(= user-lng (:language_code %)) contents)
      user-lng
      (:language-code (first (filter #(= (:language_code %) (:default_language_code %)) contents))))))

(defn content-language-selector [content-language-atom contents]
  
  (if (< 1 (count contents))
      (into [:div.badge-language-selector]
            (for [content contents]
              [:a {:href "#" :class (if (= @content-language-atom (:language_code content)) "chosen" "") :on-click #(reset! content-language-atom (:language_code content))} (str  (or (capitalize (get lang-lookup (:language_code content))) (:language_code content)))]))
      [:div.badge-language-selector]))


(defn content-setter
  "Show right language content or default content"
  [selected-language content]
  (let [filtered-content (first (filter #(= (:language_code %) selected-language) content))]
    (if filtered-content
      filtered-content
      (first (filter  #(= (:language_code %) (:default_language_code %)) content)))))
