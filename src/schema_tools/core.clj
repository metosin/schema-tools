(ns schema-tools.core
  (:require [plumbing.core :as p]
            [schema.core :as s])
  (:refer-clojure :exclude [dissoc select-keys]))

(defn keyword-key?
  "Tests whether the key is optional or required keyword key"
  [x]
  (or (keyword? x)
      (s/required-key? x)
      (s/optional-key? x)))

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
  (let [ks (set ks)]
    (p/for-map [[k v] schema
                :when (and
                        (keyword-key? k)
                        (contains? ks (s/explicit-schema-key k)))]
      k v)))

