(ns salava.registry
   (:require
     ; SERVER SIDE
     #?@(:clj [[salava.core.helper]])
     ; CLIENT SIDE
     ; List all your clojurescript route files here:
     #?@(:cljs [[salava.badge.ui.routes]
                [salava.page.ui.routes]
                [salava.gallery.ui.routes]
                [salava.file.ui.routes]
                [salava.user.ui.routes]
                [salava.displayer.ui.routes]
                [salava.oauth.ui.routes]
                [salava.core.ui.routes]
                [salava.social.ui.routes]
                [salava.admin.ui.routes]
                [salava.registerlink.ui.routes]
                [salava.extra.cancred.ui.routes]
                [salava.extra.application.ui.routes]
                [salava.extra.passport.ui.routes]
					 [salava.extra.kirkwood.ui.routes]])))

