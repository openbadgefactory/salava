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

(defn move-field-drop [fields-atom old-position new-position]
  (let [fields @fields-atom
        new-position (if-not (nil? new-position) new-position (count fields))
        direction (if (< new-position old-position) :up :down)
        start (subvec fields 0 new-position)
        end (subvec fields new-position (count fields))]

    (reset! fields-atom (vec (concat (case direction
                                       :up (vec (concat start (conj [] (nth @fields-atom old-position))))
                                       :down (let [new-end (vec (concat (conj [] (nth @fields-atom old-position)) end))]
                                               (remove (fn [b] (some #(identical? b %) (vec (concat (conj [] (nth @fields-atom old-position)) end)))) (vec (concat start new-end)))))
                                     (case direction
                                       :up (let [new-start (vec (concat start (conj [] (nth @fields-atom old-position))))]
                                             (remove (fn [b] (some #(identical? b %) new-start)) (vec (concat new-start end))))
                                       :down (vec (concat (conj [] (nth @fields-atom old-position)) end))))))))
