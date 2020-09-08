(ns salava.extra.stats.db
  (:require [clojure.tools.logging :as log]
            [clojure.java.io :as io]
            [clojure.java.jdbc :as jdbc]
            [clojure.string :as string]
            [salava.core.util :as u]
            [salava.core.http :as http]
            [clojure.data.json :as json]
            [yesql.core :refer [defqueries]]))


(defqueries "sql/extra/stats/main.sql")

(def data
  {"54175" {:facebook 1}, "188131" {:facebook 1}, "233681" {:facebook 1}, "236370" {:facebook 2}, "210870" {:facebook 1}, "3925" {:pinterest 1}, "228697" {:facebook 2}
   "168458" {:facebook 1}, "250928" {:facebook 1}, "236703" {:facebook 2}, "206729" {:twitter 1}, "223053" {:twitter 1}, "206883" {:facebook 1}, "251653" {:facebook 1}, "167178" {:facebook 1},
   "254463" {:facebook 1}, "53240" {:twitter 1}, "203479" {:twitter 1}, "207385" {:twitter 1}, "7528" {:pinterest 1}, "206882" {:facebook 1}, "251858" {:facebook 2}
   , "17426" {:twitter 1}, "224416" {:facebook 1}, "248517" {:facebook 1}, "224080" {:facebook 2}, "228832" {:facebook 1}, "190776" {:facebook 1, :pinterest 1}, "223592" {:twitter 1},
   "233307" {:facebook 1}, "90495" {:facebook 1}, "252197" {:facebook 4}, "201018" {:facebook 1}, "257654" {:facebook 1}, "236918" {:facebook 1}, "209118" {:facebook 1}
   "255080" {:facebook 4}, "258614" {:facebook 1}, "204762" {:facebook 1}, "73951" {:twitter 1}, "3085" {:twitter 1}, "250463" {:facebook 1}, "245278" {:facebook 1}, "176423" {:facebook 1},
   "108733" {:twitter 1}, "231653" {:facebook 1}, "257565" {:facebook 3}, "34318" {:twitter 1}, "244540" {:facebook 2}, "4584" {:pinterest 1}, "187853" {:facebook 1}
   , "256564" {:facebook 4}, "231503" {:facebook 1}, "232008" {:facebook 1}, "247196" {:facebook 2}, "167032" {:facebook 1}, "258276" {:facebook 2}, "212822" {:facebook 1}
   , "53778" {:facebook 1}, "81816" {:facebook 1}, "160918" {:facebook 1}, "216912" {:facebook 1}, "140322" {:twitter 1}, "236704" {:facebook 2}, "126183" {:facebook 2}
   , "255188" {:facebook 9}, "254769" {:twitter 1}, "108745" {:twitter 1}, "201021" {:facebook 1}, "174459" {:facebook 1}, "257945" {:facebook 2}, "237709" {:facebook 1}, "248691" {:facebook 1}, "218059" {:facebook 1}, "120221" {:facebook 1},
   "31490" {:twitter 1}, "209299" {:facebook 1}, "258624" {:facebook 2}, "257562" {:facebook 1}, "225337" {:facebook 3}
   , "210937" {:facebook 1}, "249873" {:facebook 1}, "76647" {:facebook 2}, "145333" {:facebook 1}, "162415" {:facebook 2}, "124432" {:facebook 1}, "162441" {:facebook 2}
   , "146243" {:twitter 1}, "258363" {:facebook 3}, "228817" {:facebook 1}, "132477" {:facebook 1}, "163652" {:facebook 1}, "148164" {:facebook 1}, "245418" {:facebook 1}
   , "163061" {:facebook 2}, "258681" {:facebook 1}, "244115" {:facebook 1}, "239538" {:twitter 1}, "107445" {:facebook 1}, "186004" {:facebook 5}, "259576" {:linkedin 2}, "
   207166" {:facebook 1}, "208884" {:facebook 1}, "146521" {:facebook 3}, "238391" {:facebook 3}, "201020" {:facebook 1}, "174457" {:facebook 1}, "253377" {:facebook 1}, "201019" {:facebook 1}, "257474" {:facebook 6}, "104383" {:twitter 1},
   "258679" {:facebook 1}, "110953" {:twitter 1}, "169568" {:facebook 1}, "186230" {:twitter 1}, "148766" {:facebook 1}
   , "148767" {:facebook 1}, "96234" {:facebook 3}, "233186" {:facebook 1}, "226372" {:facebook 1}, "257563" {:facebook 4}, "257573" {:facebook 3}, "207431" {:facebook 1}, "87687" {:twitter 1}, "111095" {:facebook 2}, "238767" {:facebook 1},})


