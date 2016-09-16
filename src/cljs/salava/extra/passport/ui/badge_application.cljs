(ns salava.extra.passport.ui.badge-application
  (:require [reagent.core :refer [atom]]
            [markdown.core :refer [md->html]]
            [salava.core.ui.layout :as layout]
            [reagent.session :as session]
            [salava.core.helper :refer [dump]]
            [salava.core.i18n :as i18n :refer [t]]))



(defn content [state]
  (let [user-lng (session/get-in [:user :language])
        obp-badge-iframe-en (str "<iframe width=\"250\" height=\"250\" src=\"https://openbadgefactory.com/c/earnablebadge/NM6K68e7HCeI/embed\" frameborder=\"0\"></iframe>")
        obp-badge-iframe-fi (str "<iframe width=\"250\" height=\"250\" src=\"https://openbadgefactory.com/c/earnablebadge/NM6JZVe7HCeH/embed\" frameborder=\"0\"></iframe>")]
    [:div
      {:dangerouslySetInnerHTML {:__html (md->html (if (= "fi" user-lng)obp-badge-iframe-fi obp-badge-iframe-en))}}]))

(defn handler [site-navi]
  (let [state (atom {})]
    (fn []
      (layout/default site-navi (content state)))))
