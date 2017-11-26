(ns schema-tools.coerce
  (:require [schema.core :as s]
            [schema.spec.core :as ss]
            [schema.utils :as su]
            [schema.coerce :as sc]
            [schema-tools.core.impl :as impl]
    #?@(:clj  [
            clojure.edn]
        :cljs [[cljs.reader]
               [goog.date.UtcDateTime]]))
  #?(:clj
     (:import [java.util Date UUID]
              [java.util.regex Pattern]
              [java.time LocalDate LocalTime Instant]
              (clojure.lang APersistentSet Keyword))))

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
(defn- filter-schema-keys [m schema-keys extra-keys-checker]
  (reduce-kv
    (fn [m k _]
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
    (let [extra-keys-schema (s/find-extra-keys-schema schema)
          extra-keys-checker (when extra-keys-schema
                               (ss/run-checker
                                 (fn [s params]
                                   (ss/checker (s/spec s) params))
                                 true
                                 extra-keys-schema))
          explicit-keys (some->> (dissoc schema extra-keys-schema)
                                 keys
                                 (mapv s/explicit-schema-key)
                                 set)]
      (when (or extra-keys-checker (seq explicit-keys))
        (fn [x]
          (if (map? x)
            (filter-schema-keys x explicit-keys extra-keys-checker)
            x))))))

; original: https://groups.google.com/forum/m/#!topic/prismatic-plumbing/NWUnqbYhfac
(defn default-coercion-matcher [schema]
  (when (impl/default? schema)
    (fn [value]
      (if (nil? value) (:value schema) value))))

(defn multi-matcher
  "Creates a matcher for (accept-schema schema), reducing
  value with fs functions if (accept-value value)."
  [accept-schema accept-value fs]
  (fn [schema]
    (when (accept-schema schema)
      (fn [value]
        (if (accept-value value)
          (reduce #(%2 %1) value fs)
          value)))))

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
  ([schema]
   (coercer schema (constantly nil)))
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
  ([value schema]
   (coerce value schema (constantly nil)))
  ([value schema matcher]
   (coerce value schema matcher ::error))
  ([value schema matcher type]
   ((coercer schema matcher type) value)))

;;
;; coercions
;;

(defn safe-coerce-string [f]
  (fn [x]
    (if (string? x)
      (try
        (f x)
        (catch #?(:clj Exception, :cljs js/Error) _ x))
      x)))

(defn string->boolean [x]
  (if (string? x)
    (condp = x
      "true" true
      "false" false
      x)
    x))

#?(:clj
   (defn string->long [^String x]
     (if (string? x)
       (try
         (Long/valueOf x)
         (catch #?(:clj Exception, :cljs js/Error) _ x))
       x)))

#?(:clj
   (defn string->double [^String x]
     (if (string? x)
       (try
         (Double/valueOf x)
         (catch #?(:clj Exception, :cljs js/Error) _ x))
       x)))

(defn- safe-int [x]
  #?(:clj  (sc/safe-long-cast x)
     :cljs x))

(defn string->number [^String x]
  (if (string? x)
    (try
      (let [parsed #?(:clj  (clojure.edn/read-string x)
                      :cljs (cljs.reader/read-string x))]
        (if (number? parsed) parsed x))
      (catch #?(:clj Exception, :cljs js/Error) _ x))
    x))

(defn string->uuid [x]
  (if (string? x)
    (try
      #?(:clj  (UUID/fromString x)
         ;; http://stackoverflow.com/questions/7905929/how-to-test-valid-uuid-guid
         :cljs (if (re-find #"^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$" x)
                 (uuid x)
                 x))
      (catch #?(:clj Exception, :cljs js/Error) _ x))
    x))

(defn string->date [x]
  (if (string? x)
    (try
      #?(:clj  (Date/from (Instant/parse x))
         :cljs (js/Date. (.getTime (goog.date.UtcDateTime.fromIsoString x))))
      (catch #?(:clj Exception, :cljs js/Error) _ x))
    x))

(def +json-coercions+
  {s/Keyword sc/string->keyword
   #?@(:clj [Keyword sc/string->keyword])
   s/Uuid string->uuid
   s/Int safe-int
   #?@(:clj [Long sc/safe-long-cast])
   #?@(:clj [Double double])
   #?@(:clj [Pattern (safe-coerce-string re-pattern)])
   #?@(:clj [Date string->date])
   #?@(:cljs [js/Date string->date])
   #?@(:clj [LocalDate (safe-coerce-string #(LocalDate/parse %))])
   #?@(:clj [LocalTime (safe-coerce-string #(LocalTime/parse %))])
   #?@(:clj [Instant (safe-coerce-string #(Instant/parse %))])})

(def +string-coercions+
  {s/Int (comp safe-int string->number)
   s/Num string->number
   s/Bool string->boolean
   #?@(:clj [Long (comp safe-int string->long)])
   #?@(:clj [Double (comp double string->double)])})

#_(defn split-params-matcher [schema]
    (if (or (and (coll? schema) (not (record? schema))))
      (fn [x]
        (if (string? x)
          (str/split x #",")
          x))))

(defn multi-params-matcher
  "If only one parameter is provided to multi param, ring
   doesn't wrap the param is collection."
  [schema]
  (if (or (and (coll? schema) (not (record? schema))))
    (fn [x] (if (coll? x) x [x]))))

;;
;; matchers
;;

(def json-coercion-matcher
  (some-fn +json-coercions+
           sc/keyword-enum-matcher
           sc/set-matcher))

(def string-coercion-matcher
  (some-fn +string-coercions+
           ; split-params-matcher
           multi-params-matcher
           json-coercion-matcher))
