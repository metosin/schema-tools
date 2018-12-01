(ns schema-tools.impl
  (:require [schema.core :as s]
            [schema.spec.variant :as variant]
            [schema.spec.core :as spec]))

(defn unlift-keys [data ns-name]
  (reduce
    (fn [acc [k v]]
      (if (= ns-name (namespace k))
        (assoc acc (keyword (name k)) v)
        acc))
    {} data))

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

(defrecord Default [schema value]
  s/Schema
  (spec [_]
    (variant/variant-spec spec/+no-precondition+ [{:schema schema}]))
  (explain [_]
    (list 'default (s/explain schema) value)))

(def default? (partial instance? Default))

(defn default [schema value]
  (s/validate schema value)
  (->Default schema value))
