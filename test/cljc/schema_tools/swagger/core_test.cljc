(ns schema-tools.swagger.core-test
  (:require
    [clojure.test :refer [deftest testing is are]]
    [schema-tools.swagger.core :as swagger]
    [schema-tools.core :as st]
    [schema.core :as s]
    #?@(:cljs [goog.date.UtcDateTime
               goog.date.Date])))

(s/defschema Abba
  {:string s/Str})

(s/defrecord Kikka [a :- s/Str])

(def exceptations
  [[s/Bool {:type "boolean"}]
   [s/Num {:type "number", :format "double"}]
   [s/Int {:type "integer", :format "int32"}]
   [s/Str {:type "string"}]
   [s/Symbol {:type "string"}]
   ;; TODO: phantom generates invalid names
   #?(:clj
      [Kikka {:type "object"
              :title "KikkaRecord"
              :properties {"a" {:type "string"}}
              :additionalProperties false
              :required ["a"]}])
   [s/Keyword {:type "string"}]
   [s/Inst {:type "string", :format "date-time"}]
   [s/Uuid {:type "string", :format "uuid"}]
   #?(:clj [java.util.regex.Pattern {:type "string", :format "regex"}])
   [s/Regex {:type "string", :format "regex"}]

   [#"a[6-9]" {:type "string", :pattern "a[6-9]"}]
   [#{s/Keyword} {:type "array"
                  :items {:type "string"}
                  :uniqueItems true}]
   [(list s/Keyword) {:type "array"
                      :items {:type "string"}}]
   [[s/Keyword] {:type "array"
                 :items {:type "string"}}]
   [Abba {:type "object"
          :title "schema-tools.swagger.core-test/Abba"
          :properties {"string" {:type "string"}}
          :additionalProperties false
          :required ["string"]}]
   [(st/schema {:string s/Str}) {:type "object"
                                 :properties {"string" {:type "string"}}
                                 :additionalProperties false
                                 :required ["string"]}]
   [(st/schema {:string s/Str} {:name "Schema2"}) {:type "object"
                                                   :title "Schema2"
                                                   :properties {"string" {:type "string"}}
                                                   :additionalProperties false
                                                   :required ["string"]}]
   [(st/schema s/Str {:swagger/default "abba"
                      :swagger/format "email"}) {:type "string"
                                                 :format "email"
                                                 :default "abba"}]
   [(st/schema {:field s/Str} {:swagger {:type "file"}}) {:type "file"}]
   [(s/maybe s/Keyword) {:type "string", :x-nullable true}]
   [(s/enum "s" "m" "l") {:type "string", :enum #{"s" "l" "m"}}]
   [(s/both s/Num (s/pred odd? 'odd?)) {:type "number", :format "double"}]
   [(s/named {} "Named") {:type "object"
                          :title "Named"
                          :additionalProperties false}]
   [(s/pred integer? 'integer?) {:type "integer", :format "int32"}]
   [{:string s/Str
     (s/required-key :req) s/Str
     (s/optional-key :opt) s/Str} {:type "object"
                                   :properties {"string" {:type "string"}
                                                "req" {:type "string"}
                                                "opt" {:type "string"}}
                                   :additionalProperties false
                                   :required ["string" "req"]}]
   [{:string s/Str
     s/Int s/Int} {:type "object"
                   :properties {"string" {:type "string"}}
                   :additionalProperties {:type "integer", :format "int32"}
                   :required ["string"]}]

   ;; clj only
   #?(:clj [java.time.Instant {:type "string", :format "date-time"}])
   #?(:clj [java.time.LocalDate {:type "string", :format "date"}])
   #?(:clj [java.time.LocalTime {:type "string", :format "time"}])
   #?(:clj [java.io.File {:type "file"}])

   ;; cljs only
   #?(:cljs [js/Date {:type "string", :format "date-time"}])
   #?(:cljs [goog.date.Date {:type "string", :format "date"}])
   #?(:cljs [goog.date.UtcDateTime {:type "string", :format "date-time"}])])

(deftest test-expectations
  (doseq [[schema swagger] exceptations]
    (is (= swagger (swagger/transform schema nil)))))

(def Id s/Str)
(def Name s/Str)
(def Street s/Str)
(s/defschema City (s/maybe (s/enum :tre :hki)))
(s/defschema Address {:street Street
                      :city City})
(s/defschema User {:id Id
                   :name Name
                   :address Address})

(deftest maybe-pred-test
  (is (true? (swagger/maybe? City))))

(deftest expand-test

  (testing "non-registered are not affected"
    (is (= {::kikka "kukka"}
           (swagger/swagger-spec
             {::kikka "kukka"}))))

  (testing "::parameters"
    #_(println "E:" (pr-str (swagger/transform (s/enum :a :b) nil)))
    (is (= {:parameters [{:in "query"
                          :name "name2"
                          :description "this survives the merge"
                          :type "string"
                          :required true}
                         {:in "query"
                          :name "name"
                          :description ""
                          :type "string"
                          :required false}
                         {:in "query"
                          :name "street"
                          :description ""
                          :type "string"
                          :required false}
                         {:in "query"
                          :name "city"
                          :description ""
                          :type "string"
                          :required false
                          :enum #{:tre :hki}
                          :allowEmptyValue true}
                         {:in "path"
                          :name "id"
                          :description ""
                          :type "string"
                          :required true}
                         {:in "body",
                          :name "schema-tools.swagger.core-test/Address",
                          :description "",
                          :required true,
                          :schema {:type "object",
                                   :title "schema-tools.swagger.core-test/Address",
                                   :properties {"street" {:type "string"},
                                                "city" {:enum #{:tre :hki},
                                                        :type "string"
                                                        :x-nullable true}},
                                   :additionalProperties false,
                                   :required ["street" "city"]}}]}
           (swagger/swagger-spec
             {:parameters [{:in "query"
                            :name "name"
                            :description "this will be overridden"
                            :required false}
                           {:in "query"
                            :name "name2"
                            :description "this survives the merge"
                            :type "string"
                            :required true}]
              ::swagger/parameters {:query {(s/optional-key :name) Name
                                            (s/optional-key :street) Street
                                            (s/optional-key :city) City}
                                    :path {:id Id}
                                    :body Address}}))))

  (testing "::responses"
    (is (= {:responses
            {200 {:schema {:type "object"
                           :title "schema-tools.swagger.core-test/User"
                           :properties {"id" {:type "string"}
                                        "name" {:type "string"}
                                        "address" {:type "object"
                                                   :title "schema-tools.swagger.core-test/Address"
                                                   :properties {"street" {:type "string"}
                                                                "city" {:enum #{:tre :hki}
                                                                        :type "string"
                                                                        :x-nullable true}}
                                                   :additionalProperties false
                                                   :required ["street" "city"]}}
                           :additionalProperties false
                           :required ["id" "name" "address"]}
                  :description ""}
             404 {:description "Ohnoes."}
             500 {:description "fail"}}}
           (swagger/swagger-spec
             {:responses {404 {:description "fail"}
                          500 {:description "fail"}}
              ::swagger/responses {200 {:schema User}
                                   404 {:description "Ohnoes."}}})))))
