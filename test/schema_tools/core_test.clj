(ns schema-tools.core-test
  (:require [midje.sweet :refer :all]
            [schema-tools.core :as st]
            [schema.core :as s]))

(fact st/dissoc
  (st/dissoc {:a s/Str
              (s/optional-key :b) s/Str
              (s/required-key :c) s/Str
              :d s/Str} :a :b :c) => {:d s/Str})
