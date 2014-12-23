(ns schema-tools.core
  (:require [plumbing.core :as p]
            [schema.core :as s]
            [schema-tools.util :refer :all])
  (:refer-clojure :exclude [dissoc select-keys get-in]))

(def AnyKeys {s/Keyword s/Any})

(defn any-keys [schema] (merge AnyKeys schema))

(defn keyword-key?
  "Tests whether the key is keyword or spesific schema key."
  [x]
  (or (keyword? x)
      (s/specific-key? x)))

(defn- explicit-key [k] (if (s/specific-key? k) (s/explicit-schema-key k) k))

(defn- explicit-key-set [ks]
  (reduce (fn [s k] (conj s (explicit-key k))) #{} ks))

(defn- explicit-path-vals
  "Returns vector of tuples containing path vector to the value and the value."
  [m] (path-vals m #(if (keyword-key? %) (s/explicit-schema-key %) %)))

;;
;; Core functions
;;

(defn dissoc
  "Dissoc[iate]s keys from Schema."
  [schema & ks]
  (let [ks? (explicit-key-set ks)]
    (p/for-map [[k v] schema
                :when (not (ks? (explicit-key k)))]
      k v)))

(defn select-keys
  "Like clojure.core/select-keys but handles boths optional-keys and required-keys."
  [schema ks]
  (let [ks? (explicit-key-set ks)]
    (p/for-map [[k v] schema
                :when (ks? (explicit-key k))]
      k v)))

(defn get-in
  "Returns the value in a nested associative Schema,
  where ks is a sequence of keys. Returns nil if the key
  is not present, or the not-found value if supplied."
  ([m ks]
    (get-in m ks nil))
  ([m ks not-found]
    (loop [sentinel (Object.)
           m m
           ks (seq ks)]
      (if ks
        (let [k (first ks)]
          (let [m (or (get m k)
                      (get m (s/optional-key k))
                      (get m (s/required-key k))
                      sentinel)]
            (if (identical? sentinel m)
              not-found
              (recur sentinel m (next ks)))))
        m))))

;;
;; Extras
;;

(defn select-schema
  "Like select-keys but selects all (nested) keys present in a Schema."
  [schema value]
  (->> schema
       explicit-path-vals
       (map first)
       (reduce (partial copy-in value) (empty value))))

(defn- transform-keys
  [m f ks]
  (let [ks? (explicit-key-set ks)]
    (p/for-map [[k v] m]
      (cond
        (and ks (not (ks? (explicit-key k)))) k
        (keyword-key? k) (f (s/explicit-schema-key k))
        :else (f k))
      v)))

(defn optional-keys
  "Makes given map keys optional. Defaults to all keys."
  [m & ks] (transform-keys m s/optional-key ks))

(defn required-keys
  "Makes given map keys required. Defaults to all keys."
  [m & ks] (transform-keys m identity ks))
