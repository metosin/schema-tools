(ns schema-tools.walk
  (:require [schema.core :as s]))

(defprotocol WalkableSchema
  (-walk [this inner outer]))

(defn walk
  [this inner outer]
  (cond
    (satisfies? WalkableSchema this) (-walk this inner outer)
    #+clj (list? this) #+clj (outer (with-meta (apply list (map inner this)) (meta this)))
    (seq? this) (outer (with-meta (doall (map inner this)) (meta this)))
    (coll? this) (outer (with-meta (into (empty this) (map inner this)) (meta this)))
    :else (outer this)))

(extend-protocol WalkableSchema
  #+clj
  clojure.lang.IMapEntry
  #+clj
  (-walk [this inner outer]
    (outer (vec (map inner this))))

  #+clj
  clojure.lang.IRecord
  #+clj
  (-walk [this inner outer]
    (outer (with-meta (reduce (fn [r x] (conj r (inner x))) this this) (meta this))))

  schema.core.Maybe
  (-walk [this inner outer]
    (outer (with-meta (s/maybe (inner (:schema this))) (meta this))))

  schema.core.Both
  (-walk [this inner outer]
    (outer (with-meta (apply s/both (map inner (:schemas this))) (meta this))))

  schema.core.Either
  (-walk [this inner outer]
    (outer (with-meta (apply s/either (map inner (:schemas this))) (meta this))))

  #+clj
  schema.core.Recursive
  #+clj
  (-walk [this inner outer]
    (outer (with-meta (s/recursive (inner (:derefable this))) (meta this))))

  schema.core.Predicate
  (-walk [this inner outer]
    (outer this))

  schema.core.NamedSchema
  (-walk [this inner outer]
    (outer (with-meta (s/named (inner (:schema this)) (:name this)) (meta this)))))
