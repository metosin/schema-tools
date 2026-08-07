(ns schema-tools.openapi.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [schema-tools.openapi.core :as openapi]
            [schema.core :as s]
            [schema-tools.core :as st]
            #?@(:cljs [goog.date.UtcDateTime
                       goog.date.Date])))

(s/defschema Item
  {:field s/Str})

(s/defrecord Param [a :- s/Str])

;; FIXME: This is broken
#_
(deftest record-schema-test
  (testing "Test convert record to schema"
    (is (= (s/named {:a s/Str} "ParamRecord") (openapi/record-schema Param)))))

(deftest plain-map-test
  (testing "Test plain-map? for plain map"
    (is (openapi/plain-map? {:id s/Int :title s/Str})))

  (testing "Test plain-map? for record"
    (is (not (openapi/plain-map? Param)))))

(deftest remove-empty-keys-test
  (testing "Test remove empty keys"
    (is (= {:id s/Int :title s/Str}
           (openapi/remove-empty-keys {:id s/Int :title s/Str :extra nil})))))

(def expectations
  [[s/Bool
    {:type "boolean"}]

   [s/Num
    {:type "number" :format "double"}]

   [s/Int
    {:type "integer" :format "int32"}]

   [s/Str
    {:type "string"}]

   [s/Symbol
    {:type "string"}]

   [s/Keyword
    {:type "string"}]

   [s/Inst
    {:type "string" :format "date-time"}]

   [s/Uuid
    {:type "string" :format "uuid"}]

   [s/Regex
    {:type "string" :format "regex"}]

   [#"a[6-9]"
    {:type "string" :pattern "a[6-9]"}]

   [#{s/Keyword}
    {:type        "array"
     :items       {:type "string"}
     :uniqueItems true}]

   [(list s/Keyword)
    {:type  "array"
     :items {:type "string"}}]

   [[s/Keyword]
    {:type  "array"
     :items {:type "string"}}]

   [Item
    {:type                 "object"
     :title                "schema-tools.openapi.core-test/Item"
     :properties           {"field" {:type "string"}}
     :additionalProperties false
     :required             ["field"]}]

   [(st/schema {:field s/Str})
    {:type                 "object"
     :properties           {"field" {:type "string"}}
     :additionalProperties false
     :required             ["field"]}]

   [(st/schema {:field s/Str} {:name "OpenAPI"})
    {:type                 "object"
     :title                "OpenAPI"
     :properties           {"field" {:type "string"}}
     :additionalProperties false
     :required             ["field"]}]

   [(st/schema {:field s/Str} {:openapi {:type "string"
                                         :format "bytes"}})
    {:type                 "string"
     :format               "bytes"}]

   [(st/schema s/Str {:openapi/default "openapi"
                      :openapi/format  "email"
                      :swagger/default "swagger"})
    {:type    "string"
     :format  "email"
     :default "openapi"}]

   [(s/maybe s/Keyword)
    {:oneOf [{:type "string"}
             {:type "null"}]}]

   [(s/both s/Num (s/pred even? 'even?))
    {:allOf [{:type "number" :format "double"}
             {:type "number" :multipleOf 2}]}]

   [(s/named {} "Named")
    {:type                 "object"
     :title                "Named"
     :additionalProperties false}]

   [(s/pred neg? 'neg?)
    {:type "number"
     :maximum 0
     :exclusiveMaximum true}]

   [{:string               s/Str
     (s/required-key :req) s/Str
     (s/optional-key :opt) s/Str}
    {:type                 "object"
     :properties
     {"string" {:type "string"}
      "req"    {:type "string"}
      "opt"    {:type "string"}}
     :additionalProperties false
     :required             ["string" "req"]}]

   [{:string s/Str
     s/Int   s/Int}
    {:type                 "object"
     :properties           {"string" {:type "string"}}
     :additionalProperties {:type "integer" :format "int32"}
     :required             ["string"]}]

   ;; clj only
   #?(:clj
      [Param
       {:type                 "object"
        :title                "ParamRecord"
        :properties           {"a" {:type "string"}}
        :additionalProperties false
        :required             ["a"]}])

   #?(:clj
      [java.util.regex.Pattern
       {:type "string" :format "regex"}])

   #?(:clj
      [java.time.Instant
       {:type "string" :format "date-time"}])

   #?(:clj
      [java.time.LocalDate
       {:type "string" :format "date"}])

   #?(:clj
      [java.time.LocalTime
       {:type "string" :format "time"}])

   #?(:clj
      [java.io.File
       {:type "file"}])

   ;; cljs only
   #?(:cljs
      [js/Date
       {:type "string" :format "date-time"}])

   #?(:cljs
      [goog.date.Date
       {:type "string" :format "date"}])

   #?(:cljs
      [goog.date.UtcDateTime
       {:type "string" :format "date-time"}])
   ])

(deftest transform-test
  (doseq [[schema openapi-spec] expectations]
    (testing "transform"
      (is (= openapi-spec (openapi/transform schema nil)))))

  (testing "transform enum"
    (let [spec (openapi/transform (s/enum "s" "m" "l") nil)]
      (is (= "string" (:type spec)))
      (is (= (set ["s" "l" "m"]) (set (:enum spec)))))))
