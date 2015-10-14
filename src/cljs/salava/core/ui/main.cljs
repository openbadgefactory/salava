(ns salava.core.ui.main
  (:require [reagent.core :as reagent]
            [salava.core.ui.dispatch :as d]))

(defn init! []
  (reagent/render [(d/main-view)] (js/document.getElementById "app")))

(init!)
