(ns schema-tools.core-test
  (:require [midje.sweet :refer :all]
            [schema-tools.core :as st]
            [schema.core :as s]))

(fact st/any-keys
  (st/any-keys {:a s/Str}) => {:a s/Str, s/Keyword s/Any}
  (st/any-keys {s/Keyword s/Str}) => {s/Keyword s/Str})

(fact st/keyword-key?
   (st/keyword-key? :a) => true
   (st/keyword-key? (s/optional-key :a)) => true
   (st/keyword-key? (s/required-key :a)) => true
   (st/keyword-key? "a") => false
   (st/keyword-key? s/Keyword) => false)

(fact st/dissoc
  (let [schema {:a s/Str
                (s/optional-key :b) s/Str
                (s/required-key :c) s/Str
                "d" s/Str
                (s/optional-key [1 2 3]) s/Str
                s/Keyword s/Str}]
    (st/dissoc schema :a :b :c "d" [1 2 3] :e) => {s/Keyword s/Str}))

(fact st/select-keys
   (let [schema {:a s/Str
                 (s/optional-key :b) s/Str
                 (s/required-key :c) s/Str
                 "d" s/Str
                 (s/optional-key [1 2 3]) s/Str
                 s/Keyword s/Str}]
     (st/select-keys schema [:a :b :c "d" [1 2 3] :e]) => {:a s/Str
                                                           (s/optional-key :b) s/Str
                                                           (s/required-key :c) s/Str
                                                           "d" s/Str
                                                           (s/optional-key [1 2 3]) s/Str}))

(fact st/get-in
   (let [schema {:a {(s/optional-key :b) {(s/required-key :c) s/Str}}
                 "d" {s/Keyword s/Str}
                 [1 2 3] s/Str}]
     (st/get-in schema [:a (s/optional-key :b) (s/required-key :c)]) => s/Str
     (st/get-in schema [:a :b :c]) => s/Str
     (st/get-in schema ["d" s/Keyword]) => s/Str
     (st/get-in schema [[1 2 3]]) => s/Str
     (st/get-in schema [:e]) => nil
     (st/get-in schema [:e] s/Str) => s/Str
     (st/get-in schema [:e :a] {:a s/Str}) => {:a s/Str}))

(fact st/assoc-in
  (let [schema {:a {(s/optional-key [1 2 3]) {(s/required-key "d") {}}}}]
    (st/assoc-in schema [:a [1 2 3] "d" :e] s/Str)
    => {:a {(s/optional-key [1 2 3]) {(s/required-key "d") {:e s/Str}}}}))

(fact st/update-in
  (let [schema {:a {(s/optional-key [1 2 3]) {(s/required-key "d") s/Str}}}]
    (st/update-in schema [:a [1 2 3] "d"] (constantly s/Int))
    => {:a {(s/optional-key [1 2 3]) {(s/required-key "d") s/Int}}}))

(fact st/select-schema
  (let [schema {:a String
                :b {(s/optional-key [1 2 3]) {(s/required-key "d") String}}}
        value {:a "kikka"
               :b {[1 2 3] {"d" "kukka"
                            ":d" "kikka"
                            :d "kukka"}}}]
    (st/select-schema schema value) => {:a "kikka"
                                        :b {[1 2 3] {"d" "kukka"}}}))

(fact st/optional-keys
  (let [schema {(s/optional-key :a) s/Str
                (s/required-key :b) s/Str
                (s/required-key [1 2 3]) s/Str
                :c s/Str
                "d" s/Str}]

    (fact "without parameters transforms all keys"
      (keys (st/optional-keys schema)) => (just [(s/optional-key :a)
                                                 (s/optional-key :b)
                                                 (s/optional-key :c)
                                                 (s/optional-key [1 2 3])
                                                 (s/optional-key "d")] :in-any-order))

    (fact "ensures defined keys are optional"
      (keys (st/optional-keys schema :b [1 2 3] "d")) => (just [(s/optional-key :a)
                                                                (s/optional-key :b)
                                                                :c
                                                                (s/optional-key [1 2 3])
                                                                (s/optional-key "d")] :in-any-order))))

(fact st/required-keys
  (let [schema {(s/required-key :a) s/Str
                (s/optional-key :b) s/Str
                (s/optional-key [1 2 3]) s/Str
                :c s/Str
                "d" s/Str
                (s/optional-key :e) s/Str}]

    (fact "without parameters transforms all keys"
      (keys (st/required-keys schema)) => (just [:a
                                                 :b
                                                 :c
                                                 [1 2 3]
                                                 "d"
                                                 :e] :in-any-order))

    (fact "ensures defined keys are required"
      (keys (st/required-keys schema :b [1 2 3] "d")) => (just [:a
                                                                :b
                                                                :c
                                                                [1 2 3]
                                                                "d"
                                                                (s/optional-key :e)] :in-any-order))))
