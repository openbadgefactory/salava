#_(ns salava.badgeIssuer.upload
    (:require
     [clojure.tools.logging :as log]
     [salava.badgeIssuer.util :refer [image->base64str]]
     [slingshot.slingshot :refer :all])
    (:import
      [javax.imageio ImageIO]))

#_(defn upload-image [ctx user file]
    (let [{:keys [tempfile content-type]} file]
      (try+
       (when-not (= "image/png" content-type)
         (throw+ {:status "error" :message "File is not a PNG image"}))
       (let [image (ImageIO/read tempfile)
             width (.getWidth image)
             height (.getHeight image)]
         (when-not (= width height)
           (throw+ {:status "error" :message "Image must be square e.g. (60 * 60)"}))
         {:status "success" :url (image->base64str image)})
       (catch Object _
         (log/error _)
         {:url "" :status "error" :message (:message _)}))))
