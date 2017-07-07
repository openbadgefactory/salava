(ns salava.extra.theme.mail
  (:require [salava.core.i18n :refer [t]]
            [salava.core.util :refer [get-site-url]]))







(defn html-mail-banner [ctx]
  (let [site-url (get-site-url ctx)]
    [:div
     {:style
      "margin-top: 10px;margin-bottom: 50px;padding-top: 0;padding-bottom: 0;"}
     [:a {:href site-url :target "_blank" :style "text-decoration: none;"}
      [:img.banner
       {:alt    "Open Badge Passport",
        :style  "max-width: 640px;",
        :src    (str site-url "/img/extra/passport/logo.png"),
        :height "auto",
        :width  "auto"}]]]))

(defmulti get-fragment #(last %&))

(defmethod get-fragment "mail-banner" [ctx user lng type]
  (html-mail-banner ctx))
