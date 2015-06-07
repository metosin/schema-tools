(ns schema-tools.coerce
  (:require [schema.core :as s]
            [schema.utils :as su]
            #+clj [schema.macros :as sm]
            #+clj [schema.macros :as sm]
            [schema.coerce :as sc])
  #+cljs (:require-macros [schema.macros :as sm]))

(defn safe-coercer
  "Produce a function that coerces a datum without validation."
  [schema coercion-matcher]
  (s/start-walker
    (su/memoize-id
      (fn [s]
        (let [walker (s/walker s)]
          (if-let [coercer (coercion-matcher s)]
            (fn [x]
              (sm/try-catchall
                (let [v (coercer x)]
                  (if (su/error? v)
                    x
                    (let [walked (walker v)]
                      (if (su/error? walked)
                        x
                        walked))))
                (catch t (sm/validation-error s x t))))
            walker))))
    schema))

(defn forwarding-matcher [matcher matcher2]
  (fn [schema]
    (if-let [f (matcher schema)]
      (fn [x]
        (if-let [x1 (f x)]
          (let [coercer (sc/coercer schema matcher2)]
            (coercer x1)))))))

(defn or-matcher [matcher matcher2]
  (fn [schema]
    (or (matcher schema) (matcher2 schema))))
