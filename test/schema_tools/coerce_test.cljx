(ns schema-tools.coerce-test
  (:require #+clj [clojure.test :refer [deftest testing is]]
            #+cljs [cljs.test :as test :refer-macros [deftest testing is]]
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
                             #+clj (Long/parseLong x)
                             #+cljs (js/Number.parseInt x 10)
                             x))))
        string->vec->long (stc/forwarding-matcher string->vec string->long)
        string->long->vec (stc/forwarding-matcher string->long string->vec)]

    (testing "string->vec->long is able to parse Long(s) and String(s) of Long(s)."
      (is (= ((sc/coercer {:a [s/Int]
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
                :f "1"})
             {:a [1 2 3]
              :b [1 2 3]
              :c [1 2 3]
              :d [[1 2 3] [4 5 6] [7 8 9]]
              :e 1
              :f 1})))

    (testing "string->long->vec is able to parse Long(s) and String(s) of Long(s)."
      (is (= ((sc/coercer {:a [s/Int]
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
                :f "1"})
             {:a [1 2 3]
              :b [1 2 3]
              :c [1 2 3]
              :d [[1 2 3] [4 5 6] [7 8 9]]
              :e 1
              :f 1})))))

(deftest or-matcher-test
  (let [base-matcher (fn [schema-pred value-pred value-fn]
                       (fn [schema]
                         (if (schema-pred schema)
                           (fn [x]
                             (if (value-pred x)
                               (value-fn x))))))
        m1 (base-matcher #(= s/Str %) string? #(string/upper-case %))
        m2 (base-matcher #(= s/Int %) number? inc)
        m3 (base-matcher #(= s/Bool %) (partial instance? s/Bool) not)
        m4 (base-matcher #(= s/Str %) string? #(string/lower-case %))]
    (testing "or-matcher selects first matcher where schema matches"
      (is (= ((sc/coercer {:band s/Str :number s/Int :lucid s/Bool}
                          (stc/or-matcher m1 m2 m3 m4))
               {:band "kiss", :number 41, :lucid false})
             {:band "KISS", :number 42, :lucid true})))))

(deftest coercer-test

  (testing "default case"

    (let [matcher {s/Str #(if (string? %) (string/upper-case %) %)}
          coercer (stc/coercer s/Str matcher)]

      (testing "successfull coercion retuns coerced value"
        (is (= (coercer "kikka") "KIKKA")))

      (testing "failed coercion throws ex-info"
        (try
          (coercer 123)
          (catch #+clj Exception #+cljs js/Error e
            (let [{:keys [schema type value]} (ex-data e)]
              (is (= type :schema-tools.coerce/error))
              (is (= schema s/Str))
              (is (= value 123))))))))

  (testing "custom type"
    (let [matcher {s/Str #(if (string? %) (string/upper-case %) %)}
          coercer (stc/coercer s/Str matcher ::horror)]

      (testing "successfull coercion retuns coerced value"
        (is (= (coercer "kikka") "KIKKA")))

      (testing "failed coercion throws ex-info"
        (try
          (coercer 123)
          (catch #+clj Exception #+cljs js/Error e
            (let [{:keys [schema type value]} (ex-data e)]
              (is (= type ::horror))
              (is (= schema s/Str))
              (is (= value 123)))))))))

(deftest coerce-test

  (testing "default case"

    (let [matcher {s/Str #(if (string? %) (string/upper-case %) %)}]

      (testing "successfull coerce retuns coerced value"
        (is (= (stc/coerce "kikka" s/Str matcher) "KIKKA")))

      (testing "failed coercion throws ex-info"
        (try
          (stc/coerce 123 s/Str matcher)
          (catch #+clj Exception #+cljs js/Error e
            (let [{:keys [schema type value]} (ex-data e)]
              (is (= type :schema-tools.coerce/error))
              (is (= schema s/Str))
              (is (= value 123))))))))

  (testing "custom type"
    (let [matcher {s/Str #(if (string? %) (string/upper-case %) %)}]

      (testing "successfull coercion retuns coerced value"
        (is (= (stc/coerce "kikka" s/Str matcher ::horror) "KIKKA")))

      (testing "failed coercion throws ex-info"
        (try
          (stc/coerce 123 s/Str matcher ::horror)
          (catch #+clj Exception #+cljs js/Error e
            (let [{:keys [schema type value]} (ex-data e)]
              (is (= type ::horror))
              (is (= schema s/Str))
              (is (= value 123)))))))))
