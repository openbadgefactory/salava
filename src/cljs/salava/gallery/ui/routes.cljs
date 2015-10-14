(ns salava.gallery.ui.routes
   (:require [salava.core.ui.layout :as layout]
            [salava.core.i18n :as i18n :refer [t]]))

(defn placeholder [content]
  (fn [site-navi params]
    #(layout/default site-navi content)))

(defn ^:export routes [context]
  {"/gallery" [[""          (placeholder [:p "Badge gallery"])]
               ["/badges"   (placeholder [:p "Badge gallery"])]
               ["/pages"    (placeholder [:p "Page gallery"])]
               ["/profiles" (placeholder [:p "User gallery"])]
               ["/getbadge" (placeholder [:p "Apply for a badge"])]]})

(defn ^:export navi [context]
  {"/gallery"          {:weight 40 :title "Gallery"           :breadcrumb (str (t :gallery/gallery) " / " ) }
   "/gallery/badges"   {:weight 41 :title "Shared badges"     :breadcrumb (str (t :gallery/gallery) " / " (t :gallery/sharedbadges))}
   "/gallery/pages"    {:weight 42 :title "Shared pages"      :breadcrumb (str (t :gallery/gallery) " / " (t :gallery/sharedpages))}
   "/gallery/profiles" {:weight 43 :title "Shared profiles"   :breadcrumb (str (t :gallery/gallery) " / " (t :gallery/sharedprofiles))}
   "/gallery/getbadge" {:weight 44 :title "Apply for a badge" :breadcrumb (str (t :gallery/gallery) " / " (t :gallery/applybadge))}})

