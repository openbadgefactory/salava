(ns salava.gallery.ui.routes
   (:require [salava.core.ui.layout :as layout]))

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
  {"/gallery/"          {:weight 40 :title "Gallery"}
   "/gallery/badges/"   {:weight 41 :title "Shared badges"}
   "/gallery/pages/"    {:weight 42 :title "Shared pages"}
   "/gallery/profiles/" {:weight 43 :title "Shared profiles"}
   "/gallery/getbadge/" {:weight 44 :title "Apply for a badge"}})

(defn ^:export heading [context]
  {"/gallery/"          "Gallery / Gallery"
   "/gallery/badges/"   "Gallery / Shared badges"
   "/gallery/pages/"    "Gallery / Shared pages"
   "/gallery/profiles/" "Gallery / Shared profiles"
   "/gallery/getbadge/" "Gallery / Apply for a badge"})
