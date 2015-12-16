(ns salava.registry
   (:require
     ; SERVER SIDE
     ; list all your compojure-api routes here
     #?@(:clj [[salava.badge.routes]
               [salava.page.routes]
               [salava.gallery.routes]
               [salava.file.routes]
               [salava.user.routes]
               [salava.core.routes]
               [salava.core.plugins :refer [defplugins]]])
     ; CLIENT SIDE
     ; List all your clojurescript route files here:
     #?@(:cljs [[salava.badge.ui.routes]
                [salava.page.ui.routes]
                [salava.gallery.ui.routes]
                [salava.file.ui.routes]
                [salava.user.ui.routes]
                [salava.core.ui.routes]])))

; List all enabled plugins here:
#?(:clj (defplugins enabled :badge :page :gallery :file :user))
