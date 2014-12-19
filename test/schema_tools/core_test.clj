(ns schema-tools.core-test
  (:require [midje.sweet :refer :all]
            [schema-tools.core :as st]
            [schema.core :as s]))

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
                s/Keyword s/Str}]
    (st/dissoc schema :a :b :c :d) => {"d" s/Str
                                       s/Keyword s/Str}))

(fact st/select-keys
   (let [schema {:a s/Str
                 (s/optional-key :b) s/Str
                 (s/required-key :c) s/Str
                 "d" s/Str
                 (s/maybe :e) s/Str
                 s/Keyword s/Str}]
     (st/select-keys schema [:a :b :c :d]) => {:a s/Str
                                               (s/optional-key :b) s/Str
                                               (s/required-key :c) s/Str}))

(fact st/get-in
   (let [schema {:a {(s/optional-key :b) {(s/required-key :c) s/Str}}
                 "d" {s/Keyword s/Str}
                 "e" s/Str}]
     (st/get-in schema [:a (s/optional-key :b) (s/required-key :c)]) => s/Str
     (st/get-in schema [:a :b :c]) => s/Str
     (st/get-in schema ["d" s/Keyword]) => s/Str
     (st/get-in schema ["e"]) => s/Str
     (st/get-in schema [:e]) => nil
     (st/get-in schema [:e] s/Str) => s/Str))

(fact st/strip-keys
  (let [schema {:a String
                :b {(s/optional-key :c) {(s/required-key :d) String}}}
        value {:a "kikka"
               :b {:c {:d "kukka"
                       :d2 "kikka"
                       :d3 "kukka"}}}]
    (st/strip-keys schema value) => {:a "kikka"
                                     :b {:c {:d "kukka"}}}))

