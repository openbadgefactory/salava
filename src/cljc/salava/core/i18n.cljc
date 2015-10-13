(ns salava.core.i18n
  #?(:cljs (:require-macros [taoensso.tower :as tower-macros]))
     (:require
       #?@(:cljs [[reagent.session :as session]
                  [taoensso.tower :as tower :refer-macros (with-tscope)]])
       #?(:clj  [taoensso.tower :as tower :refer (with-tscope)])))

(def tconfig
  #?(:clj {:fallback-locale :en
           :dev-mode? true
           :dictionary "i18n/dict.clj"}
     :cljs {:fallback-locale :en
            :compiled-dictionary (tower-macros/dict-compile* "i18n/dict.clj")}))

(def translation (tower/make-t tconfig)) ; create translation fn

(defn t [key]
  (let [lang #?(:clj "en"
                :cljs (session/get :lang))]
    (translation lang key)))
