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

(def Id s/Int)
(def Name s/Str)
(def Street s/Str)
(s/defschema City (st/schema (s/maybe (s/enum :tre :hki))
                             {:openapi/description "a city"}))
(s/defschema Filters [s/Str])
(s/defschema Address
  {:street Street
   :city   City})
(s/defschema User
  {:id      Id
   :name    Name
   :address Address})
(def Token s/Str)

(deftest expand-test
  (testing "::parameters"
    (is (= {:parameters
            [{:name        "username"
              :in          "path"
              :description "username to fetch"
              :required    true
              :schema      {:type "string"}
              :style       "simple"}
             {:name        "id"
              :in          "path"
              :description ""
              :required    true
              :schema      {:type   "integer"
                            :format "int32"}}
             {:name        "name"
              :in          "query"
              :description ""
              :required    true
              :schema      {:type "string"}}
             {:name        "city"
              :in          "query"
              :description "a city"
              :required    false
              :schema      {:description "a city"
                            :oneOf [{:enum [:tre :hki], :type "string"} ;from set
                                    {:type "null"}]}}
             {:name        "street"
              :in          "query"
              :description ""
              :required    false
              :schema      {:type "string"}}
             {:name        "filters"
              :in          "query"
              :description ""
              :required    false
              :schema      {:type  "array"
                            :items {:type "string"}}}
             {:name        "id"
              :in          "header"
              :description ""
              :required    true
              :schema      {:type   "integer"
                            :format "int32"}}
             {:name        "name"
              :in          "header"
              :description ""
              :required    true
              :schema      {:type "string"}}
             {:name        "address"
              :in          "header"
              :description ""
              :required    true
              :schema
              {:type                 "object"
               :properties
               {"street" {:type "string"}
                "city"   {:description "a city"
                          :oneOf [{:enum [:tre :hki] :type "string"}
                                  {:type "null"}]}}
               :required             ["street" "city"]
               :additionalProperties false
               :title                "schema-tools.openapi.core-test/Address"}}]}
           (openapi/openapi-spec
            {:parameters
             [{:name        "username"
               :in          "path"
               :description "username to fetch"
               :required    true
               :schema      {:type "string"}
               :style       "simple"}]
             ::openapi/parameters
             {:path   {:id Id}
              :query  {:name                     Name
                       (s/optional-key :city)    City
                       (s/optional-key :street)  Street
                       (s/optional-key :filters) Filters}
              :header User}})))

    (is (= {:parameters
            [{:name        "name2"
              :in          "query"
              :description "Will be the same"
              :required    true
              :schema      {:type "string"}}
             {:name        "id"
              :in          "path"
              :description ""
              :required    true
              :schema      {:type "integer" :format "int32"}}
             {:name        "city"
              :in          "query"
              :description "a city"
              :required    true
              :schema      {:description "a city"
                            :oneOf [{:enum [:tre :hki] :type "string"} {:type "null"}]}}
             {:name        "name"
              :in          "query"
              :description ""
              :required    false
              :schema      {:type "string"}}
             {:name        "street"
              :in          "query"
              :description ""
              :required    false
              :schema      {:type "string"}}
             {:name        "filters"
              :in          "query"
              :description ""
              :required    false
              :schema      {:type "array" :items {:type "string"}}}
             {:name        "street"
              :in          "cookie"
              :description ""
              :required    true
              :schema      {:type "string"}}
             {:name        "city"
              :in          "cookie"
              :description "a city"
              :required    true
              :schema      {:description "a city"
                            :oneOf [{:enum [:tre :hki] :type "string"} {:type "null"}]}}]}
           (openapi/openapi-spec
            {:parameters
             [{:name        "name"
               :in          "query"
               :description "Will be overridden"
               :required    false
               :schema      {:type "string"}}
              {:name        "name2"
               :in          "query"
               :description "Will be the same"
               :required    true
               :schema      {:type "string"}}]
             ::openapi/parameters
             {:path   {:id Id}
              :query  {:city                     City
                       (s/optional-key :name)    Name
                       (s/optional-key :street)  Street
                       (s/optional-key :filters) Filters}
              :cookie Address}}))))

  (testing "::schemas"
    (is (= {:components
            {:schemas
             {:some-object
              {:type "object"
               :properties
               {"name" {:type "string"}
                "desc" {:type "string"}}}
              :id {:type "integer" :format "int32"}
              :user
              {:type                 "object"
               :properties
               {"id"      {:type "integer" :format "int32"},
                "name"    {:type "string"}
                "address" {:type                 "object"
                           :properties
                           {"street" {:type "string"},
                            "city"   {:description "a city"
                                      :oneOf [{:enum [:tre :hki] :type "string"}
                                              {:type "null"}]}}
                           :required             ["street" "city"]
                           :additionalProperties false
                           :title                "schema-tools.openapi.core-test/Address"}}
               :required             ["id" "name" "address"]
               :additionalProperties false
               :title                "schema-tools.openapi.core-test/User"}
              :address
              {:type                 "object"
               :properties
               {"street" {:type "string"}
                "city"   {:description "a city"
                          :oneOf [{:enum [:tre :hki] :type "string"}
                                  {:type "null"}]}}
               :required             ["street" "city"]
               :additionalProperties false
               :title                "schema-tools.openapi.core-test/Address"}
              :some-request
              {:type                 "object"
               :properties
               {"id"      {:type "integer" :format "int32"}
                "name"    {:type "string"}
                "street"  {:type "string"}
                "filters" {:type "array" :items {:type "string"}}}
               :required             ["id" "name"]
               :additionalProperties false}}}}
           (openapi/openapi-spec
            {:components
             {:schemas
              {:some-object
               {:type "object"
                :properties
                {"name" {:type "string"}
                 "desc" {:type "string"}}}
               :user
               {:type  "string"
                :title "Will be overridden"}}
              ::openapi/schemas
              {:id           Id
               :user         User
               :address      Address
               :some-request {:id                       Id
                              :name                     Name
                              (s/optional-key :street)  Street
                              (s/optional-key :filters) Filters}}}}))))

  (testing "::content"
    (is (= {:content
            {"text/html"
             {:schema
              {:type "string"}}
             "application/json"
             {:schema
              {:type                 "object"
               :properties
               {"id"   {:type "integer" :format "int32"}
                "name" {:type "string"}
                "address"
                {:type                 "object"
                 :properties
                 {"street" {:type "string"}
                  "city"
                  {:description "a city"
                   :oneOf [{:enum [:tre :hki] :type "string"}
                           {:type "null"}]}}
                 :required             ["street" "city"]
                 :additionalProperties false
                 :title                "schema-tools.openapi.core-test/Address"}}
               :required             ["id" "name" "address"]
               :additionalProperties false
               :title                "schema-tools.openapi.core-test/User"}}
             "application/xml"
             {:schema
              {:type                 "object"
               :properties
               {"street" {:type "string"}
                "city"
                {:description "a city"
                 :oneOf [{:enum [:tre :hki] :type "string"}
                         {:type "null"}]}}
               :required             ["street" "city"]
               :additionalProperties false
               :title                "schema-tools.openapi.core-test/Address"}}
             "*/*"
             {:schema
              {:type                 "object"
               :properties
               {"id"      {:type "integer" :format "int32"}
                "name"    {:type "string"}
                "street"  {:type "string"}
                "filters" {:type "array" :items {:type "string"}}}
               :required             ["id" "name"]
               :additionalProperties false}}}}
           (openapi/openapi-spec
            {:content
             {"text/html"
              {:schema
               {:type "string"}}}
             ::openapi/content
             {"application/json" User
              "application/xml"  Address
              "*/*"              {:id                       Id
                                  :name                     Name
                                  (s/optional-key :street)  Street
                                  (s/optional-key :filters) Filters}}})))

    (is (= {:content
            {"application/json"
             {:schema
              {:type                 "object"
               :properties
               {"id"   {:type "integer" :format "int32"}
                "name" {:type "string"}
                "address"
                {:type                 "object"
                 :properties
                 {"street" {:type "string"}
                  "city"
                  {:description "a city"
                   :oneOf [{:enum [:tre :hki] :type "string"}
                           {:type "null"}]}}
                 :required             ["street" "city"]
                 :additionalProperties false
                 :title                "schema-tools.openapi.core-test/Address"}}
               :required             ["id" "name" "address"]
               :additionalProperties false
               :title                "schema-tools.openapi.core-test/User"
               :example              "Some examples here"
               :examples
               {:admin
                {:summary       "Admin user"
                 :description   "Super user"
                 :value         {:anything :here}
                 :externalValue "External value"}}
               :encoding             {:contentType "application/json"}}}}}
           (openapi/openapi-spec
            {::openapi/content
             {"application/json"
              (st/schema
               User
               {:openapi/example  "Some examples here"
                :openapi/examples {:admin
                                   {:summary       "Admin user"
                                    :description   "Super user"
                                    :value         {:anything :here}
                                    :externalValue "External value"}}
                :openapi/encoding {:contentType "application/json"}})}}))))

  (testing "::headers"
    (is (= {:headers
            {:X-Rate-Limit-Limit
             {:description "The number of allowed requests in the current period",
              :schema      {:type "integer"}},
             :City
             {:description "a city",
              :required    false,
              :schema
              {:enum [:tre :hki] :type "string"}}
             :Authorization
             {:description ""
              :required    true
              :schema      {:type "string"}}
             :User
             {:description ""
              :required    true
              :schema
              {:type                 "object"
               :properties
               {"id"   {:type "integer" :format "int32"}
                "name" {:type "string"}
                "address"
                {:type                 "object"
                 :properties
                 {"street" {:type "string"}
                  "city"
                  {:description "a city"
                   :oneOf [{:enum [:tre :hki] :type "string"}
                           {:type "null"}]}}
                 :required             ["street" "city"]
                 :additionalProperties false
                 :title                "schema-tools.openapi.core-test/Address"}}
               :required             ["id" "name" "address"]
               :additionalProperties false
               :title                "schema-tools.openapi.core-test/User"}}}}
           (openapi/openapi-spec
            {:headers
             {:X-Rate-Limit-Limit
              {:description "The number of allowed requests in the current period"
               :schema      {:type "integer"}}}
             ::openapi/headers
             {:City          City
              :Authorization Token
              :User          User}})))))