(def data2
  {"10" {:linkedin 1 :facebook 4}
   "30" {:pinterest 1 :facebook 4}
   "60" {:twitter 1 :facebook 4 :linkedin 3}
   "80" {:linkedin 4}
   "75" {:twitter 1}
   "78" {:facebook 4}
   "100" {:twitter 1 :facebook 4 :linkedin 3 :pinterest 1}
   "101" {:twitter 1 :facebook 4 :pinterest 3}
   "69" {:twitter 1 :linkedin 4 :pinterest 5}
   "67" {:twitter 1 :facebook 4}})



#_(defn log-to-db [ctx payload]
    (let [{:keys [social_hits ts]} (assoc (json/read-str payload :key-fn keyword) :social_hits data)
          stats (json/write-str (frequencies (reduce (fn [r m]
                                                      (concat r
                                                        (keys m))) [] (vals social_hits))))]
      (insert-social-media-stats! {:value stats :name "social_media_share"} (u/get-db ctx))))


(defn log-to-db [ctx payload]
 (let [{:keys [social_hits ts]} (json/read-str payload :key-fn keyword)
       spaces (select-all-spaces {} (u/get-db-col ctx :id))
       spaceid->badges (when (and (seq spaces) (seq (keys payload)))
                        (reduce
                         (fn [r s]
                           (assoc r s (select-space-badges {:id s :ids (keys data2)} (u/get-db-col ctx :id))))
                         {} spaces))

       spaceid->stats (reduce-kv
                        (fn [r k v]
                         (assoc r k (select-keys payload (vec (map str v)))))
                        {}
                        spaceid->badges)
       general-stats (frequencies (reduce (fn [r m]
                                           (concat r
                                             (keys m)))
                                    []
                                    (vals social_hits)))
       stats (if (empty? spaceid->badges)
                 {0 general-stats}
                 (merge
                  (reduce-kv
                   (fn [r k v]
                     (assoc r k (frequencies (reduce (fn [c m]
                                                       (concat c
                                                         (keys m)))
                                                     []
                                                     (vals v)))))
                   {}
                   spaceid->stats)
                  {0 general-stats}))]
     ;(prn spaceid->stats)
     ;(clojure.pprint/pprint stats)
     (insert-social-media-stats! {:value (json/write-str stats) :name "social_media_share"} (u/get-db ctx))))


#_(defn social-media-stats-latest [ctx]
   (let [stats (latest-social-media-stats {} (u/get-db-1 ctx))
         value (json/read-str (:value stats) :key-fn keyword)]
    (assoc stats :value value)))

(defn social-media-stats-latest [ctx]
 (let [stats (latest-social-media-stats {} (u/get-db-1 ctx))
       value (get (json/read-str (:value stats) :key-fn keyword) :0 {})]
  (assoc stats :value value)))

(defn social-media-stats-ts [ctx ts]
  (let [stats (map #(assoc % :value (get (json/read-str (:value %) :key-fn keyword) :0 {})) (timestamp-social-media-stats {:time ts} (u/get-db ctx)))]
   (hash-map :value (apply (partial merge-with +) (map :value stats)) :ctime ts)))

#_(defn social-media-stats-ts [ctx ts]
    (let [stats (map #(assoc % :value (json/read-str (:value %) :key-fn keyword)) (timestamp-social-media-stats {:time ts} (u/get-db ctx)))]
     (hash-map :value (apply (partial merge-with +) (map :value stats)) :ctime ts)))

(defn space-social-media-stats [ctx space-id ts]
  (let [stats (map #(assoc % :value (get (json/read-str (:value %) :key-fn keyword) (keyword (str space-id)) {})) (timestamp-social-media-stats {:time ts} (u/get-db ctx)))]
    (hash-map :value (apply (partial merge-with +) (map :value stats)) :ctime ts)))
