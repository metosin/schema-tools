(ns schema-tools.core
  (:require [schema.core :as s]
            [schema-tools.util :as stu :include-macros true])
  (:refer-clojure :exclude [dissoc select-keys get-in assoc-in update-in]))

(def AnyKeys {s/Any s/Any})
(defn any-keys [] AnyKeys)

(def AnyKeywordKeys {s/Keyword s/Any})
(defn any-keyword-keys [& schemas] (apply merge AnyKeywordKeys schemas))

(defn- explicit-key [k] (if (s/specific-key? k) (s/explicit-schema-key k) k))

(defn- explicit-key-set [ks]
  (reduce (fn [s k] (conj s (explicit-key k))) #{} ks))

;;
;; Core functions
;;

(defn dissoc
  "Dissoc[iate]s keys from Schema."
  [schema & ks]
  (let [ks? (explicit-key-set ks)]
    (into {} (filter (comp not ks? explicit-key key) schema))))

(defn select-keys
  "Like clojure.core/select-keys but handles boths optional-keys and required-keys."
  [schema ks]
  (let [ks? (explicit-key-set ks)]
    (into {} (filter (comp ks? explicit-key key) schema))))

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

;; https://github.com/clojure/core.incubator/blob/master/src/main/clojure/clojure/core/incubator.clj
(defn dissoc-in
  "Dissociates an entry from a nested associative Schema returning a new
  nested structure. keys is a sequence of keys. Any empty maps that result
  will not be present in the new Schema."
  [m [k & ks]]
  (let [k (key-in-schema m k)]
    (if ks
      (if-let [nextmap (get m k)]
        (let [newmap (dissoc-in nextmap ks)]
          (if (seq newmap)
            (assoc m k newmap)
            (dissoc m k)))
        m)
      (dissoc m k))))

;;
;; Extras
;;

(defn select-schema
  "Removes all keys that are disallowed in the Schema."
  [schema value]
  (->> value
       (s/check schema)
       stu/path-vals
       (filter (comp #(= % 'disallowed-key) second))
       (map first)
       (reduce stu/dissoc-in value)))

(defn- transform-keys
  [m f ks]
  (let [ks? (explicit-key-set ks)]
    (stu/map-keys (fn [k]
                    (cond
                      (and ks (not (ks? (explicit-key k)))) k
                      (s/specific-key? k) (f (s/explicit-schema-key k))
                      :else (f k)))
                  m)))

(defn with-optional-keys
  "Makes given map keys optional. Defaults to all keys."
  [m & ks] (transform-keys m s/optional-key ks))

(defn with-required-keys
  "Makes given map keys required. Defaults to all keys."
  [m & ks] (transform-keys m #(if (keyword? %) % (s/required-key %)) ks))
