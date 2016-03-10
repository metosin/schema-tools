(ns schema-tools.core.impl
  (:require [schema.core :as s]
            [schema.spec.variant :as variant]
            [schema.spec.core :as spec]))

(defprotocol SchemaValue
  (schema-value [this] "Returns the sub-schema for given schema."))

(extend-protocol SchemaValue
  schema.core.One
  (schema-value [this] (:schema this))

  schema.core.Maybe
  (schema-value [this] (:schema this))

  schema.core.Both
  (schema-value [this] (vec (:schemas this)))

  schema.core.Either
  (schema-value [this] (vec (:schemas this)))

  #?@(:clj [schema.core.Recursive
            (schema-value [this] @(:derefable this))])

  ; schema.core.Predicate
  ; (schema-value [this] (:p? this))

  schema.core.NamedSchema
  (schema-value [this] (:schema this))

  schema.core.ConditionalSchema
  (schema-value [this] (vec (map second (:preds-and-schemas this))))

  schema.core.CondPre
  (schema-value [this] (vec (:schemas this)))

  schema.core.Constrained
  (schema-value [this] (:schema this))

  schema.core.EnumSchema
  (schema-value [this] (:vs this))

  #?(:clj Object :cljs default)
  (schema-value [this] this)

  nil
  (schema-value [_] nil))

;;
;; Default
;;

(defrecord Default [schema default]
  s/Schema
  (spec [_]
    (variant/variant-spec spec/+no-precondition+ [{:schema schema}]))
  (explain [_]
    (list 'default (s/explain schema) default)))

(def default? (partial instance? Default))

(defn default [schema default]
  (s/validate schema default)
  (->Default schema default))
