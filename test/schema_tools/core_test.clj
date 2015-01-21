(ns schema-tools.core-test
  (:require [clojure.test :refer [deftest testing is]]
            [schema-tools.core :as st]
            [schema.core :as s]))

(deftest any-keys-test
  (is (= (st/any-keys) {s/Any s/Any}))
  (testing "allows any keys"
    (is (= (s/check (st/any-keys) {"a" true, [1 2 3] true}) nil))))

(deftest any-keyword-keys-test
  (is (= (st/any-keyword-keys) {s/Keyword s/Any}))
  (is (= (st/any-keyword-keys {s/Keyword s/Str}) {s/Keyword s/Str}))
  (is (= (st/any-keyword-keys {:a s/Str}) {:a s/Str, s/Keyword s/Any}))
  (testing "does not allow non-keyword-keys"
    (is (s/check (st/any-keyword-keys) {:a true, "b" true})))
  (testing "allows any keyword-keys"
    (is (= (s/check (st/any-keyword-keys) {:a true, :b true}) nil)))
  (testing "can be used to extend schemas"
    (is (= (s/check (st/any-keyword-keys {(s/required-key "b") s/Bool}) {:a true, "b" true}) nil))))

(deftest dissoc-test
  (let [schema {:a s/Str
                (s/optional-key :b) s/Str
                (s/required-key "c") s/Str
                s/Keyword s/Str}]
    (is (= (st/dissoc schema :a :b "c" :d) {s/Keyword s/Str}))))

(deftest select-keys-test
  (let [schema {:a s/Str
                (s/optional-key :b) s/Str
                (s/required-key "c") s/Str
                s/Keyword s/Str}]
    (is (= (st/select-keys schema [:a :b "c" :d])
           {:a                   s/Str
            (s/optional-key :b)  s/Str
            (s/required-key "c") s/Str}))))

(deftest get-in-test
  (let [schema {:a {(s/optional-key :b) {(s/required-key :c) s/Str}}
                (s/optional-key "d") {s/Keyword s/Str}}]
    (is (= (st/get-in schema [:a (s/optional-key :b) (s/required-key :c)]) s/Str))
    (is (= (st/get-in schema [:a :b :c]) s/Str))
    (is (= (st/get-in schema ["d" s/Keyword]) s/Str))
    (is (= (st/get-in schema [:e]) nil))
    (testing "works with defaults"
      (is (= (st/get-in schema [:e] s/Str) s/Str))
      (is (= (st/get-in schema [:e :a] {:a s/Str}) {:a s/Str})))))

(deftest assoc-in-test
  (let [schema {:a {(s/optional-key [1 2 3]) {(s/required-key "d") {}}}}]
    (is (= (st/assoc-in schema [:a [1 2 3] "d" :e :f] s/Str)
           {:a {(s/optional-key [1 2 3]) {(s/required-key "d") {:e {:f s/Str}}}}}))))

(deftest update-in-test
  (let [schema {:a {(s/optional-key [1 2 3]) {(s/required-key "d") s/Str}}}]
    (is (= (st/update-in schema [:a [1 2 3] "d"] (constantly s/Int))
           {:a {(s/optional-key [1 2 3]) {(s/required-key "d") s/Int}}}))))

(deftest dissoc-in-test
  (let [schema {:a {(s/optional-key [1 2 3]) {(s/required-key "d") s/Str
                                              :kikka s/Str}}}]
    (is (= (st/dissoc-in schema [:a [1 2 3] "d"])
           {:a {(s/optional-key [1 2 3]) {:kikka s/Str}}}))

    (testing "resulting empty maps are removed"
      (is (= (st/dissoc-in schema [:a [1 2 3]]) {})))))

(deftest select-schema-test
  (testing "with strictly defined schema, when value has extra keys"
    (let [schema {:a s/Str
                  :b {(s/optional-key [1 2 3]) {(s/required-key "d") s/Str}}}
          value {:a "kikka"
                 :b {[1 2 3] {"d"  "kukka"
                              ":d" "kikka"
                              :d   "kukka"}}}]
      (is (s/check schema value))
      (testing "select-schema drops unallowed keys"
        (is (= (st/select-schema schema value) {:a "kikka"
                                                :b {[1 2 3] {"d" "kukka"}}}))
        (is (= (s/check schema (st/select-schema schema value)) nil)))))

  (testing "with loosely defined schema, when value has extra keys"
    (let [schema {s/Keyword s/Str
                  :a {:b {s/Str s/Str}
                      :c {s/Any s/Str}}}
          value {:kikka "kukka"
                 :a {:b {"abba" "jabba"}
                     :c {[1 2 3] "kakka"}
                     :d :ILLEGAL-KEY}}]

      (is (s/check schema value))

      (testing "select-schema drops unallowed keys"
        (is (= (st/select-schema schema value) {:kikka "kukka"
                                                :a {:b {"abba" "jabba"}
                                                    :c {[1 2 3] "kakka"}}}))
        (is (= (s/check schema (st/select-schema schema value)) nil))))))

(deftest with-optional-keys-test
  (let [schema {(s/optional-key :a) s/Str
                (s/required-key :b) s/Str
                :c s/Str
                (s/required-key "d") s/Str}]

    (testing "without extra arguments makes all top-level keys optional"
      (is (= (keys (st/with-optional-keys schema))
             [(s/optional-key :a) (s/optional-key :b) (s/optional-key :c) (s/optional-key "d")])))

    (testing "makes all given top-level keys are optional, ignoring missing keys"

      (is (= (st/with-optional-keys schema :NON-EXISTING) schema))

      (is (= (keys (st/with-optional-keys schema :a :b "d" :NON-EXISTING))
             [(s/optional-key :a) (s/optional-key :b) :c (s/optional-key "d")])))))

(deftest with-required-keys-test
  (let [schema {(s/required-key :a) s/Str
                (s/optional-key :b) s/Str
                :c s/Str
                (s/optional-key "d") s/Str}]

    (testing "without extra arguments makes all top-level keys required"
      (is (= (keys (st/with-required-keys schema))
             [:a :b :c (s/required-key "d")])))

    (testing "makes all given top-level keys are required, ignoring missing keys"
      (is (= (st/with-required-keys schema :NON-EXISTING) schema))

      (is (= (keys (st/with-required-keys schema :b [1 2 3] "d" :NON-EXISTING))
             [:a :b :c (s/required-key "d")])))))
