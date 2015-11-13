(ns salava.file.upload
  (:require [clojure.java.io :as io]
            [slingshot.slingshot :refer :all]
            [salava.core.util :refer [public-path file-extension]]
            [salava.core.i18n :refer [t]]
            [salava.file.db :as f]))

(defn upload-file [ctx user-id file]
  (try+
    (let [{:keys [size filename tempfile content-type]} file
          mime-types (-> ctx
                         (get-in [:config :file :allowed-file-types])
                         vals
                         distinct)
          max-size (get-in ctx [:config :file :max-size] 100000000)
          extension (file-extension filename)
          path (public-path tempfile extension)
          full-path (str "resources/public/" path)
          file-data {:name filename
                :path path
                :size size
                :mime_type content-type}]
      (if-not (some #(= % content-type) mime-types)
        (throw+ (str (t :file/Filetype) " " content-type " " (t :file/isnotallowed))))
      (if (> size max-size)
        (throw+ (str (t :file/Filetoobig) " " (t :file/Maxfilesize) ": " (quot max-size (* 1024 1024)) "MB")))
      (io/make-parents full-path)
      (io/copy tempfile  (io/file full-path))
      (f/save-file! ctx user-id file-data)
      {:status "success" :message (t :file/Fileuploaded) :reason (t :file/Fileuploaded)})
    (catch Object _
      {:status "error" :message (t :file/Errorwhileuploading) :reason _})
    (finally
      (.delete (:tempfile file)))))