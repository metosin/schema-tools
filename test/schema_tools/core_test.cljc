(ns schema-tools.core-test
  (:require #?(:clj  [clojure.test :refer [deftest testing is]]
               :cljs [cljs.test :as test :refer-macros [deftest testing is]])
            [schema-tools.core :as st]
            [schema.core :as s :include-macros true]))

(s/defschema Kikka {:a s/Str :b s/Str})

(deftest any-keys-test
  (is (= {s/Any s/Any} (st/any-keys)))
  (testing "allows any keys"
    (is (= nil (s/check (st/any-keys) {"a" true, [1 2 3] true})))))

(deftest any-keyword-keys-test
  (is (= {s/Keyword s/Any} (st/any-keyword-keys)))
  (is (= {s/Keyword s/Str} (st/any-keyword-keys {s/Keyword s/Str})))
  (is (= {:a s/Str, s/Keyword s/Any} (st/any-keyword-keys {:a s/Str})))
  (testing "does not allow non-keyword-keys"
    (is (s/check (st/any-keyword-keys) {:a true, "b" true})))
  (testing "allows any keyword-keys"
    (is (= nil (s/check (st/any-keyword-keys) {:a true, :b true}))))
  (testing "can be used to extend schemas"
    (is (= nil (s/check (st/any-keyword-keys {(s/required-key "b") s/Bool}) {:a true, "b" true})))))

(deftest assoc-test
  (let [schema {:a s/Str
                (s/optional-key :b) s/Str
                (s/required-key "c") s/Str
                s/Keyword s/Str}]
    #?(:clj (testing "odd number of arguments"
              (is (thrown? IllegalArgumentException
                           (st/assoc schema :b s/Int :c)))))
    (testing "happy case"
      (is (= {(s/optional-key :a) s/Int
              :b s/Int
              "c" s/Int
              (s/optional-key :d) s/Int
              s/Keyword s/Int}
             (st/assoc schema
                       (s/optional-key :a) s/Int
                       :b s/Int
                       "c" s/Int
                       (s/optional-key :d) s/Int
                       s/Keyword s/Int))))
    (testing "make anonymous if value changed"
      (is (not (nil? (meta (st/assoc Kikka :a s/Str)))))
      (is (nil? (meta (st/assoc Kikka :c s/Str)))))))

(deftest dissoc-test
  (let [schema {:a s/Str
                (s/optional-key :b) s/Str
                (s/required-key "c") s/Str
                s/Keyword s/Str}]
    (testing "dissoc"
      (is (= {s/Keyword s/Str} (st/dissoc schema :a :b "c" :d))))
    (testing "make anonymous if value changed"
      (is (not (nil? (meta (st/dissoc Kikka :d)))))
      (is (nil? (meta (st/dissoc Kikka :a)))))))

(deftest select-keys-test
  (let [schema {:a s/Str
                (s/optional-key :b) s/Str
                (s/required-key "c") s/Str
                s/Keyword s/Str}]
    (testing "select-keys"
      (is (= {:a s/Str
              (s/optional-key :b) s/Str
              (s/required-key "c") s/Str}
             (st/select-keys schema [:a :b "c" :d]))))
    (testing "make anonymous if value changed"
      (is (not (nil? (meta (st/select-keys Kikka [:a :b])))))
      (is (nil? (meta (st/select-keys Kikka [:a])))))))

