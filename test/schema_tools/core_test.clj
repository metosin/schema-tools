(ns schema-tools.core-test
  (:require [midje.sweet :refer :all]
            [schema-tools.core :as st]
            [schema.core :as s]))

(fact "any-keys"
  (st/any-keys) => {s/Any s/Any}
  (fact "allows any keys"
    (s/check (st/any-keys) {"a" true, [1 2 3] true}) => nil))

(fact "any-keyword-keys"
  (st/any-keyword-keys) => {s/Keyword s/Any}
  (st/any-keyword-keys {s/Keyword s/Str}) => {s/Keyword s/Str}
  (st/any-keyword-keys {:a s/Str}) => {:a s/Str, s/Keyword s/Any}
  (fact "does not allow non-keyword-keys"
    (s/check (st/any-keyword-keys) {:a true, "b" true}) =not=> nil)
  (fact "allows any keyword-keys"
    (s/check (st/any-keyword-keys) {:a true, :b true}) => nil)
  (fact "can be used to extend schemas"
    (s/check (st/any-keyword-keys {(s/required-key "b") s/Bool}) {:a true, "b" true}) => nil))

(fact "dissoc"
  (let [schema {:a s/Str
                (s/optional-key :b) s/Str
                (s/required-key "c") s/Str
                s/Keyword s/Str}]
    (st/dissoc schema :a :b "c" :d) => {s/Keyword s/Str}))

(fact "select-keys"
  (let [schema {:a s/Str
                (s/optional-key :b) s/Str
                (s/required-key "c") s/Str
                s/Keyword s/Str}]
    (st/select-keys schema [:a :b "c" :d]) => {:a                   s/Str
                                               (s/optional-key :b)  s/Str
                                               (s/required-key "c") s/Str}))

(fact "get-in"
  (let [schema {:a {(s/optional-key :b) {(s/required-key :c) s/Str}}
                (s/optional-key "d") {s/Keyword s/Str}}]
    (st/get-in schema [:a (s/optional-key :b) (s/required-key :c)]) => s/Str
    (st/get-in schema [:a :b :c]) => s/Str
    (st/get-in schema ["d" s/Keyword]) => s/Str
    (st/get-in schema [:e]) => nil
    (fact "works with defaults"
      (st/get-in schema [:e] s/Str) => s/Str
      (st/get-in schema [:e :a] {:a s/Str}) => {:a s/Str})))

(fact "assoc-in"
  (let [schema {:a {(s/optional-key [1 2 3]) {(s/required-key "d") {}}}}]
    (st/assoc-in schema [:a [1 2 3] "d" :e :f] s/Str)
    => {:a {(s/optional-key [1 2 3]) {(s/required-key "d") {:e {:f s/Str}}}}}))

(fact "update-in"
  (let [schema {:a {(s/optional-key [1 2 3]) {(s/required-key "d") s/Str}}}]
    (st/update-in schema [:a [1 2 3] "d"] (constantly s/Int))
    => {:a {(s/optional-key [1 2 3]) {(s/required-key "d") s/Int}}}))

(fact "select-schema"
  (fact "with strictly defined schema, when value has extra keys"
    (let [schema {:a s/Str
                  :b {(s/optional-key [1 2 3]) {(s/required-key "d") s/Str}}}
          value {:a "kikka"
                 :b {[1 2 3] {"d"  "kukka"
                              ":d" "kikka"
                              :d   "kukka"}}}]
      (s/check schema value) =not=> nil
      (fact "select-schema drops unallowed keys"
        (st/select-schema schema value) => {:a "kikka"
                                            :b {[1 2 3] {"d" "kukka"}}}
        (s/check schema (st/select-schema schema value)) => nil)))

  (fact "with loosely defined schema, when value has extra keys"
    (let [schema {s/Keyword s/Str
                  :a {:b {s/Str s/Str}
                      :c {s/Any s/Str}}}
          value {:kikka "kukka"
                 :a {:b {"abba" "jabba"}
                     :c {[1 2 3] "kakka"}
                     :d :ILLEGAL-KEY}}]

      (s/check schema value) =not=> nil

      (fact "select-schema drops unallowed keys"
        (st/select-schema schema value) => {:kikka "kukka"
                                            :a {:b {"abba" "jabba"}
                                                :c {[1 2 3] "kakka"}}}
        (s/check schema (st/select-schema schema value)) => nil))))

(fact "with-optional-keys"
  (let [schema {(s/optional-key :a) s/Str
                (s/required-key :b) s/Str
                :c s/Str
                (s/required-key "d") s/Str}]

    (fact "without extra arguments makes all top-level keys optional"
      (keys (st/with-optional-keys schema))

      => (just [(s/optional-key :a)
                (s/optional-key :b)
                (s/optional-key :c)
                (s/optional-key "d")] :in-any-order))

    (fact "makes all given top-level keys are optional, ignoring missing keys"

      (st/with-optional-keys schema :NON-EXISTING) => schema

      (keys (st/with-optional-keys schema :a :b "d" :NON-EXISTING))
      => (just [(s/optional-key :a)
                (s/optional-key :b)
                :c
                (s/optional-key "d")] :in-any-order))))

(fact "with-required-keys"
  (let [schema {(s/required-key :a) s/Str
                (s/optional-key :b) s/Str
                :c s/Str
                (s/optional-key "d") s/Str}]

    (fact "without extra arguments makes all top-level keys required"
      (keys (st/with-required-keys schema))

      => (just [:a
                :b
                :c
                (s/required-key "d")] :in-any-order))

    (fact "makes all given top-level keys are required, ignoring missing keys"
      (st/with-required-keys schema :NON-EXISTING) => schema

      (keys (st/with-required-keys schema :b [1 2 3] "d" :NON-EXISTING))

      => (just [:a
                :b
                :c
                (s/required-key "d")] :in-any-order))))
