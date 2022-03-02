(ns schema-tools.openapi.core
  #?@
   (:clj
    [(:require
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
      (s/named schema (str name "Record")))))

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
  [schema opts]
  (when-let [name (some->
                   (or (:name opts)
                       (s/schema-name schema)
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
  (ex-info
   (str "don't know how to convert " schema " into a OpenAPI schema. ")
   {:schema schema}))

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

(defn transform
  [schema opts]
  (if (satisfies? OpenapiSchema schema)
    (-transform schema opts)
    (if-let [rschema (record-schema schema)]
      (transform rschema opts)
      (transform-type schema opts))))

(extend-protocol OpenapiSchema

  nil
  (-transform [_ _])

  schema_tools.core.Schema
  (-transform [{:keys [schema data]} opts]
    (merge
     (transform schema (merge opts (select-keys data [:name :description])))
     (impl/unlift-keys data "openapi")))

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

  #_#_schema.core.Recursive
  (-transform [this opts]
    (transform (:derefable this) opts))

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
    (transform schema (assoc opts :name name)))

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

;;
;; Extract OpenAPI parameters
;;

(defn- is-nilable?
  [spec]
  (and (contains? spec :oneOf)
       (= 2 (count (:oneOf spec)))
       (-> :type
           (group-by (:oneOf spec))
           (contains? "null"))))

(defn- extract-nilable
  [spec]
  (->> (:oneOf spec)
       (remove #(= (:type %) "null"))
       (first)))

(defn- extract-single-param
  [in spec]
  (let [nilable? (is-nilable? spec)
        new-spec (if nilable?
                   (extract-nilable spec)
                   spec)]
    {:name        (or (schema-name new-spec nil)
                      (:title new-spec)
                      (:type new-spec))
     :in          in
     :description ""
     :required    (case in
                    :path true
                    (not nilable?))
     :schema      new-spec}))

(defn- extract-object-param
  [in {:keys [properties required]}]
  (mapv
   (fn [[k schema]]
     {:name        (or (schema-name schema nil)
                       (key-name k))
      :in          (name in)
      :description ""
      :required    (case in
                     :path true
                     (contains? (set required) k))
      :schema      schema})
   properties))

(defn extract-parameter
  [in spec]
  (let [parameter-spec (transform spec nil)
        object?        (and (contains? parameter-spec :properties)
                            (= "object" (:type parameter-spec)))]
    (if object?
      (extract-object-param in parameter-spec)
      (-> (extract-single-param in parameter-spec) vector))))

;;
;; expand the spec
;;

(defmulti expand (fn [k _ _ _] k))

(defmethod expand ::schemas
  [_ v acc _]
  {:schemas
   (into
    (or (:schemas acc) {})
    (for [[name schema] v]
      {name (transform schema nil)}))})

(defmethod expand ::content
  [_ v acc _]
  {:content
   (into
    (or (:content acc) {})
    (for [[content-type schema] v]
      {content-type {:schema (transform schema nil)}}))})

(defmethod expand ::parameters
  [_ v acc _]
  (let [old    (or (:parameters acc) [])
        new    (mapcat (fn [[in spec]] (extract-parameter in spec)) v)
        merged (->> (into old new)
                    (reverse)
                    (reduce
                     (fn [[ps cache :as acc] p]
                       (let [c (select-keys p [:in :name])]
                         (if-not (cache c)
                           [(conj ps p) (conj cache c)]
                           acc)))
                     [[] #{}])
                    (first)
                    (reverse)
                    (vec))]
    {:parameters merged}))

(defmethod expand ::headers
  [_ v acc _]
  {:headers
   (into
    (or (:headers acc) {})
    (for [[name spec] v]
      {name (-> (extract-single-param :header (transform spec nil))
                (dissoc :in :name))}))})

(defn expand-qualified-keywords
  [x options]
  (let [accept? (set (keys (methods expand)))]
    (walk/postwalk
     (fn [x]
       (if (plain-map? x)
         (reduce-kv
          (fn [acc k v]
            (if (accept? k)
              (-> acc (dissoc k) (merge (expand k v acc options)))
              acc))
          x
          x)
         x))
     x)))

;;
;; Generate the OpenAPI spec
;;

(defn openapi-spec
  "Transform data into an OpenAPI spec. Input data must conform to the Swagger3
  Spec (https://swagger.io/specification/) with a exception that it can have
  any qualified keywords which are expanded with the
  `schema-tools.openapi.core/expand` multimethod."
  ([x]
   (openapi-spec x nil))
  ([x options]
   (expand-qualified-keywords x options)))
