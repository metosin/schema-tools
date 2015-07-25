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

(deftest select-schema!-test

  (testing "simple case"
    (is (= (st/select-schema! s/Str "kikka") "kikka")))

  (testing "strictly defined schema, with disallowed keys"
    (let [schema {:a s/Str
                  :b {(s/optional-key [1 2 3]) [{(s/required-key "d") s/Str}]}}
          value {:a "kikka"
                 :b {[1 2 3] [{"d" "kukka"
                               ":d" "kikka"
                               :d "kukka"}]}}]

      (testing "value does not match schema"
        (is (invalid? schema value)))

      (testing "select-schema! drops disallowed keys making value match schema"
        (let [selected (st/select-schema! schema value)]
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

      (testing "select-schema! drops disallowed keys making value match schema"
        (let [selected (st/select-schema! schema value)]
          (is (valid? schema selected))
          (is (= selected {:kikka "kukka", :a {:b {"abba" "jabba"}, :c {[1 2 3] "kakka"}}}))))))

  (testing "other errors cause schema.utils.ErrorContainer"
    (is (thrown? clojure.lang.ExceptionInfo (st/select-schema! {:a s/Str} {:a 123}))))

  (testing "with coercion matcher"
    (let [schema {:name s/Str, :sex (s/enum :male :female)}
          value {:name "Linda", :age 66, :sex "female"}]

      (testing "select-schema! fails on type mismatch"
        (is (thrown? clojure.lang.ExceptionInfo (st/select-schema! schema value))))

      (testing "select-schema! with extra coercion matcher succeeds"
        (let [selected (st/select-schema! sc/json-coercion-matcher schema value)]
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

      (testing "select-schema! drops disallowed keys making value match schema"
        (let [selected (st/select-schema! schema value)]
          (is (valid? schema selected))
          (is (= selected {:x-abba "kikka", :a "kakka"})))))))
