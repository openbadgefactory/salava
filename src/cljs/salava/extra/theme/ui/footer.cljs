(ns salava.extra.theme.ui.footer
  (:require [reagent.session :as session]
            [salava.core.helper :refer [dump]]
            [salava.core.i18n :refer [t]]))


(defn link [text link]
  [:a {:key text :class "bottom-link" :href link}
        text])

(defn email [email]
  [:a {:key email :class "bottom-link" :href (str "mailto: " email "?Subject=Contact%20request") }
      email])

(defn footer-element []
  (let [rows      (session/get :footer)
        helper-fn (fn [item]
                    (do
                      (case (:type item)
                        "email" (email (:text item))
                        "link"  (link (:text item) (:link item))
                        "text"  (str (:text item))
                        "default")
                      ))]
    [:footer.footer
     (into [:div.footer-container
            (for [items rows]
              (into [:p {:class "text-muted" :key (hash items)}]
                    (interpose " | "  (map helper-fn items))
                    
                    ))])]))
