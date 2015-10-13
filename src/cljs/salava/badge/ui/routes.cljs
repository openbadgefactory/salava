(ns salava.badge.ui.routes)

(defn show-badge [{:keys [badge-id]}]
  [:p "Got badge id " badge-id])

(defn ^:export routes [context]
  {"/badge" [["/"        (constantly [:p "My badges"])]
             [["/show/" :badge-id] show-badge]
             ["/import/" (constantly [:p "Import badges"])]
             ["/upload/" (constantly [:p "Upload badges"])]
             ["/export/" (constantly [:p "Export badges"])]
             ["/stats/"  (constantly [:p "Badge stats"])]]})

(defn ^:export navi [context]
  {"/badge/"        {:weight 20 :title "Badges"}
   "/badge/import/" {:weight 21 :title "Import"}
   "/badge/upload/" {:weight 22 :title "Upload"}
   "/badge/export/" {:weight 23 :title "Export"}
   "/badge/stats/"  {:weight 24 :title "Stats"}})

(defn ^:export heading [context]
  {"/badge/" "Badges / My badges"
   "/badge/import/" "Badges / Import"
   "/badge/upload/" "Badges / Upload"
   "/badge/export/" "Badges / Export"
   "/badge/stats/" "Badges / Stats"})
