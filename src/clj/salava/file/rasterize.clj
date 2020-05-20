(ns salava.file.rasterize
  (:require [clojure.java.io :as io]
            [clojure.string :as string])
  (:import [java.awt RenderingHints]
           [java.io File ByteArrayInputStream ByteArrayOutputStream]
           [java.nio.charset StandardCharsets]
           [org.apache.batik.anim.dom SAXSVGDocumentFactory]
           [org.apache.batik.transcoder TranscoderInput TranscoderOutput SVGAbstractTranscoder]
           [org.apache.batik.transcoder.image PNGTranscoder]))

;; SVG rasterizing functions, copied from https://github.com/HausnerR/batik-rasterize

(defn- get-high-quality-hints []
  (let [add-hint (fn [hints k v] (.add hints (RenderingHints. k v)))]
    (doto (RenderingHints. nil nil)
      (add-hint RenderingHints/KEY_ALPHA_INTERPOLATION RenderingHints/VALUE_ALPHA_INTERPOLATION_QUALITY)
      (add-hint RenderingHints/KEY_INTERPOLATION       RenderingHints/VALUE_INTERPOLATION_BICUBIC)
      (add-hint RenderingHints/KEY_ANTIALIASING        RenderingHints/VALUE_ANTIALIAS_ON)
      (add-hint RenderingHints/KEY_COLOR_RENDERING     RenderingHints/VALUE_COLOR_RENDER_QUALITY)
      (add-hint RenderingHints/KEY_DITHERING           RenderingHints/VALUE_DITHER_DISABLE)
      (add-hint RenderingHints/KEY_RENDERING           RenderingHints/VALUE_RENDER_QUALITY)
      (add-hint RenderingHints/KEY_STROKE_CONTROL      RenderingHints/VALUE_STROKE_PURE)
      (add-hint RenderingHints/KEY_FRACTIONALMETRICS   RenderingHints/VALUE_FRACTIONALMETRICS_ON)
      (add-hint RenderingHints/KEY_TEXT_ANTIALIASING   RenderingHints/VALUE_TEXT_ANTIALIAS_OFF))))

(defn- high-quality-png-transcoder []
  (proxy [PNGTranscoder] []
    (createRenderer []
      (let [renderer (proxy-super createRenderer)]
        (.setRenderingHints renderer (get-high-quality-hints))
        renderer))))

;; SVG parser can't handle "rgba(1,1,1,0.1)" style format -> convert to hex
(defn- hexify [[_ & rgb]]
  (str \"
       (->> rgb (map #(Integer. %)) (map #(format "%02x" %)) (cons "#") (apply str))
       \"))

(defn- rgba->hex [xml-string]
  (string/replace xml-string #"\"rgba\((\d+),(\d+),(\d+),[\.0-9]+\)\"" hexify))


(defn parse-svg-string [^String s]
  (let [factory (SAXSVGDocumentFactory. "org.apache.xerces.parsers.SAXParser")]
    (with-open [in (-> s rgba->hex (.getBytes StandardCharsets/UTF_8) ByteArrayInputStream.)]
      (.createDocument factory nil in))))

(defn render-svg-document
  ([svg-document filename]
   (render-svg-document svg-document filename {}))
  ([svg-document filename options]
   (let [scale (or (:scale options) 1)
         width (:width options)
         transcoder (high-quality-png-transcoder)]

     (cond
       (not width)
       (throw (ex-info "Cannot transcode - can't determine SVG document width"
                       {:options    options})))

     (.addTranscodingHint transcoder SVGAbstractTranscoder/KEY_WIDTH (float (* scale width)))

     (with-open [out-stream (if filename
                              (io/output-stream filename)
                              (ByteArrayOutputStream.))]
       (let [in (TranscoderInput. svg-document)
             out (TranscoderOutput. out-stream)]
         (.transcode transcoder in out)
         (or filename (.toByteArray out-stream)))))))

(defn render-svg-string
  ([uri filename]
   (render-svg-document (parse-svg-string uri) filename {}))
  ([uri filename options]
   (render-svg-document (parse-svg-string uri) filename options)))
