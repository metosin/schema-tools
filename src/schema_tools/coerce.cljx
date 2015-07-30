(ns schema-tools.coerce
  (:require [schema.core :as s]
            [schema.utils :as su]
            #+clj [schema.macros :as sm]
            #+clj [schema.macros :as sm]
            [schema.coerce :as sc])
  #+cljs (:require-macros [schema.macros :as sm]))

;;
;; Internals
;;

(defn- coerce-or-error! [value schema coercer]
  (let [coerced (coercer value)]
    (if-let [error (su/error-val coerced)]
      (sm/error!
        (str "Could not coerce value to schema: " (pr-str error))
        {:schema schema :value value :error error})
      coerced)))

; original: https://gist.github.com/abp/0c4106eba7b72802347b
(defn- filter-schema-keys
  [m schema-keys extra-keys-walker]
  (reduce-kv (fn [m k _]
               (if (or (contains? schema-keys k)
                       (and extra-keys-walker
                            (not (su/error? (extra-keys-walker k)))))
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
  (if (and (map? schema) (not (record? schema)))
    (let [extra-keys-schema (s/find-extra-keys-schema schema)
          extra-keys-walker (if extra-keys-schema (s/walker extra-keys-schema))
          explicit-keys (some->> (dissoc schema extra-keys-schema)
                                 keys
                                 (mapv s/explicit-schema-key)
                                 set)]
      (if (or extra-keys-walker (seq explicit-keys))
        (fn [x]
          (if (map? x)
            (filter-schema-keys x explicit-keys extra-keys-walker)
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
  "Produce a function that simultaneously coerces and validates a value against a schema.
  If a value can't be coerced to match the schema, an ex-info is thrown (like schema.core/validate)."
  [schema matcher]
  (let [coercer (sc/coercer schema matcher)]
    (fn [value]
      (coerce-or-error! value schema coercer))))

(defn coerce
  "Simultaneously coerces and validates a value to match the given schema. If a value can't
  be coerced to match the schema, an ex-info is thrown (like schema.core/validate). To get
  schema info as ex-data, one should use a coercer created with schema-tools.coerce/coercer."
  [value coercer]
  (coerce-or-error! value nil coercer))
