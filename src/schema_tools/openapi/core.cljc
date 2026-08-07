(ns schema-tools.openapi.core
  #?@
   (:clj
    [(:require
      [clojure.string :as str]
      [clojure.walk :as walk]
      [schema-tools.impl :as impl]
      [schema.core :as s]
      [schema.utils :as su])]
    :cljs
    [(:require
      [clojure.string :as str]
      [clojure.walk :as walk]
      [schema-tools.impl :as impl]
      [schema.core :as s]
      [schema.utils :as su])]))

;;
;; common
;;

(declare transform)

(defn record-schema
  [x]
  (when-let [schema (some-> x su/class-schema :schema)]
    (let [name #?(:clj (.getSimpleName ^Class x),
                  :cljs (some-> su/class-schema :klass pr-str (str/split "/") last))]
      (s/schema-with-name schema (str name "Record")))))

(defn- collection-schema
  [e options]
  (-> {:type  "array"
       :items (if (not (next e))
                (transform (first e) (assoc options ::no-meta true))
                {:oneOf (-> #(transform % (assoc options ::no-meta true))
                            (mapv e)
                            (set)
                            (vec))})}))

(defn plain-map?
  [m]
  (and (map? m)
       (not (record? m))))

(defn remove-empty-keys
  [m]
  (into (empty m) (filter (comp not nil? val) m)))

(defn schema-name
  [schema _opts]
  (when-let [name (some->
                   (or (s/schema-name schema)
                       (when (instance? #?(:clj schema.core.NamedSchema
                                           :cljs s/NamedSchema)
                                        schema)
                         (:name schema)))
                   (name))]
    (let [ns (s/schema-ns schema)]
      (if ns (str ns "/" name) name))))

(defn key-name
  [k]
  (if (keyword? k)
    (let [n (namespace k)]
      (str (and n (str n "/")) (name k)))
    k))

(defn properties
  [schema opts]
  (some->> (for [[k v] schema
                 :when (s/specific-key? k)
                 :let  [v (transform v opts)]]
             (and v [(key-name (s/explicit-schema-key k)) v]))
           (seq)
           (into (empty schema))))

(defn additional-properties
  [schema]
  (if-let [extra-key (s/find-extra-keys-schema schema)]
    (let [v (get schema extra-key)]
      (transform v nil))
    false))

(defn object-schema
  [this opts]
  (when (plain-map? this)
    (remove-empty-keys
     {:type                 "object"
      :title                (schema-name this opts)
      :properties           (properties this opts)
      :additionalProperties (additional-properties this)
      :required             (some->> (filterv s/required-key? (keys this))
                                     (seq)
                                     (mapv key-name))})))

(defn not-supported!
  [schema]
  (throw (ex-info (str "don't know how to convert " schema " into a OpenAPI schema. ")
                  {:schema schema})))

;;
;; transformations
;;

(defmulti transform-pred (fn [pred _] pred) :default ::default)

(defmethod transform-pred string?
  [_ _]
  {:type "string"})

(defmethod transform-pred integer?
  [_ _]
  {:type "integer" :format "int32"})

(defmethod transform-pred keyword?
  [_ _]
  {:type "string"})

(defmethod transform-pred symbol?
  [_ _]
  {:type "string"})

(defmethod transform-pred pos?
  [_ _]
  {:type "number" :minimum 0 :exclusiveMinimum true})

(defmethod transform-pred neg?
  [_ _]
  {:type "number" :maximum 0 :exclusiveMaximum true})

(defmethod transform-pred even?
  [_ _]
  {:type "number" :multipleOf 2})

(defmethod transform-pred ::default
  [e {:keys [ignore-missing-mappings?]}]
  (when-not ignore-missing-mappings?
    (not-supported! e)))

(defmulti transform-type (fn [t _] t) :default ::default)

(defmethod transform-type #?(:clj java.lang.Boolean :cljs js/Boolean)
  [_ _]
  {:type "boolean"})

(defmethod transform-type #?(:clj java.lang.Number :cljs js/Number)
  [_ _]
  {:type "number" :format "double"})

(defmethod transform-type #?(:clj clojure.lang.Keyword :cljs cljs.core.Keyword)
  [_ _]
  {:type "string"})

(defmethod transform-type #?(:clj java.util.Date :cljs js/Date)
  [_ _]
  {:type "string" :format "date-time"})

(defmethod transform-type #?(:clj java.util.UUID :cljs cljs.core/UUID)
  [_ _]
  {:type "string" :format "uuid"})

(defmethod transform-type #?(:clj java.util.regex.Pattern :cljs schema.core.Regex)
  [_ _]
  {:type "string" :format "regex"})

(defmethod transform-type #?(:clj java.lang.String :cljs js/String)
  [_ _]
  {:type "string"})

#?(:clj (defmethod transform-type clojure.lang.Symbol
          [_ _]
          {:type "string"}))

#?(:clj (defmethod transform-type java.time.Instant
          [_ _]
          {:type "string" :format "date-time"}))

#?(:clj (defmethod transform-type java.time.LocalDate
          [_ _]
          {:type "string" :format "date"}))

#?(:clj (defmethod transform-type java.time.LocalTime
          [_ _]
          {:type "string" :format "time"}))

