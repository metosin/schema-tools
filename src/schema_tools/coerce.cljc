(ns schema-tools.coerce
  (:require [schema.core :as s]
            [schema.spec.core :as ss]
            [schema.utils :as su]
            [schema.coerce :as sc]))

;;
;; Internals
;;

(defn- coerce-or-error! [value schema coercer type]
  (let [coerced (coercer value)]
    (if-let [error (su/error-val coerced)]
      (throw
        (ex-info
          (str "Could not coerce value to schema: " (pr-str error))
          {:type type :schema schema :value value :error error}))
      coerced)))

; original: https://gist.github.com/abp/0c4106eba7b72802347b
(defn- filter-schema-keys
  [m schema-keys extra-keys-checker]
  (reduce-kv (fn [m k _]
               (if (or (contains? schema-keys k)
                       (and extra-keys-checker
                            (not (su/error? (extra-keys-checker k)))))
                 m
                 (dissoc m k)))
             m
             m))

;;
;; Matchers
;;

; original: https://gist.github.com/abp/0c4106eba7b72802347b
(defn map-filter-matcher
  "Creates a matcher which removes all illegal keys from non-record maps."
  [schema]
  (when (and (map? schema) (not (record? schema)))
    (let [extra-keys-schema  (s/find-extra-keys-schema schema)
          extra-keys-checker (when extra-keys-schema
                               (ss/run-checker (fn [s params]
                                                 (ss/checker (s/spec s) params))
                                               true
                                               extra-keys-schema))
          explicit-keys      (some->> (dissoc schema extra-keys-schema)
                                      keys
                                      (mapv s/explicit-schema-key)
                                      set)]
      (when (or extra-keys-checker (seq explicit-keys))
        (fn [x]
          (if (map? x)
            (filter-schema-keys x explicit-keys extra-keys-checker)
            x))))))

(defn or-matcher
  "Creates a matcher where the first matcher matching the
  given schema is used."
  [& matchers]
  (fn [schema]
    (some #(% schema) matchers)))

;; alpha
(defn ^:no-doc forwarding-matcher
  "Creates a matcher where all matchers are combined with OR,
  but if the lead-matcher matches, it creates a sub-coercer and
  forwards the coerced value to tail-matchers."
  [lead-matcher & tail-matchers]
  (let [match-tail (apply or-matcher tail-matchers)]
    (or-matcher
      (fn [schema]
        (if-let [f (lead-matcher schema)]
          (fn [x]
            (let [x1 (f x)]
              ; don't sub-coerce untouched values
              (if (and x1 (not= x x1))
                (let [coercer (sc/coercer schema match-tail)]
                  (coercer x1))
                x1)))))
      match-tail)))

;;
;; coercion
;;

(defn coercer
  "Produce a function that simultaneously coerces and validates a value against a `schema.`
  If a value can't be coerced to match the schema, an `ex-info` is thrown - like `schema.core/validate`,
  but with overridable `:type`, defaulting to `:schema-tools.coerce/error.`"
  ([schema matcher]
   (coercer schema matcher ::error))
  ([schema matcher type]
   (let [coercer (sc/coercer schema matcher)]
     (fn [value]
       (coerce-or-error! value schema coercer type)))))

(defn coerce
  "Simultaneously coerces and validates a value to match the given `schema.` If a `value` can't
  be coerced to match the `schema`, an `ex-info` is thrown - like `schema.core/validate`,
  but with overridable `:type`, defaulting to `:schema-tools.coerce/error.`"
  ([value schema matcher]
    (coerce value schema matcher ::error))
  ([value schema matcher type]
   ((coercer schema matcher type) value)))
