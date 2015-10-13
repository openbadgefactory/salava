(ns salava.gallery.ui.routes)

(defn ^:export routes [context]
  {"/gallery" [["/"          (constantly [:p "Badge gallery"])]
               ["/badges/"   (constantly [:p "Badge gallery"])]
               ["/pages/"    (constantly [:p "Page gallery"])]
               ["/profiles/" (constantly [:p "User gallery"])]
               ["/getbadge/" (constantly [:p "Apply for a badge"])]]})

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
