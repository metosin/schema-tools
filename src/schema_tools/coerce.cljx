(ns schema-tools.coerce
  (:require [schema.core :as s]
            [schema.utils :as su]
            #+clj [schema.macros :as sm]
            #+clj [schema.macros :as sm]
            [schema.coerce :as sc])
  #+cljs (:require-macros [schema.macros :as sm]))


;;
;; Matchers
;;

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
