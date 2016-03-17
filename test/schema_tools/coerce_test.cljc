(ns schema-tools.coerce-test
  (:require #?(:clj  [clojure.test :refer [deftest testing is]]
               :cljs [cljs.test :as test :refer-macros [deftest testing is]])
            [clojure.string :as string]
            [schema.core :as s]
            [schema.coerce :as sc]
            [clojure.string :as str]
            [schema-tools.coerce :as stc]))

(deftest forwarding-matcher-test
  (let [string->vec (fn [schema]
                      (if (vector? schema)
                        (fn [x]
                          (if (string? x)
                            (str/split x #",")
                            x))))
        string->long (fn [schema]
                       (if (= s/Int schema)
                         (fn [x]
                           (if (string? x)
                             #?(:clj  (Long/parseLong x)
                                :cljs (js/parseInt x 10))
                             x))))
        string->vec->long (stc/forwarding-matcher string->vec string->long)
        string->long->vec (stc/forwarding-matcher string->long string->vec)]

    (testing "string->vec->long is able to parse Long(s) and String(s) of Long(s)."
      (is (= {:a [1 2 3]
              :b [1 2 3]
              :c [1 2 3]
              :d [[1 2 3] [4 5 6] [7 8 9]]
              :e 1
              :f 1}
             ((sc/coercer {:a [s/Int]
                           :b [s/Int]
                           :c [s/Int]
                           :d [[s/Int]]
                           :e s/Int
                           :f s/Int}
                          string->vec->long)
              {:a [1 2 3]
               :b "1,2,3"
               :c ["1" "2" "3"]
               :d ["1,2,3" "4,5,6" "7,8,9"]
               :e 1
               :f "1"}))))

    (testing "string->long->vec is able to parse Long(s) and String(s) of Long(s)."
      (is (= {:a [1 2 3]
              :b [1 2 3]
              :c [1 2 3]
              :d [[1 2 3] [4 5 6] [7 8 9]]
              :e 1
              :f 1}
             ((sc/coercer {:a [s/Int]
                           :b [s/Int]
                           :c [s/Int]
                           :d [[s/Int]]
                           :e s/Int
                           :f s/Int}
                          string->long->vec)
              {:a [1 2 3]
               :b "1,2,3"
               :c ["1" "2" "3"]
               :d ["1,2,3" "4,5,6" "7,8,9"]
               :e 1
               :f "1"}))))))

(defn boolean? [x]
  (or (true? x) (false? x)))

(deftest or-matcher-test
  (let [base-matcher (fn [schema-pred value-pred value-fn]
                       (fn [schema]
                         (if (schema-pred schema)
                           (fn [x]
                             (if (value-pred x)
                               (value-fn x))))))
        m1 (base-matcher #(= s/Str %) string? #(string/upper-case %))
        m2 (base-matcher #(= s/Int %) number? inc)
        m3 (base-matcher #(= s/Bool %) boolean? not)
        m4 (base-matcher #(= s/Str %) string? #(string/lower-case %))]
    (testing "or-matcher selects first matcher where schema matches"
      (is (= {:band "KISS", :number 42, :lucid true}
             ((sc/coercer {:band s/Str :number s/Int :lucid s/Bool}
                          (stc/or-matcher m1 m2 m3 m4))
              {:band "kiss", :number 41, :lucid false}))))))

(deftest coercer-test

  (testing "default case"

    (let [matcher {s/Str #(if (string? %) (string/upper-case %) %)}
          coercer (stc/coercer s/Str matcher)]

      (testing "successfull coercion retuns coerced value"
        (is (= "KIKKA" (coercer "kikka"))))

      (testing "failed coercion throws ex-info"
        (try
          (coercer 123)
          (catch #?(:clj Exception :cljs js/Error) e
            (let [{:keys [schema type value]} (ex-data e)]
              (is (= :schema-tools.coerce/error type))
              (is (= s/Str schema))
              (is (= 123 value))))))))

  (testing "custom type"
    (let [matcher {s/Str #(if (string? %) (string/upper-case %) %)}
          coercer (stc/coercer s/Str matcher ::horror)]

      (testing "successfull coercion retuns coerced value"
        (is (= "KIKKA" (coercer "kikka"))))

      (testing "failed coercion throws ex-info"
        (try
          (coercer 123)
          (catch #?(:clj Exception :cljs js/Error) e
            (let [{:keys [schema type value]} (ex-data e)]
              (is (= ::horror type))
              (is (= s/Str schema))
              (is (= 123 value)))))))))

(deftest multi-matcher-test
  (let [schema {:a s/Int, :b s/Int}
        matcher (stc/multi-matcher (partial = s/Int) integer? [(partial * 2) dec])]
    (is (= {:a 3 :b 19} ((sc/coercer! schema matcher) {:a 2 :b 10})))))
