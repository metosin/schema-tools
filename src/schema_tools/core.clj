(ns schema-tools.core
  (:require [plumbing.core :as p]
            [schema.core :as s])
  (:refer-clojure :exclude [dissoc]))

(defn dissoc
  [schema & ks]
  (let [ks? (set ks)]
    (p/for-map
      [[k v] schema
       :when (not (ks? (s/explicit-schema-key k)))]
      k v)))
