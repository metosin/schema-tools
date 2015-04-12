(ns schema-tools.util)

(defn path-vals
  "Returns vector of tuples containing path vector to the value and the value."
  ([m] (path-vals m identity))
  ([m fk]
    (letfn
      [(pvals [l p m]
              (reduce
                (fn [l [k v]]
                  (let [k (fk k)]
                    (if (map? v)
                      (pvals l (conj p k) v)
                      (cons [(conj p k) v] l))))
                l m))]
      (pvals [] [] m))))

;; https://github.com/clojure/core.incubator/blob/master/src/main/clojure/clojure/core/incubator.clj
(defn dissoc-in
  "Dissociates an entry from a nested associative structure returning a new
  nested structure. keys is a sequence of keys. Any empty maps that result
  will not be present in the new structure."
  [m [k & ks]]
  (if ks
    (if-let [nextmap (get m k)]
      (let [newmap (dissoc-in nextmap ks)]
        (if (seq newmap)
          (assoc m k newmap)
          (dissoc m k)))
      m)
    (dissoc m k)))

(defn map-keys [f m]
  (with-meta
    (persistent!
      (reduce-kv
        (fn [acc k v] (assoc! acc (f k) v))
        (transient (empty m))
        m))
    (meta m)))
