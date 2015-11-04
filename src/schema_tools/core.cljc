(ns schema-tools.core
  (:require [schema.core :as s]
            [schema-tools.coerce :as stc]
            [schema-tools.util :as stu])
  (:refer-clojure :exclude [assoc dissoc select-keys update get-in assoc-in update-in merge]))

(defn- explicit-key [k] (if (s/specific-key? k) (s/explicit-schema-key k) k))

(defn- explicit-key-set [ks]
  (reduce (fn [s k] (conj s (explicit-key k))) #{} ks))

(defn- key-in-schema [m k]
  (cond
    (contains? m k) k
    (contains? m (s/optional-key k)) (s/optional-key k)
    (contains? m (s/required-key k)) (s/required-key k)
    (and (s/specific-key? k) (contains? m (s/explicit-schema-key k))) (s/explicit-schema-key k)
    :else k))

(defn- get-in-schema [m k & [default]]
  (get m (key-in-schema m k) default))

(defn- maybe-anonymous [original current]
  (if (= original current)
    original
    (vary-meta
      current
      (fn [meta]
        (let [new-meta (clojure.core/dissoc meta :name :ns)]
          (if (empty? new-meta)
            nil
            new-meta))))))

(defn- transform-keys
  [schema f ks]
  (assert (or (not ks) (vector? ks)) "input should be nil or a vector of keys.")
  (maybe-anonymous
    schema
    (let [ks? (explicit-key-set ks)]
      (stu/map-keys
        (fn [k]
          (cond
            (and ks (not (ks? (explicit-key k)))) k
            (s/specific-key? k) (f (s/explicit-schema-key k))
            :else (f k)))
        schema))))
;;
;; Definitions
;;

(def AnyKeys {s/Any s/Any})
(defn any-keys [] AnyKeys)

(def AnyKeywordKeys {s/Keyword s/Any})
(defn any-keyword-keys [& schemas] (apply clojure.core/merge AnyKeywordKeys schemas))

;;
;; Core functions
;;

(defn assoc
  "Assoc[iate]s key & vals into Schema."
  [schema & kvs]
  (maybe-anonymous
    schema
    (reduce
      (fn [schema [k v]]
        #?(:clj (when-not v
                  (throw (IllegalArgumentException.
                           "assoc expects even number of arguments after map/vector, found odd number"))))
        (let [rk (key-in-schema schema k)]
          (-> schema
              (clojure.core/dissoc rk)
              (clojure.core/assoc k v))))
      schema
      (partition 2 2 nil kvs))))

(defn dissoc
  "Dissoc[iate]s keys from Schema."
  [schema & ks]
  (maybe-anonymous
    schema
    (reduce
      (fn [schema k] (clojure.core/dissoc schema (key-in-schema schema k)))
      schema ks)))

(defn select-keys
  "Like `clojure.core/select-keys` but handles boths optional-keys and required-keys."
  [schema ks]
  (maybe-anonymous
    schema
    (let [ks? (explicit-key-set ks)]
      (into {} (filter (comp ks? explicit-key key) schema)))))

(defn get-in
  "Returns the value in a nested associative Schema,
  where `ks` is a sequence of keys. Returns `nil` if the key
  is not present, or the `not-found` value if supplied."
  ([m ks]
   (get-in m ks nil))
  ([m ks not-found]
   (loop [sentinel #?(:clj (Object.) :cljs (js/Object.))
          m m
          ks (seq ks)]
     (if ks
       (let [k (first ks)]
         (let [m (get-in-schema m k sentinel)]
           (if (identical? sentinel m)
             not-found
             (recur sentinel m (next ks)))))
       m))))

(defn assoc-in
  "Associates a value in a nested associative Schema, where `ks` is a
  sequence of keys and `v` is the new value and returns a new nested Schema.
  If any levels do not exist, hash-maps will be created."
  [schema [k & ks] v]
  (maybe-anonymous
    schema
    (let [kis (key-in-schema schema k)]
      (if ks
        (clojure.core/assoc schema kis (assoc-in (get-in-schema schema k) ks v))
        (clojure.core/assoc schema kis v)))))

