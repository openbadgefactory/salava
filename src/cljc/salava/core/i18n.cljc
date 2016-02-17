(ns salava.core.i18n
  #?(:cljs (:require-macros [taoensso.tower :as tower-macros]))
     (:require
       #?@(:cljs [[reagent.session :as session]
                  [salava.translator.ui.main :as tr]
                  [taoensso.tower :as tower :refer-macros (with-tscope)]])
       #?(:clj  [taoensso.tower :as tower :refer (with-tscope)])))

(def tconfig
  #?(:clj {:fallback-locale :en
           :dev-mode? true
           :dictionary "i18n/dict.clj"}
     :cljs {:fallback-locale :en
            :compiled-dictionary (tower-macros/dict-compile* "i18n/dict.clj")}))

(def translation (tower/make-t tconfig)) ; create translation fn

(defn get-t [lang key]
  (let [out-str (translation lang key)]
    (if-not (= out-str "")
      out-str
      (str "[" key "]"))))


#?(:clj  (defn t [key] (get-t "en" key))

   :cljs (defn t [& keylist]
           (let [lang (session/get-in [:user :lang])]
             (if (session/get :i18n-editable)
               (tr/get-editable translation lang keylist)
               (apply str (map (fn [k] (if (keyword? k) (get-t lang k) k)) keylist))))))
