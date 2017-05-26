(ns salava.core.ui.content-language
  (:require [reagent.session :as session]))

(defn init-content-language
  "chose language between user-lng and default lng"
  [contents]
  (let [user-lng (session/get-in [:user :language])]
    (if (some #(= user-lng (:language-code %)) contents)
      user-lng
      (:language-code (first (filter :default contents))))))

(defn content-language-selector [content-language-atom contents]
  (if (< 1 (count contents))
      (into [:div.badge-language-selector]
            (for [content contents]
              [:a {:href "#" :class (if (= @content-language-atom (:language-code content)) "chosen" "") :on-click #(reset! content-language-atom (:language-code content))} (str (:language-name content))]))
      [:div.badge-language-selector]))


(defn content-setter
  "Show right language content or default content"
  [selected-language content]
  (let [filtered-content (first (filter #(= (:language-code %) selected-language) content))]
    (if filtered-content
      filtered-content
      (first (filter :default content)))))
