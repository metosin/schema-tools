(ns schema-tools.coerce-test
  (:require
    #?@(:clj  [[clojure.test :refer [deftest testing is are]]]
        :cljs [[cljs.test :as test :refer-macros [deftest testing is are]]
               [cljs.reader]
               [goog.date.UtcDateTime]])
    [clojure.string :as string]
    [schema.core :as s]
    [schema.coerce :as sc]
    [clojure.string :as str]
    [schema-tools.coerce :as stc]
    [schema.utils :as su])
  #?(:clj
     (:import [java.util Date UUID]
              [java.util.regex Pattern]
              [java.time LocalDate LocalTime Instant]
              (clojure.lang Keyword))))

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

(deftest or-matcher-test
  (let [boolean? #(or (true? %) (false? %))
        base-matcher (fn [schema-pred value-pred value-fn]
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

  (testing "1-arity just for validating"
    (is (= "kikka" ((stc/coercer s/Str) "kikka"))))

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

(def shared-coercion-expectations
  {"s/Keyword" [s/Keyword :kikka :kikka
                s/Keyword ::kikka ::kikka
                s/Keyword "kikka" :kikka
                s/Keyword "kikka/kikka" :kikka/kikka
                s/Keyword 'kikka ::fails]
   #?@(:clj ["Keyword" [Keyword :kikka :kikka
                        Keyword ::kikka ::kikka
                        Keyword "kikka" :kikka
                        Keyword "kikka/kikka" :kikka/kikka
                        Keyword 'kikka ::fails]])

   "s/Uuid" [s/Uuid "5f60751d-9bf7-4344-97ee-48643c9949ce" (stc/string->uuid "5f60751d-9bf7-4344-97ee-48643c9949ce")
             s/Uuid (keyword "5f60751d-9bf7-4344-97ee-48643c9949ce") (stc/string->uuid "5f60751d-9bf7-4344-97ee-48643c9949ce")
             s/Uuid #uuid "5f60751d-9bf7-4344-97ee-48643c9949ce" (stc/string->uuid "5f60751d-9bf7-4344-97ee-48643c9949ce")
             s/Uuid "INVALID" ::fails]

   "s/Bool" [s/Bool :true true
             s/Bool :false false
             s/Bool :invalid ::fails]

   "s/Str" [s/Str :text "text"
            s/Str :retain.ns/please "retain.ns/please"]

   "s/Int" [s/Int 1 1
            s/Int :1 1
            s/Int :1.0 1
            s/Int 92233720368547758071 92233720368547758071
            s/Int -92233720368547758071 -92233720368547758071
            s/Int "1" ::fails]

   #?@(:clj ["Long" [Long 1 1
                     Long :1 1
                     Long 9223372036854775807 9223372036854775807
                     Long -9223372036854775807 -9223372036854775807
                     Long "1" ::fails]])

   #?@(:clj ["Double" [Double 1 1.0
                       Double 1.1 1.1
                       Double :1 1.0
                       Double 1.7976931348623157E308 1.7976931348623157E308
                       Double -1.7976931348623157E308 -1.7976931348623157E308
                       Double "1.0" ::fails]])

   #?@(:clj ["Date" [Date "2014-02-18T18:25:37.456Z" (stc/string->date "2014-02-18T18:25:37.456Z")
                     Date (keyword "2014-02-18T18:25:37Z") (stc/string->date "2014-02-18T18:25:37Z")
                     Date "2014-02-18T18:25:37Z" (stc/string->date "2014-02-18T18:25:37Z")
                     Date "2014-02-18T18" ::fails
                     Date "INVALID" ::fails]])

   #?@(:cljs ["js/Date" [js/Date "2014-02-18T18:25:37.456Z" (stc/string->date "2014-02-18T18:25:37.456Z")
                         js/Date "2014-02-18T18:25:37Z" (stc/string->date "2014-02-18T18:25:37Z")
                         js/Date (keyword "2014-02-18T18:25:37Z") (stc/string->date "2014-02-18T18:25:37Z")
                         ;; TODO: this works differently in clj!
                         js/Date "2014-02-18T18" (stc/string->date "2014-02-18T18")
                         js/Date "INVALID" ::fails]])

   #?@(:clj ["LocalDate" [LocalDate "2014-02-19" (LocalDate/parse "2014-02-19")
                          LocalDate (keyword "2014-02-19") (LocalDate/parse "2014-02-19")
                          LocalDate "INVALID" ::fails]])

   #?@(:clj ["LocalTime" [LocalTime "10:23" (LocalTime/parse "10:23")
                          LocalTime (keyword "10:23:37") (LocalTime/parse "10:23:37")
                          LocalTime "10:23:37" (LocalTime/parse "10:23:37")
                          LocalTime "10:23:37.456" (LocalTime/parse "10:23:37.456")
                          LocalTime "INVALID" ::fails]])

   #?@(:clj ["Instant" [Instant "2014-02-18T18:25:37.456Z" (Instant/parse "2014-02-18T18:25:37.456Z")
                        Instant "2014-02-18T18:25:37Z" (Instant/parse "2014-02-18T18:25:37Z")
                        Instant (keyword "2014-02-18T18:25:37Z") (Instant/parse "2014-02-18T18:25:37Z")
                        Instant "2014-02-18T18:25" ::fails
                        Instant "INVALID" ::fails]])})

