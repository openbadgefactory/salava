(ns salava.page.ui.edit
  (:require [reagent.core :refer [atom cursor]]
            [reagent.session :as session]
            [clojure.walk :as walk :refer [keywordize-keys]]
            [ajax.core :as ajax]
            [salava.core.ui.layout :as layout]
            [salava.core.i18n :as i18n :refer [t]]
            [salava.core.helper :refer [dump]]
            [salava.page.ui.helper :as ph]))

(defn update-block-value [block-atom key value]
  (swap! block-atom assoc key value))

(defn select-badge [block-atom badges id]
  (let [badge (some #(if (= (:id %) id) %) badges)]
    (update-block-value block-atom :badge badge)))

(defn edit-block-badges [block-atom badges]
  (let [badge-id (get-in @block-atom [:badge :id] 0)
        image (get-in @block-atom [:badge :image_file])
        format (:format @block-atom)]
    [:div.form-group
     [:div.col-xs-8
      [:div.badge-select
       [:select {:class "form-control"
                 :value badge-id
                 :on-change #(select-badge block-atom @badges (js/parseInt (.-target.value %)))}
        [:option {:value 0} (t "-" :page/none "-")]
        (for [badge @badges]
          [:option {:value (:id badge)}
           (:name badge)])]]
      [:div.badge-select
       [:select {:class "form-control"
                 :value format
                 :on-change #(update-block-value block-atom :format (.-target.value %))}
        [:option {:value "short"} (t :page/Short)]
        [:option {:value "long"} (t :page/Long)]]]]
     [:div {:class "col-xs-4 badge-image"}
      (if image
        [:img {:src (str "/" image)}])]]))

(defn edit-block-files [block-atom])

(defn edit-block-text [block-atom]
  (let [content (:content @block-atom)]
    [:div.form-group
     [:div.col-md-12
      [:input {:class "form-control"
               :type "text"
               :value content
               :on-change #(update-block-value block-atom :content (.-target.value %))}]]]))

(defn block-type [block-atom]
  (let [type (:type @block-atom)]
    [:div
     [:select {:class "form-control"
               :value type
               :on-change #(update-block-value block-atom :type (.-target.value %))}
      [:option {:value "heading"} (t :page/Heading)]
      [:option {:value "sub-heading"} (t :page/Subheading)]
      [:option {:value "badge"} (t :page/Badge)]
      [:option {:value "badge-group"} (t :page/Badgegroup)]
      [:option {:value "html"} (t :page/Html)]
      [:option {:value "file"} (t :page/File)]]]))

(defn block [block-atom badges]
  (let [{:keys [type values]} @block-atom]
    [:div.block
     [:div.block-move]
     [:div.block-content
      [:div.form-group
       [:div.col-xs-8
        [block-type block-atom]]
       [:div {:class "col-xs-4 block-remove"}
        [:span {:class "remove-button"}
         [:i {:class "fa fa-close"}]]]]
      (case type
        ("heading" "sub-heading" "html") (edit-block-text block-atom)
        ("badge" "badge-group") (edit-block-badges block-atom badges)
        ("file") (edit-block-files block-atom)
        nil)]]))

(defn page-blocks [blocks badges]
  (into [:div {:id "page-blocks"}]
        (for [index (range (count @blocks))]
          (block (cursor blocks [index]) badges))))

(defn page-description [description]
  [:div.form-group
   [:label {:class "col-md-2"
            :for "page-description"}
    (t :page/Description)]
   [:div.col-md-10
    [:textarea {:id "page-description"
                :class "form-control"
                :value @description
                :on-change #(reset! description (.-target.value %))}]]])

(defn page-title [name]
  [:div.form-group
   [:label {:class "col-md-2"
            :for "page-name"}
     (t :page/Title)]
   [:div.col-md-10
    [:input {:id "page-name"
             :class "form-control"
             :type "text"
             :value @name
             :on-change #(reset! name (.-target.value %))}]]])

(defn content [state]
  (let [{:keys [id name description blocks]} (:page @state)]
    [:div {:id "page-edit"}
     [:div.row
      [:div.col-sm-12
       [:h1 (t :page/Editpage ": " name)]]]
     [:div.row
      [:div.col-xs-8
       [:a {:class "btn btn-active"}
        (t "1." :page/Content)]
       [:a {:class "btn"}
        (t "2." :page/Theme)]
       [:a {:class "btn"}
        (t "." :page/Settings)]
       [:a {:class "btn"}
        (t "4." :page/Preview)]]
      [:div {:class "col-xs-4 buttons-right"}
       [:a {:class "btn btn-primary"}
        (t :page/View)]
       [:a {:class "btn btn-warning"}
        (t :page/Delete)]]]
     [:form.form-horizontal
      [:div {:id "title-and-description"}
       [page-title (cursor state [:page :name])]
       [page-description (cursor state [:page :description])]]
      [page-blocks (cursor state [:page :blocks]) (cursor state [:badges])]]]))

(defn init-data [state id]
  (ajax/GET
    (str "/obpv1/page/edit/" id)
    {:handler (fn [data]
                (let [data-with-kws (keywordize-keys data)]
                  (reset! state data-with-kws)))}))

(defn handler [site-navi params]
  (let [id (:page-id params)
        state (atom {:page {:blocks []
                            :name ""
                            :description ""
                            :id nil}
                     :badges []})]
    (init-data state id)
    (fn []
      (layout/default site-navi (content state)))))
