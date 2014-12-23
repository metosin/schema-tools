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

(defn explicit-path-vals
  "Returns vector of tuples containing path vector to the value and the value."
  [m] (path-vals m #(if (keyword-key? %) (s/explicit-schema-key %) %)))

;;
;; Core functions
;;

(defn dissoc
  "Dissoc[iate]s keys from Schema."
  [schema & ks]
  (let [ks? (set ks)]
    (p/for-map
      [[k v] schema
       :when (or
               (not (keyword-key? k))
               (not (ks? (s/explicit-schema-key k))))]
      k v)))

(defn select-keys
  "Select part of schema based on vector of keywords.
   Plain keyword should match optional-key with mathcing keyword."
  [schema ks]
  (let [ks? (set ks)]
    (p/for-map [[k v] schema
                :when (and
                        (keyword-key? k)
                        (ks? (s/explicit-schema-key k)))]
      k v)))

(defn get-in
  "Returns the value in a nested associative Schema,
  where ks is a sequence of keys. Returns nil if the key
  is not present, or the not-found value if supplied."
  [schema ks & [not-found]]
  (reduce (fn [acc k]
            (or (get acc k) (get acc (s/optional-key k) (get acc (s/required-key k))) not-found))
          schema
          ks))

;;
;; Extras
;;

(defn strip-keys
  "Strips recursively all keys disallowed keys from value."
  [schema value]
  (->> value
       (s/check schema)
       path-vals
       (filter (p/fn-> second 'disallowed-key))
       (map first)
       (reduce (partial dissoc-in) value)))

(defn select-schema
  "Selects recursively all valid keyword-keys based on Schema."
  [schema value]
  (->> schema
       explicit-path-vals
       (map first)
       (reduce (partial copy-in value) {})))

(defn- transform-keys
  [m f ks]
  (let [ks? (explicit-key-set ks)]
    (p/for-map [[k v] m]
      (cond
        (and ks (not (ks? (explicit-key k)))) k
        (keyword-key? k) (f (s/explicit-schema-key k))
        :else (f k))
      v)))

(defn optional-keys [m & ks] (transform-keys m s/optional-key ks))
(defn required-keys [m & ks] (transform-keys m identity ks))