(def json-coercion-expectations
  {"s/Int" [s/Int 1 1
            s/Int 92233720368547758071 92233720368547758071
            s/Int -92233720368547758071 -92233720368547758071
            s/Int "1" ::fails]

   #?@(:clj ["Long" [Long 1 1
                     Long 9223372036854775807 9223372036854775807
                     Long -9223372036854775807 -9223372036854775807
                     Long "1" ::fails]])

   #?@(:clj ["Double" [Double 1 1.0
                       Double 1.1 1.1
                       Double 1.7976931348623157E308 1.7976931348623157E308
                       Double -1.7976931348623157E308 -1.7976931348623157E308
                       Double "1.0" ::fails]])})

(def string-coercion-expectations
  {#?@(:clj ["Long" [Long 1 1
                     Long 9223372036854775807 9223372036854775807
                     Long -9223372036854775807 -9223372036854775807
                     Long "1" 1
                     Long "1.0" ::fails]])

   #?@(:clj ["Double" [Double 1 1.0
                       Double 1.1 1.1
                       Double 1.7976931348623157E308 1.7976931348623157E308
                       Double -1.7976931348623157E308 -1.7976931348623157E308
                       Double "1" 1.0
                       Double "1.0" 1.0]])

   "s/Int" [s/Int 1 1
            s/Int 92233720368547758071 92233720368547758071
            s/Int -92233720368547758071 -92233720368547758071
            s/Int "1" 1
            s/Int "1.0" 1
            s/Int "1.1" ::fails]

   "s/Num" [s/Num 1 1
            s/Num 1.0 1.0
            s/Num "1" 1
            s/Num "1.0" 1.0
            s/Num "-1.0" -1.0
            s/Num "+1.0" 1.0
            s/Num "1.0e10" 1.0e10
            s/Num "invalid" ::fails]

   "s/Bool" [s/Bool true true
             s/Bool "true" true
             s/Bool "false" false
             s/Bool "invalid" ::fails]})

(deftest json-matcher-test
  (doseq [[name ess] (merge shared-coercion-expectations json-coercion-expectations)
          :let [es (partition 3 ess)]
          [schema value expected] es]
    (testing name
      (let [result ((sc/coercer schema stc/json-coercion-matcher) value)]
        (if (= ::fails expected)
          (is (= true (boolean (su/error-val result))))
          (is (= expected result))))))

  #?(:clj
     (testing "Pattern"
       (is (instance? Pattern ((stc/coercer Pattern stc/json-coercion-matcher) ".*")))
       (is (instance? Pattern ((stc/coercer Pattern stc/json-coercion-matcher) (keyword ".*")))))))

(deftest string-matcher-test
  (doseq [[name ess] (merge shared-coercion-expectations string-coercion-expectations)
          :let [es (partition 3 ess)]
          [schema value expected] es]
    (testing name
      (let [result ((sc/coercer schema stc/string-coercion-matcher) value)]
        (if (= ::fails expected)
          (is (= true (boolean (su/error-val result))))
          (is (= expected result))))))

  #?(:clj
     (testing "Pattern"
       (is (instance? Pattern ((stc/coercer Pattern stc/json-coercion-matcher) ".*")))
       (is (instance? Pattern ((stc/coercer Pattern stc/json-coercion-matcher) (keyword ".*")))))))
