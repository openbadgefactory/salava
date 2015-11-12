(ns salava.page.themes
  (:require [salava.core.i18n :refer [t]]))

(def default-theme-id 0)

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

(defn valid-theme-id [theme-id]
 (if (some #(= theme-id (:id %)) themes)
  theme-id
  default-theme-id))