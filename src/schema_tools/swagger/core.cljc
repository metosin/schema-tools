(ns schema-tools.swagger.core
  (:require [clojure.walk :as walk]
            [schema-tools.core]
            [schema.utils :as su]
            [schema.core :as s]))

;;
;; common
;;

(declare transform)

(defn remove-empty-keys [m]
  (into (empty m) (filter (comp not nil? val) m)))

(defn record-schema [x]
  (if-let [schema (some-> x su/class-schema :schema)]
    (s/named schema (str (.getSimpleName ^Class x) "Record"))))

(defn plain-map? [x]
  (and (map? x)
       (not (record? x))))

(defn required-keys [schema]
  (filterv s/required-key? (keys schema)))

(defn schema-name [schema opts]
  (if-let [name (some->
                  (or
                    (:name opts)
                    (s/schema-name schema)
                    (if (instance? schema.core.NamedSchema schema)
                      (:name schema)))
                  (name))]
    (let [ns (s/schema-ns schema)]
      (if ns (str ns "/" name) name))))

(defn key-name [x]
  (if (keyword? x)
    (let [n (namespace x)]
      (str (if n (str n "/")) (name x)))
    x))

(defn assoc-collection-format [m {:keys [in] :as options}]
  (cond-> m
          (#{:query :formData} in)
          (assoc :collectionFormat (:collection-format options "multi"))))

(defn not-supported! [schema]
  (throw
    (ex-info
      (str "don't know how to convert " schema " into a Swagger Schema. ")
      {:schema schema})))

(defn maybe? [schema]
  (instance? schema.core.Maybe schema))

#_(defn reference? [m]
    (contains? m :$ref))

#_(defn reference [e {:keys [ignore-missing-mappings?]}]
    (if-let [schema-name (s/schema-name e)]
      {:$ref (str "#/definitions/" schema-name)}
      (if-not ignore-missing-mappings?
        (not-supported! e))))

(defn- collection-schema [e options]
  (-> {:type "array"
       :items (transform (first e) (assoc options ::no-meta true))}
      (assoc-collection-format options)))

(defn properties [schema opts]
  (some->> (for [[k v] schema
                 :when (s/specific-key? k)
                 :let [_ (println k "...." v "=>" (transform v opts))
                       v (transform v opts)]]
             (and v [(key-name (s/explicit-schema-key k)) v]))
           (seq) (into (empty schema))))

(defn additional-properties [schema]
  (if-let [extra-key (s/find-extra-keys-schema schema)]
    (let [v (get schema extra-key)]
      (transform v nil))
    false))

(defn object-schema [this opts]
  (if (plain-map? this)
    (remove-empty-keys
      {:type "object"
       :title (schema-name this opts)
       :properties (properties this opts)
       :additionalProperties (additional-properties this)
       :required (some->> (filterv s/required-key? (keys this)) seq (mapv key-name))})))

;;
;; transformations
;;

(defmulti transform-pred (fn [this _] this) :default ::default)
(defmethod transform-pred string? [_ _] {:type "string"})
(defmethod transform-pred integer? [_ _] {:type "integer" :format "int32"})
(defmethod transform-pred keyword? [_ _] {:type "string"})
(defmethod transform-pred symbol? [_ _] {:type "string"})

(defmethod transform-pred ::default [e {:keys [ignore-missing-mappings?]}]
  (if-not [ignore-missing-mappings?]
    (not-supported! e)))

(defmulti transform-type (fn [c _] c) :default ::default)
#?(:clj (defmethod transform-type java.lang.Integer [_ _] {:type "integer" :format "int32"}))
#?(:clj (defmethod transform-type java.lang.Long [_ _] {:type "integer" :format "int64"}))
#?(:clj (defmethod transform-type java.lang.Double [_ _] {:type "number" :format "double"}))
#?(:clj (defmethod transform-type java.lang.Number [_ _] {:type "number" :format "double"}))
#?(:clj (defmethod transform-type java.lang.String [_ _] {:type "string"}))
#?(:clj (defmethod transform-type java.lang.Boolean [_ _] {:type "boolean"}))
#?(:clj (defmethod transform-type clojure.lang.Keyword [_ _] {:type "string"}))


(defmethod transform-type #?(:clj  clojure.lang.Keyword
                             :cljs cljs.core.Keyword) [_ _] {:type "string"})

#?(:clj (defmethod transform-type clojure.lang.Symbol [_ _] {:type "string"}))
#?(:clj (defmethod transform-type java.util.UUID [_ _] {:type "string" :format "uuid"}))
#?(:clj (defmethod transform-type java.util.Date [_ _] {:type "string" :format "date-time"}))
#?(:clj (defmethod transform-type java.time.Instant [_ _] {:type "string" :format "date-time"}))
#?(:clj (defmethod transform-type java.time.LocalDate [_ _] {:type "string" :format "date"}))
#?(:clj (defmethod transform-type java.time.LocalTime [_ _] {:type "string" :format "time"}))
#?(:clj (defmethod transform-type java.util.regex.Pattern [_ _] {:type "string" :format "regex"}))
#?(:clj (defmethod transform-type java.io.File [_ _] {:type "file"}))

