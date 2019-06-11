(ns salava.extra.application.ui.helper
  (:require
   [reagent.core :refer [atom cursor create-class]]
   [reagent.session :as session]
   [salava.core.i18n :refer [t]]
   [clojure.string :refer [join blank?]]
   [dommy.core :as dommy :refer-macros [sel sel1]]))

(defn application-plugin? []
  (some #(= "extra/application" %) (session/get :plugins)))

(defn url-builder [params state]
  (let [params (remove (fn [[k v]] (blank? v)) params)
        query (join (cons (str (name (key (first params)))"="(val (first params)))
                          (map #(str (if (not (coll? (val %)))
                                       (str "&"(name (key %)) "=" (val %))
                                       (if (empty? (val %)) "" (if (empty? (rest (val %)))
                                                                 (str "&"(name (key %)) "[0]="(first (val %)))
                                                                 (join (cons (str "&"(name (key %)) "[0]="(first (val %)))
                                                                             (map (fn [e] (str "&"(name (key %)) "[" (.indexOf (val %) e) "]=" e)) (rest (val %)))))))))
                               (rest params))))]
    (.replaceState js/history "" "Badge Gallery" (str "?" query))
    (swap! state assoc :query-param (.-href js/window.location))))