(defn update-in
  "'Updates' a value in a nested associative Schema, where `ks` is a
  sequence of keys and `f` is a function that will take the old value
  and any supplied args and return the new value, and returns a new
  nested Schema. If any levels do not exist, hash-maps will be
  created."
  [schema [k & ks] f & args]
  (maybe-anonymous
    schema
    (let [kis (key-in-schema schema k)]
      (if ks
        (clojure.core/assoc schema kis (apply update-in (get-in-schema schema k) ks f args))
        (clojure.core/assoc schema kis (apply f (get-in-schema schema k) args))))))

;; (c) original https://github.com/clojure/core.incubator/blob/master/src/main/clojure/clojure/core/incubator.clj
(defn dissoc-in
  "Dissociates an entry from a nested associative Schema returning a new
  nested structure. keys is a sequence of keys. Any empty maps that result
  will not be present in the new Schema."
  [schema [k & ks]]
  (let [k (key-in-schema schema k)]
    (if ks
      (if-let [nextmap (get schema k)]
        (let [newmap (dissoc-in nextmap ks)]
          (if (seq newmap)
            (clojure.core/assoc schema k newmap)
            (dissoc schema k)))
        schema)
      (dissoc schema k))))

(defn update
  "Updates a value in a map with a function."
  [schema k f & args]
  (apply update-in schema [k] f args))

(defn merge
  "Returns a Schema that consists of the rest of the Schemas conj-ed onto
  the first. If a schema key occurs in more than one map, the mapping from
  the latter (left-to-right) will be the mapping in the result. Works only
  with Map schemas."
  [& schemas]
  {:pre [(every? #(or (map? %) (nil? %)) schemas)]}
  (maybe-anonymous
    (first schemas)
    (when (some identity schemas)
      (reduce
        (fn [acc m]
          (reduce
            (fn [acc [k v]]
              (clojure.core/assoc (dissoc acc k) k v))
            acc m)) schemas))))

;;
;; Extras
;;

(defn select-schema
  "Strips all disallowed keys from nested Map schemas via coercion. Takes an optional
  coercion matcher for extra coercing the selected value(s) on a single sweep. If a value
  can't be coerced to match the schema `ExceptionInfo` is thrown (like `schema.core/validate`)."
  ([value schema]
   (select-schema value schema (constantly nil)))
  ([value schema matcher]

    ; temporary migration check for upgrading to 0.5.0+
   (try
     (s/explain schema)
     (catch #?(:clj Exception :cljs js/Error) _
       (throw (ex-info "Illegal argument order - breaking change in 0.5.0."
                       {:value value
                        :schema schema
                        :matcher matcher}))))

   (stc/coerce value schema (stc/or-matcher stc/map-filter-matcher matcher))))

(defn optional-keys
  "Makes given map keys optional. Defaults to all keys."
  ([m] (optional-keys m nil))
  ([m ks] (transform-keys m s/optional-key ks)))

(defn required-keys
  "Makes given map keys required. Defaults to all keys."
  ([m] (required-keys m nil))
  ([m ks] (transform-keys m #(if (keyword? %) % (s/required-key %)) ks)))

(defn schema-with-description
  "Records description in schema's metadata."
  [schema description]
  (vary-meta schema assoc :description description))

(defn schema-description
  "Returns the description of a schema attached via schema-with-description."
  [schema]
  (-> schema meta :description))

#?(:clj
   (defn resolve-schema
     "Returns the schema var if the schema contains the `:name` and `:ns`
     definitions (set by `schema.core/defschema`)."
     [schema]
     (if-let [schema-ns (s/schema-ns schema)]
       (ns-resolve schema-ns (s/schema-name schema)))))

#?(:clj
   (defn resolve-schema-description
     "Returns the schema description, in this lookup order:
     a) schema meta :description
     b) schema var meta :doc if not \"\"
     c) nil"
     [schema]
     (or (schema-description schema)
         (if-let [schema-ns (s/schema-ns schema)]
           (let [doc (-> (ns-resolve schema-ns (s/schema-name schema)) meta :doc)]
             (if-not (= "" doc) doc))))))
