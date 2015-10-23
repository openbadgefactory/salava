(ns salava.page.ui.helper
  (:require [markdown.core :refer [md->html]]
            [salava.core.i18n :refer [t]]
            [salava.badge.ui.helper :as bh]))

(defn badge-block [{:keys [format image_file name description issued_on criteria_url criteria_markdown issuer_name issuer_url issuer_email]} ]
  [:div {:class "row badge-block"}
   [:div {:class "col-md-4 badge-image"}
    [:img {:src (str "/" image_file)}]]
   [:div {:class "col-md-8"}
    [:div.row
     [:div.col-md-12
      [:h3 name]]]
    [:div.row
     [:div.col-md-12
      (bh/issued-on issued_on)]]
    [:div.row
     [:div.col-md-12
      (bh/issuer-label-and-link issuer_name issuer_url issuer_email)]]
    [:div.row
     [:div.col-md-12 description]]
    [:div.row
     [:div.col-md-12
      [:h3 (t :badge/Criteria)]]]
    [:div.row
     [:div.col-md-12
      [:a {:href criteria_url} (t :badge/Opencriteriapage)]]]
    (if (= format "long")
      [:div.row
       [:div {:class "col-md-12"
              :dangerouslySetInnerHTML {:__html (md->html criteria_markdown)}}]])]])

(defn html-block [{:keys [content]}]
  [:div.html-block
   {:dangerouslySetInnerHTML {:__html content}}])

(defn file-block [block]
  [:div.file-block
   "TODO: fixme"])

(defn heading-block [{:keys [size content]}]
  [:div.heading-block
   (case size
     "h1" [:h1 content]
     "h2" [:h2 content]
     nil)])