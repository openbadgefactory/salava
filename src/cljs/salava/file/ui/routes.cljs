(ns salava.file.ui.routes)

(defn ^:export routes [context]
  {"/pages" [["/files" (constantly [:p "My files"])]]})

(defn ^:export navi [context]
  {"/pages/files" {:weight 35 :title "Files"}})
