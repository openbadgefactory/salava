(ns salava.user.ui.routes)

(defn ^:export routes [context]
  {"/user" [["/login/"   (constantly [:p "Login page"])]
            ["/account/" (constantly [:p "IMy account"])]]})


(defn ^:export navi [context] {})

(defn ^:export heading [context] {})