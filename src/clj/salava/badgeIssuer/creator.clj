(ns salava.badgeIssuer.creator
  (:import [java.awt Graphics2D Color Font Polygon BasicStroke GraphicsEnvironment GradientPaint RenderingHints]
           [java.awt.image BufferedImage]
           [javax.imageio ImageIO])

  (:require [slingshot.slingshot :refer :all]
            [salava.badgeIssuer.db :as db]
            [salava.badgeIssuer.util :refer [image->base64str]]
            [salava.core.util :as util :refer [plugin-fun get-plugins]]
            [clojure.java.io :as io :refer [as-url]]
            [clojure.tools.logging :as log]))

(defn fonts []
 (let [f (GraphicsEnvironment/getLocalGraphicsEnvironment)]
   (->> f (.getAllFonts) (map #(.getFontName %)))))

(def settings {:width 120
               :height 120
               :base [Color/BLACK Color/WHITE]
               :fonts (fonts)
               :base-polygon-point 10
               :font-style [Font/ITALIC Font/BOLD Font/PLAIN]
               :font-size (range 30 50)})

(def default-points
 (let [{:keys [width height]} settings]
   {:x1 0
    :y1 0
    :x2 width
    :y2 height}))

(def canvas (BufferedImage. (:width settings) (:height settings) BufferedImage/TYPE_INT_ARGB))

(defn make-name [ctx user-id]
 (let [user-info (as-> (first (plugin-fun (get-plugins ctx) "db" "user-information")) f (if f (f ctx user-id) {}))
       {:keys [first_name last_name ]} user-info
       initials (str (first first_name) (first last_name))
       names [initials
              (clojure.string/upper-case initials)
              (clojure.string/lower-case initials)
              first_name
              (clojure.string/capitalize first_name)
              last_name
              (clojure.string/capitalize last_name)]]
  (if (seq names) (rand-nth names) "")))

(defn- rand-num [start end]
  (+ start (rand-int (- end start))))

(defn- hexagon [])

(defn- draw-polygon [n]
  (let [{:keys [width height base-polygon-point]} settings
        polygon-points (if n n (-> (rand-num 2 7) inc))
        xpoints (take polygon-points (repeatedly #(rand-num (- base-polygon-point) (- width 10))))
        ypoints (take polygon-points (repeatedly #(rand-num (- base-polygon-point) (- height 10))))]
    {:points
     (mapv #(assoc {} :x %1 :y %2) xpoints ypoints)}))

(defn shape-coll []
 (let [{:keys [width height]} settings
       n (rand-num 0 10)
       height (- height n)]
   [{:points [{:x (/ width 2) :y 0} {:x width :y height} {:x 0 :y height}]}; triangle])
    {:points [{:x 0 :y 0} {:x (/ width 2) :y height} {:x width :y 0}]}
    {:points [{:x 0 :y (/ height 2)} {:x (/ width 2) :y 0} {:x width :y (/ height 2)} {:x width :y 0}]}
    (draw-polygon nil)]))

(defn- make-color
  "generate random rgb color values"
  []
  (let [base  (rand-nth (:base settings))
        r (int (/ (+ (rand-int 255) (.getRed base)) 2))
        g (int (/ (+ (rand-int 255) (.getGreen base)) 2))
        b (int (/ (+ (rand-int 255) (.getBlue base)) 2))]

    (Color. r g b)))

(defn- get-color []
  (let [{:keys [width height]} settings
        rand (rand-num 0 3)
        gradient (GradientPaint.
                  (rand-num 0 (/ height 2))
                  (rand-num 0 (/ width 2))
                  (make-color)
                  (rand-num 0 height)
                  (rand-num 0 width)
                  (make-color))
        gradient_2 (GradientPaint.
                    (rand-num 0 height)
                    (rand-num 0 width)
                    (make-color)
                    (rand-num 0 height)
                    (rand-num 0 width)
                    (make-color))]
    (case rand
      0 (make-color)
      1 gradient
      2 gradient_2
      (make-color))))

(defn shape-shift
  "randomly generate filled shape"
  [g]
  (let [_ (rand-num 0 5)
        {:keys [x1 x2 y1 y2]} default-points]
    (case _
      1 (.fillRect g x1 y1 x2 y2)
      2 (.fillOval g x1 y1 x2 y2)
      3 (.fillPolygon g (let [polygon (rand-nth (shape-coll))
                              pgon (new Polygon)]
                         (doseq [p (:points polygon)]
                           (. pgon (addPoint (:x p) (:y p))))
                         pgon))
      (.drawPolygon g (let [polygon (rand-nth (shape-coll))
                            pgon (new Polygon)]
                        (doseq [p (:points polygon)]
                          (. pgon (addPoint (:x p) (:y p))))
                        pgon)))))

(defn write-text [g ctx user-id]
  (let [{:keys [width height base font-size font-style fonts]} settings
        add-font? (= (rand-num 0 20) 15)
        font (when add-font? (Font. (rand-nth fonts) (rand-nth font-style) (rand-nth font-size)))]
      (.setFont g font)
      (.setColor g (make-color))
      (.drawString g (make-name ctx user-id) 65 65)))

(defn rendering-hints [g]
  (doall
    [(.setRenderingHint g
       (RenderingHints/KEY_ANTIALIASING)
       (RenderingHints/VALUE_ANTIALIAS_ON))
     (.setRenderingHint g
       (RenderingHints/KEY_INTERPOLATION)
       (RenderingHints/VALUE_INTERPOLATION_BICUBIC))
     (.setRenderingHint g
       (RenderingHints/KEY_RENDERING)
       (RenderingHints/VALUE_RENDER_QUALITY))]))


(defn generate-image [ctx user]
  (try+
    (let [{:keys [width height base font-size font-style fonts]} settings
          g (.createGraphics canvas)
          r (rand-num 1 10)
          stroke (BasicStroke. r)]
      (doto g
        (rendering-hints)
        (.setBackground (rand-nth base))
        (.clearRect 0 0 width height)
        (.setPaint (get-color))
        (.setStroke stroke)
        (shape-shift))
      (if-let [url (image->base64str canvas)]
        {:status "success" :url url}
        (throw+ {:status "error" :url "" :message (log/error "Error generating image")})))
    (catch Object _
     (log/error "Error generating image: " (.getMessage _))
     {:url "" :status "error" :message (str "Error generating image: " (.getMessage _))})))
