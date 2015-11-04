(ns schema-tools.experimental.walk
  "Add walk support for schema.experimental.* Schemas.

   Extends the WalkableSchema so requiring this namespace somewhere provides
   global support.

   Note: Walking through either abstract-map or extended-schema doesn't change
   the other. I.e. if you have Animal and Cat, which extends Animal, and walk
   through the Cat the Animal is not changed."
  (:require [schema-tools.walk :as sw]
            [schema.experimental.abstract-map :as abstract-map]))

(extend-protocol sw/WalkableSchema
  schema.experimental.abstract_map.AbstractSchema
  (-walk [this inner outer]
    (outer (with-meta (abstract-map/->AbstractSchema
                        (atom (reduce-kv (fn [acc k sub-schema]
                                           (assoc acc k (inner sub-schema)))
                                         {}
                                         @(:sub-schemas this)))
                        (:type this)
                        (inner (:schema this))
                        (:open? this))
                      (meta this))))

  schema.experimental.abstract_map.SchemaExtension
  (-walk [this inner outer]
    (outer (with-meta (abstract-map/->SchemaExtension
                        (:schema-name this)
                        (inner (:base-schema this))
                        (inner (:extended-schema this))
                        (:explain-value this))
                      (meta this)))))
