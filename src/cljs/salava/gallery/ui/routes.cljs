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
  {"/gallery"          {:weight 40 :title (t :gallery/Gallery)          :breadcrumb (str (t :gallery/Gallery) " / " (t :gallery/Sharedbadges))}
   "/gallery/badges"   {:weight 41 :title (t :gallery/Sharedbadges)     :breadcrumb (str (t :gallery/Gallery) " / " (t :gallery/Sharedbadges))}
   "/gallery/pages"    {:weight 42 :title (t :gallery/Sharedpages)      :breadcrumb (str (t :gallery/Gallery) " / " (t :gallery/Sharedpages))}
   "/gallery/profiles" {:weight 43 :title (t :gallery/Sharedprofiles)   :breadcrumb (str (t :gallery/Gallery) " / " (t :gallery/Sharedprofiles))}
   "/gallery/getbadge" {:weight 44 :title (t :gallery/Applybadge)       :breadcrumb (str (t :gallery/Gallery) " / " (t :gallery/Applybadge))}})

