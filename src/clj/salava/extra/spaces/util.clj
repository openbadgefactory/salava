(ns salava.extra.spaces.util
 (:require
  [salava.core.util :as u]
  [clojure.string :refer [blank?]]
  [clojure.tools.logging :as log]
  [slingshot.slingshot :refer :all])
 (:import
  [java.io ByteArrayOutputStream]
  [javax.imageio ImageIO]))


(defn save-image! [ctx url]
 (if-not (blank? url) (u/file-from-url-fix ctx url) nil))

(defn uuid [] (str (java.util.UUID/randomUUID)))

(defn image->base64str [canvas]
  (let [out (ByteArrayOutputStream.)]
    (ImageIO/write canvas "png" out)
    (str "data:image/png;base64," (u/bytes->base64 (.toByteArray out)))))

(defn- check-logo [width height]
  (and
    (some #(= width %) (range (- height 10) (+ height 10)))
    (some #(= height %) (range (- width 10) (+ width 10)))))

(defn- check-banner [width]
  (<= width 1000))

(defn upload-image [ctx user file kind]
  (let [{:keys [size tempfile content-type]} file
        max-size 250000] ;;250kb
    (try+
     (when-not (= "image/png" content-type)
       (throw+ {:status "error" :message "extra-spaces/FilenotPNG"}))
     (when (> size max-size)
       (throw+ {:status "error" :message "extra-spaces/Filetoobig"}))
     (let [image (ImageIO/read tempfile)
           width (.getWidth image)
           height (.getHeight image)]
       (case kind
        "logo" (when-not (check-logo width height) (throw+ {:status "error" :message "extra-spaces/Imagemustbesquare"}))
        "banner" (when-not (check-banner width) (throw+ {:status "error" :message "extra-spaces/Maxwidthexceeded"}))
        nil)
       {:status "success" :url (image->base64str image)})
     (catch Object _
       (log/error _)
       {:url "" :status "error" :message (:message _)}))))