(deftest get-in-test
  (let [schema {:a {(s/optional-key :b) {(s/required-key :c) s/Str}}
                (s/optional-key "d") {s/Keyword s/Str}}]
    (is (= s/Str (st/get-in schema [:a (s/optional-key :b) (s/required-key :c)])))
    (is (= s/Str (st/get-in schema [:a :b :c])))
    (is (= s/Str (st/get-in schema ["d" s/Keyword])))
    (is (= nil (st/get-in schema [:e])))
    (testing "works with defaults"
      (is (= s/Str (st/get-in schema [:e] s/Str)))
      (is (= {:a s/Str} (st/get-in schema [:e :a] {:a s/Str})))))
  (let [uniform-schema      [s/Str]
        schema-with-singles [(s/one s/Str "0") (s/optional s/Int "1") s/Keyword]
        bounded-schema      [(s/one s/Int "0") (s/optional s/Int "1")]
        complex-schema {:a [(s/one {:b s/Int} "0") [s/Str]]}]
    (testing "works with sequences"
      (is (= s/Str (st/get-in uniform-schema [0])))
      (is (= s/Str (st/get-in uniform-schema [1000])))
      (is (= s/Str (st/get-in schema-with-singles [0])))
      (is (= s/Int (st/get-in schema-with-singles [1])))
      (is (= s/Keyword (st/get-in schema-with-singles [2])))
      (is (= s/Keyword (st/get-in schema-with-singles [1000])))
      (is (= s/Int (st/get-in bounded-schema [1])))
      (is (= nil (st/get-in bounded-schema [2])))
      (is (= s/Str (st/get-in complex-schema [:a 1 1000])))
      (is (= s/Str (st/get-in complex-schema [:a 1000 1])))
      (is (= s/Int (st/get-in complex-schema [:a 0 :b]))))))

(deftest assoc-in-test
  (let [schema {:a {(s/optional-key [1 2 3]) {(s/required-key "d") {}}}}]
    (testing "assoc-in"
      (is (= {:a {(s/optional-key [1 2 3]) {(s/required-key "d") {:e {:f s/Str}}}}}
             (st/assoc-in schema [:a [1 2 3] "d" :e :f] s/Str))))
    (testing "make anonymous if value changed"
      (is (not (nil? (meta (st/assoc-in Kikka [:a] s/Str)))))
      (is (nil? (meta (st/assoc-in Kikka [:c :d] s/Str)))))))

(deftest update-in-test
  (let [schema {:a {(s/optional-key [1 2 3]) {(s/required-key "d") s/Str}}}]
    (testing "update-in"
      (is (= {:a {(s/optional-key [1 2 3]) {(s/required-key "d") s/Int}}}
             (st/update-in schema [:a [1 2 3] "d"] (constantly s/Int)))))
    (testing "make anonymous if value changed"
      (is (not (nil? (meta (st/update-in Kikka [:a] (constantly s/Str))))))
      (is (nil? (meta (st/update-in Kikka [:c :d] (constantly s/Str))))))))

