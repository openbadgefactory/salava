(ns salava.gallery.ui.routes
   (:require [salava.core.ui.layout :as layout]
             [salava.core.i18n :as i18n :refer [t]]
             [salava.gallery.ui.badges :as b]
             [salava.gallery.ui.badge-view :as bv]
             [salava.gallery.ui.pages :as p]
             [salava.gallery.ui.profiles :as u]))

(defn placeholder [content]
  (fn [site-navi params]
    #(layout/default site-navi content)))

(defn ^:export routes [context]
  {"/gallery" [[""          b/handler]
               ["/badges"   b/handler]
               [["/badges/" :user-id] b/handler]
               ["/pages"    p/handler]
               [["/pages/" :user-id] p/handler]
               ["/profiles" u/handler]
               [["/badgeview/" :badge-content-id] bv/handler]
               ["/getbadge" (placeholder [:p "Apply for a badge"])]]})

(defn ^:export navi [context]
  {"/gallery"          {:weight 40 :title (t :gallery/Gallery)        :top-navi true  :breadcrumb (t :gallery/Gallery " / " :gallery/Sharedbadges)}
   "/gallery/badges"   {:weight 41 :title (t :gallery/Sharedbadges)   :site-navi true :breadcrumb (t :gallery/Gallery " / " :gallery/Sharedbadges)}
   "/gallery/pages"    {:weight 42 :title (t :gallery/Sharedpages)    :site-navi true :breadcrumb (t :gallery/Gallery " / " :gallery/Sharedpages)}
   "/gallery/profiles" {:weight 43 :title (t :gallery/Sharedprofiles) :site-navi true :breadcrumb (t :gallery/Gallery " / " :gallery/Sharedprofiles)}
   "/gallery/getbadge" {:weight 44 :title (t :gallery/Applybadge)     :site-navi true :breadcrumb (t :gallery/Gallery " / " :gallery/Applybadge)}
   "/gallery/badges/\\d+" {:breadcrumb (t :gallery/Gallery " / " :gallery/Sharedbadges)}
   "/gallery/pages/\\d+" {:breadcrumb (t :gallery/Gallery " / " :gallery/Sharedpages)}
   "/gallery/badgeview/\\w+" {:breadcrumb (t :gallery/Gallery " / " :gallery/Viewbadge)}})

