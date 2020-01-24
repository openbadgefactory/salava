(ns salava.badgeIssuer.util
  (:require
   [salava.core.util :refer [bytes->base64]])
  (:import
    [java.io ByteArrayOutputStream]
    [javax.imageio ImageIO]))

(defn image->base64str [canvas]
  (let [out (ByteArrayOutputStream.)]
    (ImageIO/write canvas "png" out)
    (str "data:image/png;base64," (bytes->base64 (.toByteArray out)))))
