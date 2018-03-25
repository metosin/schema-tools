(ns schema-tools.swagger.core-test
  (:require
    [clojure.test :refer [deftest testing is are]]
    [schema-tools.swagger.core :as swagger]
    [schema-tools.core :as st]
    [schema.core :as s]))

(s/defschema Abba
  {:string s/Str})

(s/defrecord Kikka [a :- String])

(def exceptations
  [[s/Bool {:type "boolean"}]
   [s/Num {:type "number", :format "double"}]
   [s/Int {:type "integer", :format "int32"}]
   [s/Str {:type "string"}]
   [Kikka {:type "object"
           :title "KikkaRecord"
           :properties {:a {:type "string"}}
           :additionalProperties false
           :required [:a]}]
   [s/Keyword {:type "string"}]
   [s/Inst {:type "string", :format "date-time"}]
   [s/Uuid {:type "string", :format "uuid"}]
   [#"a[6-9]" {:type "string", :pattern "a[6-9]"}]
   [java.time.Instant {:type "string", :format "date-time"}]
   [java.time.LocalDate {:type "string", :format "date"}]
   [java.time.LocalTime {:type "string", :format "time"}]
   [java.util.regex.Pattern {:type "string", :format "regex"}]
   [java.io.File {:type "file"}]
   [#{s/Keyword} {:type "array"
                  :items {:type "string"}
                  :uniqueItems true}]
   [(list s/Keyword) {:type "array"
                      :items {:type "string"}}]
   [[s/Keyword] {:type "array"
                 :items {:type "string"}}]
   [Abba {:type "object"
          :title "Abba"
          :properties {:string {:type "string"}}
          :additionalProperties false
          :required [:string]}]
   [(st/schema {:string s/Str}) {:type "object"
                                 :properties {:string {:type "string"}}
                                 :additionalProperties false
                                 :required [:string]}]
   [(st/schema {:string s/Str} {:name "Schema2"}) {:type "object"
                                                   :title "Schema2"
                                                   :properties {:string {:type "string"}}
                                                   :additionalProperties false
                                                   :required [:string]}]
   [(s/maybe s/Keyword) {:type "string", :x-nullable true}]
   [(s/enum "s" "m" "l") {:type "string", :enum ["s" "l" "m"]}]
   [(s/both s/Num (s/pred odd? 'odd?)) {:type "number", :format "double"}]
   [(s/named {} "Named") {:type "object"
                          :title "Named"
                          :additionalProperties false}]
   [(s/pred integer? 'integer?) {:type "integer", :format "int32"}]
   [{:string s/Str
     (s/required-key :req) s/Str
     (s/optional-key :opt) s/Str} {:type "object"
                                   :properties {:string {:type "string"}
                                                :req {:type "string"}
                                                :opt {:type "string"}}
                                   :additionalProperties false
                                   :required [:string :req]}]
   [{:string s/Str
     s/Int s/Int} {:type "object"
                   :properties {:string {:type "string"}}
                   :additionalProperties {:type "integer", :format "int32"}
                   :required [:string]}]])

(deftest test-expectations
  (doseq [[schema swagger] exceptations]
    (is (= swagger (swagger/transform schema nil)))))
