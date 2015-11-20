(ns schema-tools.core.impl
  (:require [schema.core :as s]))

(defprotocol SchemaValue
  (schema-value [this] "Returns the sub-schema for given schema."))

(extend-protocol SchemaValue
  schema.core.One
  (schema-value [this] (:schema this))

  schema.core.Maybe
  (schema-value [this] (:schema this))

  ; schema.core.Both
  ; schema.core.Either
  ; schema.core.Recursive
  ; schema.core.Predicate

  schema.core.NamedSchema
  (schema-value [this] (:schema this))

  ; schema.core.ConditionalSchema
  ; schema.core.CondPre

  schema.core.Constrained
  (schema-value [this] (:schema this))

  Object
  (schema-value [this] this)

  nil
  (schema-value [_] nil))

