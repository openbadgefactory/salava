(ns salava.metabadge.ui.block
  (:require [salava.metabadge.ui.metabadge :as mb]))

(defn ^:export meta_icon [meta_badge meta_badge_req]
  [mb/metabadgeicon meta_badge meta_badge_req])

(defn ^:export meta_link [param]
  [mb/metabadge param])