(deftest dissoc-in-test
  (let [schema {:a {(s/optional-key [1 2 3]) {(s/required-key "d") s/Str
                                              :kikka s/Str}}}]
    (testing "dissoc-in"
      (is (= {:a {(s/optional-key [1 2 3]) {:kikka s/Str}}}
             (st/dissoc-in schema [:a [1 2 3] "d"]))))
    (testing "resulting empty maps are removed"
      (is (= {} (st/dissoc-in schema [:a [1 2 3]]))))
    (testing "make anonymous if value changed"
      (let [schema (s/schema-with-name {:a {:b {:c s/Str}}} 'Kikka)]
        (is (not (nil? (meta (st/dissoc-in schema [:a :b :d])))))
        (is (nil? (meta (st/dissoc-in schema [:a :b :c]))))))))

(deftest update-test
  (testing "update"
    (is (= {:a 2} (st/update {:a 1} :a inc)))
    (is (= {(s/optional-key :a) 2} (st/update {(s/optional-key :a) 1} :a inc)))
    (is (= {(s/required-key :a) 2} (st/update {(s/required-key :a) 1} :a inc))))
  (testing "make anonymous if value changed"
    (is (not (nil? (meta (st/update Kikka :a (constantly s/Str))))))
    (is (nil? (meta (st/update Kikka :c (constantly s/Str)))))))

(deftest merge-test
  (testing "is merged left to right"
    (is (= {(s/optional-key :a) s/Num
            (s/optional-key :b) s/Num
            (s/required-key :c) s/Str}
           (st/merge {:a s/Str
                      (s/optional-key :b) s/Str
                      (s/required-key :c) s/Str}
                     {(s/optional-key :a) s/Num
                      (s/optional-key :b) s/Num}))))
  (testing "nills"
    (is (= nil (st/merge nil nil)))
    (is (= {:a s/Str} (st/merge {:a s/Str} nil)))
    (is (= {:a s/Str} (st/merge nil {:a s/Str}))))
  (testing "non-maps can't be mapped"
    (is (thrown? #?(:clj AssertionError :cljs js/Error) (st/merge [s/Str] [s/Num]))))
  (testing "make anonymous if value changed"
    (is (nil? (meta (st/merge {:b s/Str} Kikka))))
    (is (not (nil? (meta (st/merge Kikka {:b s/Str})))))
    (is (nil? (meta (st/merge Kikka {:c s/Str}))))))

(deftest optional-keys-test
  (let [schema {(s/optional-key :a) s/Str
                (s/required-key :b) s/Str
                :c s/Str
                (s/required-key "d") s/Str}]
    (testing "without extra arguments makes all top-level keys optional"
      (is (= (keys (st/optional-keys schema))
             [(s/optional-key :a) (s/optional-key :b) (s/optional-key :c) (s/optional-key "d")])))
    (testing "invalid input"
      (is (thrown-with-msg? #?(:clj AssertionError :cljs js/Error)
                            #"input should be nil or a vector of keys."
                            (st/optional-keys schema :ANY))))
    (testing "makes all given top-level keys are optional, ignoring missing keys"
      (is (= schema (st/optional-keys schema [:NON-EXISTING])))
      (is (= [(s/optional-key :a) (s/optional-key :b) :c (s/optional-key "d")]
             (keys (st/optional-keys schema [:a :b "d" :NON-EXISTING])))))
    (testing "make anonymous if value changed"
      (let [schema (s/schema-with-name {(s/optional-key :a) s/Str, :b s/Str} 'Kikka)]
        (is (not (nil? (meta (st/optional-keys schema [])))))
        (is (nil? (meta (st/optional-keys schema [:b]))))))))

(deftest required-keys-test
  (let [schema {(s/required-key :a) s/Str
                (s/optional-key :b) s/Str
                :c s/Str
                (s/optional-key "d") s/Str}]
    (testing "without extra arguments makes all top-level keys required"
      (is (= [:a :b :c (s/required-key "d")]
             (keys (st/required-keys schema)))))
    (testing "invalid input"
      (is (thrown-with-msg? #?(:clj AssertionError :cljs js/Error)
                            #"input should be nil or a vector of keys."
                            (st/required-keys schema :ANY))))
    (testing "makes all given top-level keys are required, ignoring missing keys"
      (is (= schema (st/required-keys schema [:NON-EXISTING])))
      (is (= [:a :b :c (s/required-key "d")]
             (keys (st/required-keys schema [:b [1 2 3] "d" :NON-EXISTING])))))
    (testing "make anonymous if value changed"
      (let [schema (s/schema-with-name {(s/optional-key :a) s/Str, :b s/Str} 'Kikka)]
        (is (not (nil? (meta (st/required-keys schema [])))))
        (is (nil? (meta (st/required-keys schema [:a]))))))))

(deftest schema-description
  (testing "schema-with-description"
    (is (= {:description "It's a ping"} (meta (st/schema-with-description {:ping s/Str} "It's a ping")))))
  (testing "schema-description"
    (is (= "It's a ping" (st/schema-description (st/schema-with-description {:ping s/Str} "It's a ping"))))))

(s/defschema Omena
  "Omena is an apple"
  {:color (s/enum :green :red)})

#?(:clj
   (deftest resolve-schema-test
     (testing "defined schema can be resolved"
       (is (= #'Omena (st/resolve-schema Omena))))
     (testing "just named schema can't be resolved"
       (is (= nil (st/resolve-schema (s/schema-with-name {:ping s/Str} "Ping")))))))

#?(:clj
   (deftest resolve-schema-description-test
     (testing "schema with description"
       (is (= "Banaani" (st/resolve-schema-description (st/schema-with-description Omena "Banaani")))))
     (testing "schema with docstring"
       (is (= "Omena is an apple" (st/resolve-schema-description Omena))))
     (testing "schema without docstring"
       (is (= nil (st/resolve-schema-description Kikka))))
     (testing "anonymous schema"
       (is (= nil (st/resolve-schema-description {:ping s/Str}))))))
