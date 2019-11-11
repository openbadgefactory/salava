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
                [salava.extra.socialuser.ui.routes]
                [salava.extra.kirkwood.ui.routes]
                [salava.extra.theme.ui.routes]
                [salava.extra.hpi.ui.routes]
                [salava.extra.msftembo.ui.routes]
                [salava.extra.hpass.ui.routes]
                [salava.extra.badgbl.ui.routes]
                [salava.extra.boat.ui.routes]
                [salava.metabadge.ui.routes]
                [salava.connections.ui.routes]
                [salava.profile.ui.routes]
                [salava.location.ui.routes]])))
