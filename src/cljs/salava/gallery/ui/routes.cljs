(ns salava.gallery.ui.routes
   (:require [salava.core.ui.layout :as layout]
             [salava.core.i18n :as i18n :refer [t]]
             [salava.gallery.ui.badges :as b]
             [salava.gallery.ui.pages :as p]))

(defn placeholder [content]
  (fn [site-navi params]
    #(layout/default site-navi content)))

(defn ^:export routes [context]
  {"/gallery" [[""          b/handler]
               ["/badges"   b/handler]
               [["/badges/" :user-id] b/handler]
               ["/pages"    (placeholder [:p "Page gallery"])]
               ["/profiles" (placeholder [:p "User gallery"])]
               ["/getbadge" (placeholder [:p "Apply for a badge"])]]})

(defn ^:export navi [context]
  {"/gallery"          {:weight 40 :title (t :gallery/Gallery)          :breadcrumb (t :gallery/Gallery " / " :gallery/Sharedbadges)}
   "/gallery/badges"   {:weight 41 :title (t :gallery/Sharedbadges)     :breadcrumb (t :gallery/Gallery " / " :gallery/Sharedbadges)}
   "/gallery/pages"    {:weight 42 :title (t :gallery/Sharedpages)      :breadcrumb (t :gallery/Gallery " / " :gallery/Sharedpages)}
   "/gallery/profiles" {:weight 43 :title (t :gallery/Sharedprofiles)   :breadcrumb (t :gallery/Gallery " / " :gallery/Sharedprofiles)}
   "/gallery/getbadge" {:weight 44 :title (t :gallery/Applybadge)       :breadcrumb (t :gallery/Gallery " / " :gallery/Applybadge)}})