#?(:clj (defmethod transform-type java.io.File
          [_ _]
          {:type "file"}))

#?(:clj (defmethod transform-type java.lang.Integer
          [_ _]
          {:type "integer" :format "int32"}))

#?(:clj (defmethod transform-type java.lang.Long
          [_ _]
          {:type "integer" :format "int64"}))

#?(:clj (defmethod transform-type java.lang.Double
          [_ _]
          {:type "number" :format "double"}))

#?(:cljs (defmethod transform-type goog.date.Date
           [_ _]
           {:type "string" :format "date"}))

#?(:cljs (defmethod transform-type goog.date.UtcDateTime
           [_ _]
           {:type "string" :format "date-time"}))

(defmethod transform-type ::default
  [t {:keys [ignore-missing-mappings?]}]
  (when-not ignore-missing-mappings?
    (not-supported! t)))

(defprotocol OpenapiSchema
  (-transform [this opts]))

(def ref-root "#/components/schemas/")

(defn- ref-name
  [name]
  (str/replace name "/" "."))

(defn- transform-one
  [schema opts]
  (if (satisfies? OpenapiSchema schema)
    (-transform schema opts)
    (if-let [rschema (record-schema schema)]
      (transform rschema opts)
      (transform-type schema opts))))

(defn transform
  [schema opts]
  (let [inline? (::inline? opts)
        toplevel? (nil? (::definitions opts))
        opts (-> opts
                 (update ::definitions #(or % (atom {})))
                 (dissoc ::inline?))
        definitions (::definitions opts)
        name (some-> (schema-name schema opts) ref-name)
        transformed (cond
                      (or inline? (not name))
                      (transform-one schema opts)

                      (get @definitions name)
                      {:$ref (str ref-root name)}

                      :else
                      (do
                        (swap! definitions assoc name ::recursion-stopper)
                        (swap! definitions assoc name (transform-one schema opts))
                        {:$ref (str ref-root name)}))]
    (cond-> transformed (and toplevel? (seq @definitions)) (assoc :definitions @definitions))))

(defn transform-inline
  [schema opts]
  (transform schema (assoc opts ::inline? true)))

(extend-protocol OpenapiSchema

  nil
  (-transform [_ _])

  schema_tools.core.Schema
  (-transform [{:keys [schema data]} opts]
    (or (:openapi data)
        (merge
         (transform (if-let [name (:name data)]
                      (s/schema-with-name schema name)
                      schema)
                    opts)
         (select-keys data [:description])
         (impl/unlift-keys data "openapi"))))

  #?(:clj  java.util.regex.Pattern
     :cljs js/RegExp)
  (-transform [this _]
    {:type "string" :pattern (str #?(:clj this, :cljs (.-source this)))})

  schema.core.Both
  (-transform [this opts]
    {:allOf (mapv #(transform % opts) (:schemas this))})

  schema.core.Predicate
  (-transform [this opts]
    (transform-pred (:p? this) opts))

  schema.core.EnumSchema
  (-transform [this opts]
    (assoc (transform (type (first (:vs this))) opts) :enum (vec (:vs this))))

  schema.core.Maybe
  (-transform [this opts]
    {:oneOf [(transform (:schema this) opts)
             {:type "null"}]})

  schema.core.Either
  (-transform [this opts]
    {:oneOf (mapv #(transform % opts) (:schemas this))})

  schema.core.Recursive
  (-transform [this opts]
    (transform @(:derefable this) opts))

  schema.core.EqSchema
  (-transform [this opts]
    {:enum [(:v this)]})

  schema.core.One
  (-transform [this opts]
    (transform (:schema this) opts))

  schema.core.AnythingSchema
  (-transform [_ {:keys [in] :as opts}]
    (if (and in (not= :body in))
      (transform (s/maybe s/Str) opts)
      {}))

  schema.core.ConditionalSchema
  (-transform [this opts]
    {:oneOf (-> #(transform % opts)
                (comp second)
                (keep (:preds-and-schemas this))
                (vec))})

  schema.core.CondPre
  (-transform [this opts]
    {:oneOf (mapv #(transform % opts) (:schemas this))})

  schema.core.Constrained
  (-transform [this opts]
    (transform (:schema this) opts))

  schema.core.NamedSchema
  (-transform [{:keys [schema name]} opts]
    (transform-inline (s/schema-with-name schema name) opts))

  #?(:clj  clojure.lang.Sequential
     :cljs cljs.core/List)
  (-transform [this options]
    (collection-schema this options))

  #?(:clj  clojure.lang.IPersistentSet
     :cljs cljs.core/PersistentHashSet)
  (-transform [this options]
    (assoc (collection-schema this options) :uniqueItems true))

  #?(:clj  clojure.lang.APersistentVector
     :cljs cljs.core.PersistentVector)
  (-transform [this options]
    (collection-schema this options))

  #?(:clj  clojure.lang.PersistentArrayMap
     :cljs cljs.core.PersistentArrayMap)
  (-transform [this opts]
    (object-schema this opts))

  #?(:clj  clojure.lang.PersistentHashMap
     :cljs cljs.core.PersistentHashMap)
  (-transform [this opts]
    (object-schema this opts)))
