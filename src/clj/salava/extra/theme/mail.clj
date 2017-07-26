(ns salava.extra.theme.mail
  (:require [salava.core.i18n :refer [t]]
            [salava.core.util :refer [get-site-url]]))







(defn html-mail-banner [ctx]
  (let [site-url (get-site-url ctx)
        banner (get-in ctx [:config :extra/theme :mail-banner] nil)
        site-name (get-in ctx [:config :core :site-name])]
    [:div
     {:style
      "margin-top: 10px;margin-bottom: 50px;padding-top: 0;padding-bottom: 0;"}
     [:a {:href site-url :target "_blank" :style "text-decoration: none;"}
      [:img.banner
         {:alt    site-name,
          :style  "max-width: 640px;",
          :src    banner,
          :height "auto",
          :width  "auto"}]]]))

(defmulti get-fragment #(last %&))

(defmethod get-fragment "mail-banner" [ctx user lng type]
  (html-mail-banner ctx))
