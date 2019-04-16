(ns salava.core.ui.field
  (:require [cljs-uuid-utils.core :refer [make-random-uuid uuid-string]]
            [salava.core.helper :refer [dump]]))

(defn random-key []
  (-> (make-random-uuid)
      (uuid-string)))


(defn add-field
  ([fields-atom new-field] (add-field fields-atom new-field (count @fields-atom)))
  ([fields-atom new-field index]
   (let [[before-blocks after-blocks] (split-at index @fields-atom)]
     (reset! fields-atom (vec (concat before-blocks [(assoc new-field :key (random-key))] after-blocks))))))

(defn add-field-atomic
  ([fields-atom new-field] (add-field-atomic fields-atom new-field (count @fields-atom)))
  ([fields-atom new-field index]
   (let [[before-blocks after-blocks] (split-at index @fields-atom)]
     (reset! fields-atom (vec (concat before-blocks [(assoc @new-field :key (random-key))] after-blocks))))))



(defn remove-field [fields-atom index]
  (let [fields @fields-atom
        start (subvec fields 0 index)
        end (subvec fields (inc index) (count fields))]
    (reset! fields-atom (vec (concat start end)))))

(defn move-field [direction fields-atom old-position]
  (let [new-position (cond
                       (= :down direction) (if-not (= old-position (- (count @fields-atom) 1))
                                             (inc old-position))
                       (= :up direction) (if-not (= old-position 0)
                                           (dec old-position)))]
    (if new-position
      (swap! fields-atom assoc old-position (nth @fields-atom new-position)
             new-position (nth @fields-atom old-position)))))

(defn vec-remove
  "remove elem in coll"
  [coll pos]
  (vec (concat (subvec coll 0 pos) (subvec coll (inc pos)))))

(defn move-positions [fields-atom old-position new-position]
  (let [fields @fields-atom
        new-position (if-not (nil? new-position) new-position (do #_(assoc fields (inc (count fields)) {})
                                                                (inc (count fields))))
        start (subvec fields 0 new-position)
        end (subvec fields new-position (count fields))
        new-start (vec (concat start (conj [] (nth @fields-atom old-position))))
        new-end (remove (fn [b] (some #(= b %) end)) (vec (concat new-start end))) #_(vec-remove end (- old-position (dec (count end))))
        ;[before-blocks after-blocks] (split-at new-position @fields-atom)
        ;new (into [(nth @fields-atom old-position)] after-blocks)
        ]
     ;(dump start)
    ;(dump new-start)
    ;(dump end)

    (dump new-end)
    ;(dump new-start)
    (when (and new-start #_new-end)
    (reset! fields-atom  (vec (concat new-start end))))
    ;(dump (nth @fields-atom old-position))

    ;(reset! fields-atom  (vec (concat before-blocks (vec-remove new old-position))))
    ;(dump @fields-atom)
    ;(dump before-blocks)
    ;(dump after-blocks)
    ;(dump new)


    )
  )

(defn move-field-drop [fields-atom old-position new-position]
  (let [old-position-content (nth @fields-atom new-position)

        ]
  (swap! fields-atom assoc new-position (nth @fields-atom old-position)
         ;new-position (nth @fields-atom old-position)
         )
  (for [n (nthrest @fields-atom (inc old-position)) #_(subvec @fields-atom old-position (count @fields-atom))
          :let [new (inc old-position)]]
    (swap! fields-atom assoc n (nth @fields-atom new)))
  ))

;; (defn move-field-drop [fields-atom old-position new-position]
  ;;(let [start (subvec @fields-atom 0 new-position)
    ;;    end (map #(nth (inc (index)) (subvec @fields (inc index) (count fields)))]
  ;;(swap! fields-atom assoc old-position (nth @fields-atom new-position)
     ;;    new-position (nth @fields-atom old-position))
  ;;))

#_(defn  move-field-drop [fields-atom old-position new-position]
  (let [fields @fields-atom
        start (subvec fields 0 new-position)
        end (subvec fields old-position (count fields))]
    (swap! fields-atom assoc (dec (count start)) (nth @fields-atom new-position)
           assoc 0 (nth))
    #_(reset! fields-atom (vec (concat start end)))))

#_(defn move-field-drop [fields-atom old-position new-position]
   (let [[before-blocks after-blocks] (split-at new-position @fields-atom)]

  ))
