(ns salava.file.icons)

(defn file-icon [mime-type]
  "Returns Font Awesome icon based on file"
  (case mime-type
    ("image/jpeg" "image/gif" "image/png") "fa-file-picture-o"
    ("text/plain") "fa-file-text-o"
    ("application/msword" "application/vnd.openxmlformats-officedocument.wordprocessingml.document" "application/vnd.oasis.opendocument.text") "fa-file-word-o"
    ("application/vnd.ms-excel" "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" "application/vnd.oasis.opendocument.spreadsheet") "fa-file-excel-o"
    ("application/pdf") "fa-file-pdf-o"
    ("application/vnd.ms-powerpoint" "application/vnd.openxmlformats-officedocument.presentationml.presentation" "application/vnd.oasis.opendocument.presentation") "fa-file-powerpoint-o"
    "fa-file-o")) ;default