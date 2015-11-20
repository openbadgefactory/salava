(ns salava.page.themes
  (:require [salava.core.i18n :refer [t]]))

(def default-theme-id 0)

(def default-border-id 1)

(def themes
  ; List of available page themes.

  ; Each theme should have styles in page SCSS-file (src/scss/page.scss),
  ; Example:
  ; #theme-%THEME-ID% {
  ;     font: 400 24px "Halant" !important;
  ;     font-size: 16px;
  ;     line-height: 17px;
  ;     color: #000000;
  ;     .panel-left {
  ;         background-image: url ('/img/themes/%THEME-NAME%_left.png');
  ;     }
  ;    .panel-right {
  ;        background-image: url ('/img/theme/%THEME-NAME%_right.png');
  ;     }
  ; }
  [{:id 0 :name (t :page/Defaulttheme)}
   {:id 1 :name (t :page/Customtheme)}
   {:id 2 :name (t :page/Classictheme)}
   {:id 3 :name (t :page/Handmadetheme)}
   {:id 4 :name (t :page/Arttheme)}
   {:id 5 :name (t :page/Cardboardtheme)}
   {:id 6 :name (t :page/Legotheme)}
   {:id 7 :name (t :page/Diamondstheme)}
   {:id 8 :name (t :page/Ornamenttheme)}
   {:id 9 :name (t :page/Vintagetheme)}
   {:id 10 :name (t :page/Colourstheme)}
   {:id 11 :name (t :page/Dottedtheme)}
   {:id 12 :name (t :page/Scripttheme)}
   {:id 13 :name (t :page/Legoextremetheme)}])

(def borders
 [{:id 0 :style "none" :width 0 :color "#ffffff"}
  {:id 1 :style "solid" :width 1 :color "#dddddd"}
  {:id 2 :style "dashed" :width 1 :color "#dddddd"}
  {:id 3 :style "dotted" :width 1 :color "#dddddd"}
  {:id 4 :style "solid" :width 1 :color "#999999"}
  {:id 5 :style "dashed" :width 1 :color "#999999"}
  {:id 6 :style "dotted" :width 1 :color "#999999"}
  {:id 7 :style "solid" :width 1 :color "#333333"}
  {:id 8 :style "dashed" :width 1 :color "#333333"}
  {:id 9 :style "dotted" :width 1 :color "#333333"}
  {:id 10 :style "solid" :width 2 :color "#dddddd"}
  {:id 11 :style "dashed" :width 2 :color "#dddddd"}
  {:id 12 :style "dotted" :width 2 :color "#dddddd"}
  {:id 13 :style "solid" :width 2 :color "#999999"}
  {:id 14 :style "dashed" :width 2 :color "#999999"}
  {:id 15 :style "dotted" :width 2 :color "#999999"}
  {:id 16 :style "solid" :width 2 :color "#333333"}
  {:id 17 :style "dashed" :width 2 :color "#333333"}
  {:id 18 :style "dotted" :width 2 :color "#333333"}])

(defn valid-theme-id [theme-id]
 (if (some #(= theme-id (:id %)) themes)
  theme-id
  default-theme-id))

(defn valid-border-id [border-id]
 (if (some #(= border-id (:id %)) borders)
  border-id
  default-border-id))

(defn border-attributes [border-id]
 (->> borders
      (filter #(= border-id (:id %)))
      first))