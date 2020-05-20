(ns salava.badge.pdf-helper
  (:require [clj-pdf-markdown.core :as md :refer [html-tag->clj-pdf] :exclude [render render-children* markdown->clj-pdf wrap-image node-level node-destination node-title node-text-children]]))

(comment
  "adapted from https://github.com/clj-pdf/clj-pdf-markdown")


(defmulti wrap-image
  (fn [pdf-config node result]
    (md/image-alone? pdf-config node)))

(defn- node-level
  [node]
  (->> node bean :level (str "h") keyword))

(defn- node-destination [node] (-> node bean :destination))

(defn- node-title [node] (-> node bean :title))

(defn- node-parent [node] (-> node bean :parent))

(defn- node-parent-class-k [node]
  (-> node node-parent md/commonmark-node->class-k))

(declare render)

(defn render-children* [pdf-config node]
  (mapv (partial render pdf-config) (md/node-children node)))

(defn- node-text-children
  "Recursively walks over the given commonmark-java AST node depth-first,
   extracting and concatenating literals from any text nodes it visits."
  [node]
  (->> (tree-seq (constantly true) md/node-children node)
       (filter #(instance? org.commonmark.node.Text %))
       (map #(.getLiteral %))
       (apply str)))

(defmethod wrap-image :alone
  [pdf-config node result]
  result)

(defmethod wrap-image :default [pdf-config _ result]
  (if (= :Paragraph (node-parent-class-k _))
    result
    [:chunk (md/get-offset-config pdf-config) result]))

(defmulti render
  (fn [pdf-config node]
    (md/commonmark-node->class-k node))
  :hierarchy md/render-hierarchy)

(defmethod render :Document [pdf-config node]
  (->> node
       (render-children* pdf-config)
       (md/wrap-document pdf-config node)))

(defmethod render :Heading [pdf-config node]
 (into
   [:heading (get-in pdf-config [:heading (node-level node)])]
   (render-children* pdf-config node)))

(defmethod render :Paragraph [pdf-config node]
  (->> node
       (render-children* pdf-config)
       (md/wrap-paragraph pdf-config node)))

(defmethod render :Text [pdf-config node]
  (.getLiteral node))

(defmethod render :BulletList [pdf-config node]
  (into [:list (get-in pdf-config [:list :ul])]
        (render-children* pdf-config node)))

(defmethod render :OrderedList [pdf-config node]
  (into [:list (get-in pdf-config [:list :ol])]
        (render-children* pdf-config node)))

(defmethod render :ListItem [pdf-config node]
  (->> node (render-children* pdf-config) first))

(defmethod render :Link [pdf-config node]
  (into [:anchor (merge (:anchor pdf-config) {:target (node-destination node)})]
        (render-children* pdf-config node)))

(defmethod render :Image [pdf-config node]
  (let [annotation [(node-title node) (node-text-children node)]
        config (:image pdf-config)
        offset-config (select-keys config [:x :y])
        image-config (-> config
                         (dissoc :x :y)
                         (merge {:annotation annotation}))
        image-element [:image image-config (node-destination node)]]
    (wrap-image pdf-config node image-element)))

(defmethod render :Emphasis [pdf-config node]
  (into [:phrase {:style :italic}]
        (render-children* pdf-config node)))

(defmethod render :StrongEmphasis [pdf-config node]
  (into [:phrase {:style :bold}]
        (render-children* pdf-config node)))

(defmethod render :ThematicBreak [pdf-config node]
  (let [m (:line pdf-config)]
    (case m :pagebreak [:pagebreak] [:line m])))

(defmethod render :LineBreak [pdf-config node]
  [:spacer (-> pdf-config :spacer :single-value)])

(defmethod render :HtmlBlock [pdf-config node]
  (->> node
       .getLiteral
       (html-tag->clj-pdf pdf-config)
       ;; This preserve line-break count equivalency (parag = 2 breaks)
       (merge [:paragraph (:paragraph pdf-config)])))

(defmethod render :HtmlInline [pdf-config node]
  (->> node .getLiteral (html-tag->clj-pdf pdf-config)))

(defmethod render :Literal [pdf-config node]
  (.getLiteral node))

(defmethod render :default [_ node]

  (.getLiteral node))

(defn markdown->clj-pdf
  "Takes a string of markdown and a renderer configuration and converts the string
  to a hiccup-compatible data structure."
  ([s]
   (markdown->clj-pdf {} s))
  ([pdf-config s]
   (let [config (merge-with md/merge-maybe md/default-pdf-config pdf-config)]
     (->> s
          (md/mark-extra-line-breaks config)
          md/parse-markdown
          (render config)))))