#_(defmethod transform-type ::default [e {:keys [ignore-missing-mappings?]}]
    (if-not [ignore-missing-mappings?]
      (not-supported! e)))

(defprotocol SwaggerSchema
  (-transform [this opts]))

(defn transform [schema opts]
  (if (satisfies? SwaggerSchema schema)
    (-transform schema opts)
    (if-let [rschema (record-schema schema)]
      (transform rschema opts)
      (transform-type schema opts))))

(extend-protocol SwaggerSchema

  nil
  (-transform [_ _])

  schema_tools.core.Schema
  (-transform [this opts]
    (transform (:schema this) (merge opts (select-keys (:data this) [:name :description]))))

  #?@(:clj
      [java.util.regex.Pattern
       (-transform [this _]
         {:type "string" :pattern (str this)})])

  schema.core.Both
  (-transform [this options]
    (transform (first (:schemas this)) options))

  schema.core.Predicate
  (-transform [this options]
    (println "pred:" (:p? this))
    (transform-pred (:p? this) options))

  schema.core.EnumSchema
  (-transform [this options]
    (assoc (transform (type (first (:vs this))) options) :enum (vec (:vs this))))

  schema.core.Maybe
  (-transform [e {:keys [in] :as opts}]
    (let [schema (transform (:schema e) opts)]
      (condp contains? in
        #{:query :formData} (assoc schema :allowEmptyValue true)
        #{nil :body} (assoc schema :x-nullable true)
        schema)))

  schema.core.Either
  (-transform [this opts]
    (transform (first (:schemas this)) opts))

  #_#_schema.core.Recursive
      (-transform [this opts]
                  (transform (:derefable this) opts))

  schema.core.EqSchema
  (-transform [this opts]
    (transform (type (:v this)) opts))

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
    {:x-oneOf (vec (keep (comp #(transform % opts) second) (:preds-and-schemas this)))})

  schema.core.CondPre
  (-transform [this opts]
    {:x-oneOf (mapv #(transform % opts) (:schemas this))})

  schema.core.Constrained
  (-transform [this opts]
    (transform (:schema this) opts))

  schema.core.NamedSchema
  (-transform [{:keys [schema name]} opts]
    (transform schema (assoc opts :name name)))

  #?@(:clj
      [clojure.lang.Sequential
       (-transform [this options]
         (collection-schema this options))])

  #?@(:clj
      [clojure.lang.IPersistentSet
       (-transform [this options]
         (assoc (collection-schema this options) :uniqueItems true))])

  #?(:clj  clojure.lang.PersistentArrayMap
     :cljs cljs.core.PersistentArrayMap)
  (-transform [this opts]
    (object-schema this opts))

  #?(:clj  clojure.lang.PersistentHashMap
     :cljs cljs.core.PersistentHashMap)
  (-transform [this opts]
    (object-schema this opts)))

;;
;; extract swagger2 parameters
;;

(defmulti extract-parameter (fn [in _] in))

(defmethod extract-parameter :body [_ schema]
  (let [swagger (transform schema {:in :body, :type :parameter})]
    [{:in "body"
      :name (or (schema-name schema nil) "body")
      :description ""
      :required (not (maybe? schema))
      :schema swagger}]))

(defmethod extract-parameter :default [in schema]
  (let [{:keys [properties required]} (transform schema {:in in, :type :parameter})]
    (mapv
      (fn [[k {:keys [type] :as swagger}]]
        (merge
          {:in (name in)
           :name (key-name k)
           :description ""
           :type type
           :required (contains? (set required) k)}
          swagger))
      properties)))

;;
;; expand the spec
;;

(defmulti expand (fn [k _ _ _] k))

(defmethod expand ::responses [_ v acc _]
  {:responses
   (into
     (or (:responses acc) {})
     (for [[status response] v]
       [status (-> response
                   (update :schema transform {:type :schema})
                   (update :description (fnil identity ""))
                   (remove-empty-keys))]))})

(defmethod expand ::parameters [_ v acc _]
  (let [old (or (:parameters acc) [])
        new (mapcat (fn [[in spec]] (extract-parameter in spec)) v)
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

(defn expand-qualified-keywords [x options]
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
;; generate the swagger spec
;;

(defn swagger-spec
  "Transforms data into a swagger2 spec. Input data must conform
  to the Swagger2 Spec (http://swagger.io/specification/) with a
  exception that it can have any qualified keywords that are expanded
  with the `schema-tools.swagger.core/expand` multimethod."
  ([x]
   (swagger-spec x nil))
  ([x options]
   (expand-qualified-keywords x options)))
