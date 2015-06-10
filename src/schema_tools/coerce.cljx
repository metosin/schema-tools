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

;;
;; Matchers
;;

(defn or-matcher
  "Creates a matcher where the first matcher matching the
  given schema is used."
  [& matchers]
  (fn [schema]
    (some #(if-let [match (% schema)] match) matchers)))

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
