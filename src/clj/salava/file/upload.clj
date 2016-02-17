(ns salava.file.upload
  (:require [clojure.java.io :as io]
            [slingshot.slingshot :refer :all]
            [pantomime.mime :refer [mime-type-of]]
            [salava.core.util :refer [public-path file-extension]]
            [salava.core.i18n :refer [t]]
            [salava.core.time :refer [unix-time]]
            [salava.file.db :as f]))

(defn upload-file [ctx user-id file image-only?]
  (try+
    (let [{:keys [size filename tempfile]} file
          types (-> ctx
                    (get-in [:config :file :allowed-file-types])
                    vals
                    distinct)
          mime-types (if image-only?
                       (filter #(re-matches #"^image/.*" %) types)
                       types)
          max-size (get-in ctx [:config :file :max-size] 100000000)
          extension (file-extension filename)
          mime-type (mime-type-of tempfile)
          path (public-path tempfile extension)
          full-path (str "resources/public/" path)
          insert-data {:name filename
                     :path path
                     :size size
                     :mime_type mime-type
                     :tags []}]

      (if-not (some #(= % mime-type) mime-types)
        (throw+ (str (t :file/Filetype) " " mime-type " " (t :file/isnotallowed))))
      (if (> size max-size)
        (throw+ (str (t :file/Filetoobig) ". " (t :file/Maxfilesize) ": " (quot max-size (* 1024 1024)) "MB")))

      (io/make-parents full-path)
      (io/copy tempfile  (io/file full-path))
      (let [file-id (f/save-file! ctx user-id insert-data)
            file-data (assoc insert-data :id file-id
                                         :ctime (unix-time)
                                         :mtime (unix-time))]
        {:status "success" :message (t :file/Fileuploaded) :reason (t :file/Fileuploaded) :data file-data}))
    (catch Object _
      {:status "error" :message (t :file/Errorwhileuploading) :reason _})
    (finally
      (.delete (:tempfile file)))))