(ns schema-tools.select-schema-test
  (:require #+clj [clojure.test :refer [deftest testing is]]
    #+cljs [cljs.test :as test :refer-macros [deftest testing is]]
            [schema-tools.core :as st]
            [schema.coerce :as sc]
            [schema.core :as s :include-macros true]
            [schema.utils :as su]))

(defn valid? [schema value]
  (nil? (s/check schema value)))

(defn invalid? [schema value]
  (not (valid? schema value)))

(deftest select-schema-test

  (testing "simple case"
    (is (= (st/select-schema "kikka" s/Str) "kikka")))

  (testing "strictly defined schema, with disallowed keys"
    (let [schema {:a s/Str
                  :b {(s/optional-key [1 2 3]) [{(s/required-key "d") s/Str}]}}
          value {:a "kikka"
                 :b {[1 2 3] [{"d" "kukka"
                               ":d" "kikka"
                               :d "kukka"}]}}]

      (testing "value does not match schema"
        (is (invalid? schema value)))

      (testing "select-schema drops disallowed keys making value match schema"
        (let [selected (st/select-schema value schema)]
          (is (valid? schema selected))
          (is (= selected {:a "kikka", :b {[1 2 3] [{"d" "kukka"}]}}))))))

  (testing "loosely defined schema, with disallowed keys"
    (let [schema {s/Keyword s/Str
                  :a {:b {s/Str s/Str}
                      :c {s/Any s/Str}}}
          value {:kikka "kukka"
                 :a {:b {"abba" "jabba"}
                     :c {[1 2 3] "kakka"}
                     :d :ILLEGAL-KEY}}]

      (testing "value does not match schema"
        (is (invalid? schema value)))

      (testing "select-schema drops disallowed keys making value match schema"
        (let [selected (st/select-schema value schema)]
          (is (valid? schema selected))
          (is (= selected {:kikka "kukka", :a {:b {"abba" "jabba"}, :c {[1 2 3] "kakka"}}}))))))

  (testing "other errors cause coercion exception"
    (is (thrown-with-msg?
          #+clj clojure.lang.ExceptionInfo #+cljs js/Error
          #"Could not coerce value to schema"
          (st/select-schema {:a 123} {:a s/Str}))))

  (testing "with coercion matcher"
    (let [schema {:name s/Str, :sex (s/enum :male :female)}
          value {:name "Linda", :age 66, :sex "female"}]

      (testing "select-schema fails on type mismatch"
        (is (thrown-with-msg?
              #+clj clojure.lang.ExceptionInfo #+cljs js/Error
              #"Could not coerce value to schema"
              (st/select-schema value schema))))

      (testing "select-schema with extra coercion matcher succeeds"
        (let [selected (st/select-schema value schema sc/json-coercion-matcher)]
          (is (valid? schema selected))
          (is (= selected {:name "Linda" :sex :female}))))))

  (testing "with predicate keys"
    (let [x- (s/pred #(re-find #"x-" (name %)) ":x-.*")
          schema {x- s/Any
                  :a s/Any}
          value {:x-abba "kikka"
                 :y-abba "kukka"
                 :a "kakka"}]

      (testing "value does not match schema"
        (is (invalid? schema value)))

      (testing "select-schema drops disallowed keys making value match schema"
        (let [selected (st/select-schema value schema)]
          (is (valid? schema selected))
          (is (= selected {:x-abba "kikka", :a "kakka"}))))))

  (testing "using with pre 0.5.0 argument order"
    (is (thrown-with-msg?
          #+clj clojure.lang.ExceptionInfo #+cljs js/Error
          #"Illegal argument order - breaking change in 0.5.0."
          (st/select-schema (s/enum :a :b) :b)))))