;; TODO: This test does not really validate schema
#?(:clj
   (deftest test-schema-validation
     (is (not
          (nil?
           (openapi/openapi-spec
            {:openapi "3.0.3"
             :info
             {:title          "Sample Pet Store App"
              :description    "This is a sample server for a pet store."
              :termsOfService "http://example.com/terms/"
              :contact
              {:name  "API Support",
               :url   "http://www.example.com/support"
               :email "support@example.com"}
              :license
              {:name "Apache 2.0",
               :url  "https://www.apache.org/licenses/LICENSE-2.0.html"}
              :version        "1.0.1"}
             :servers
             [{:url         "https://development.gigantic-server.com/v1"
               :description "Development server"}
              {:url         "https://staging.gigantic-server.com/v1"
               :description "Staging server"}
              {:url         "https://api.gigantic-server.com/v1"
               :description "Production server"}]
             :components
             {::openapi/schemas {:user    User
                                 :address Address}
              ::openapi/headers {:token Token}}
             :paths
             {"/api/ping"
              {:get
               {:description "Returns all pets from the system that the user has access to"
                :responses   {200 {::openapi/content
                                   {"application/xml" User
                                    "application/json"
                                    (st/schema
                                     Address
                                     {:openapi/example  "Some examples here"
                                      :openapi/examples {:admin
                                                         {:summary       "Admin user"
                                                          :description   "Super user"
                                                          :value         {:anything :here}
                                                          :externalValue "External value"}}
                                      :openapi/encoding {:contentType "application/json"}})}}}}}
              "/user/:id"
              {:post
               {:tags                ["user"]
                :description         "Returns pets based on ID"
                :summary             "Find pets by ID"
                :operationId         "getPetsById"
                :requestBody         {::openapi/content {"application/json" User}}
                :responses           {200      {:description "pet response"
                                                ::openapi/content
                                                {"application/json" User}}
                                      :default {:description "error payload",
                                                ::openapi/content
                                                {"text/html" User}}}
                ::openapi/parameters {:path   {:id Id}
                                      :header {:token Token}}}}}}))))))

(deftest backport-openapi-meta-unnamespaced
  (is (= {:type "string" :format "password" :random-value "42"}
         (openapi/transform
          (st/schema
           s/Str
           {:openapi/type         "string"
            :openapi/format       "password"
            :openapi/random-value "42"})
          nil))))
