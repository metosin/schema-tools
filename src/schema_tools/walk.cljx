(ns schema-tools.walk
  "Provides walk function which can be used to transform schemas while
   preserving their structure and type."
  (:require [schema.core :as s])
  #+cljs (:refer-clojure :exclude [record?]))

(defprotocol WalkableSchema
  (-walk [this inner outer]))

#+cljs
(defn- record? [x]
  (satisfies? IRecord x))

(defn walk
  "Calls inner for sub-schemas of this schema, creating new Schema of the same
   type as given and preserving the metadata. Calls outer with the created
   Schema."
  {:added "0.3.0"}
  [this inner outer]
  (cond
    (satisfies? WalkableSchema this) (-walk this inner outer)
    (record? this) (outer (with-meta (reduce (fn [r x] (conj r (inner x))) this this) (meta this)))
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
  (-walk [this _ outer]
    (outer this))

  schema.core.NamedSchema
  (-walk [this inner outer]
    (outer (with-meta (s/named (inner (:schema this)) (:name this)) (meta this))))

  schema.core.CondPre
  (-walk [this inner outer]
    (outer (with-meta (apply s/cond-pre (map inner (:schemas this))) (meta this)))))
