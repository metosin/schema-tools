(ns schema-tools.core
  (:require [plumbing.core :as p]
            [schema.core :as s]
            [schema-tools.util :refer :all])
  (:refer-clojure :exclude [dissoc select-keys get-in assoc-in update-in]))

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

(defn- key-in-schema [m k]
  (cond
    (contains? m k) k
    (contains? m (s/optional-key k)) (s/optional-key k)
    (contains? m (s/required-key k)) (s/required-key k)
    :else k))

(defn- get-in-schema [m k & [default]]
  (get m (key-in-schema m k) default))

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
          (let [m (get-in-schema m k sentinel)]
            (if (identical? sentinel m)
              not-found
              (recur sentinel m (next ks)))))
        m))))

(defn assoc-in
  "Associates a value in a nested associative Schema, where ks is a
  sequence of keys and v is the new value and returns a new nested Schema.
  If any levels do not exist, hash-maps will be created."
  [m [k & ks] v]
  (let [kis (key-in-schema m k)]
    (if ks
      (assoc m kis (assoc-in (get-in-schema m k) ks v))
      (assoc m kis v))))

(defn update-in
  "'Updates' a value in a nested associative Schema, where ks is a
  sequence of keys and f is a function that will take the old value
  and any supplied args and return the new value, and returns a new
  nested Schema. If any levels do not exist, hash-maps will be
  created."
  [m [k & ks] f & args]
  (let [kis (key-in-schema m k)]
    (if ks
      (assoc m kis (apply update-in (get-in-schema m k) ks f args))
      (assoc m kis (apply f (get-in-schema m k) args)))))

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
