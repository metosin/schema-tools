(ns schema-tools.core-test
  (:require #?(:clj [clojure.test :refer [deftest testing is]]
               :cljs [cljs.test :as test :refer-macros [deftest testing is]])
                    [schema-tools.core :as st]
                    [schema.core :as s :include-macros true]
                    [schema.coerce :as sc]
                    [schema-tools.coerce :as stc]))

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

(def basic-schema
  {:a s/Str
   (s/optional-key :b) s/Str
   (s/required-key "c") s/Str
   s/Keyword s/Str})

(deftest assoc-test
  #?(:clj (testing "odd number of arguments"
            (is (thrown? IllegalArgumentException
                         (st/assoc basic-schema :b s/Int :c)))))
  (testing "happy case"
    (is (= {(s/optional-key :a) s/Int
            :b s/Int
            "c" s/Int
            (s/optional-key :d) s/Int
            s/Keyword s/Int}
           (st/assoc basic-schema
                     (s/optional-key :a) s/Int
                     :b s/Int
                     "c" s/Int
                     (s/optional-key :d) s/Int
                     s/Keyword s/Int))))
  (testing "make anonymous if value changed"
    (is (not (nil? (meta (st/assoc Kikka :a s/Str)))))
    (is (nil? (meta (st/assoc Kikka :c s/Str))))))

(deftest dissoc-test
  (testing "dissoc"
    (is (= {s/Keyword s/Str} (st/dissoc basic-schema :a :b "c" :d))))
  (testing "make anonymous if value changed"
    (is (not (nil? (meta (st/dissoc Kikka :d)))))
    (is (nil? (meta (st/dissoc Kikka :a))))))

(deftest select-keys-test
  (testing "select-keys"
    (is (= {:a s/Str
            (s/optional-key :b) s/Str
            (s/required-key "c") s/Str}
           (st/select-keys basic-schema [:a :b "c" :d]))))
  (testing "make anonymous if value changed"
    (is (not (nil? (meta (st/select-keys Kikka [:a :b])))))
    (is (nil? (meta (st/select-keys Kikka [:a]))))))

(deftest open-schema-test
  (let [schema {:a s/Int, :b [(s/maybe {:a s/Int, s/Keyword s/Keyword})]}
        value {:a 1, :b [{:a 1, "kikka" "kukka"}], "kukka" "kakka"}]
    (is (= {:a s/Int, :b [(s/maybe {:a s/Int, s/Any s/Any})], s/Any s/Any}
           (st/open-schema schema)))
    (is (= value ((stc/coercer (st/open-schema schema)) value)))))

(def get-in-schema
  {:a {(s/optional-key :b) {(s/required-key :c) s/Str}}
   (s/optional-key "d") {s/Keyword s/Str}})

(def uniform-schema [s/Str])
(def schema-with-singles [(s/one s/Str "0") (s/optional s/Int "1") s/Keyword])
(def bounded-schema [(s/one s/Int "0") (s/optional s/Int "1")])
(def complex-schema {:a [(s/one {:b s/Int} "0") [s/Str]]})

(deftest get-in-test
  (is (= s/Str (st/get-in get-in-schema [:a (s/optional-key :b) (s/required-key :c)])))
  (is (= s/Str (st/get-in get-in-schema [:a :b :c])))
  (is (= s/Str (st/get-in get-in-schema ["d" s/Keyword])))
  (is (= nil (st/get-in get-in-schema [:e])))
  (testing "works with defaults"
    (is (= s/Str (st/get-in get-in-schema [:e] s/Str)))
    (is (= {:a s/Str} (st/get-in get-in-schema [:e :a] {:a s/Str}))))

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
    (is (= s/Int (st/get-in complex-schema [:a 0 :b]))))

  (testing "schema records in path are walked over as normal records"
    (let [schema {:a (s/maybe {:b s/Str})}]
      (is (= (s/maybe {:b s/Str}) (st/get-in schema [:a])))
      (is (= {:b s/Str} (st/get-in schema [:a :schema])))
      (is (= s/Str (st/get-in schema [:a :schema :b])))))

  #_(testing "maybe"
      (is (= s/Str (st/schema-value (s/maybe s/Str))))
      (is (= s/Str (st/get-in {:a (s/maybe {:b s/Str})} [:a :b]))))

  #_(testing "named"
      (is (= s/Str (st/schema-value (s/named s/Str 'FooBar))))
      (is (= s/Str (st/get-in {:a (s/named {:b s/Str} 'FooBar)} [:a :b]))))

  #_(testing "constrained"
      (is (= s/Str (st/schema-value (s/constrained s/Str odd?))))
      (is (= s/Str (st/get-in {:a (s/constrained {:b s/Str} odd?)} [:a :b]))))

  #_(testing "both"
      (is (= [{:a s/Str} {:a s/Int}] (st/schema-value (s/both {:a s/Str} {:a s/Int}))))
      (is (= s/Str (st/get-in {:a (s/both s/Str)} [:a 0])))
      (is (= s/Int (st/get-in {:a (s/both s/Str s/Int)} [:a 1])))
      (is (= s/Str (st/get-in (s/both {:a s/Str} {:a s/Int}) [0 :a])))
      (is (= s/Int (st/get-in (s/both {:a s/Str} {:a s/Int}) [1 :a]))))

  #_(testing "either"
      (is (= [{:a s/Str} {:a s/Int}] (st/schema-value (s/either {:a s/Str} {:a s/Int}))))
      (is (= s/Str (st/get-in {:a (s/either s/Str)} [:a 0])))
      (is (= s/Int (st/get-in {:a (s/either s/Str s/Int)} [:a 1])))
      (is (= s/Str (st/get-in (s/either {:a s/Str} {:a s/Int}) [0 :a])))
      (is (= s/Int (st/get-in (s/either {:a s/Str} {:a s/Int}) [1 :a]))))

  #_(testing "conditional"
      (is (= [{:a s/Str} {:a s/Int}] (st/schema-value (s/conditional odd? {:a s/Str} even? {:a s/Int}))))
      (is (= s/Str (st/get-in {:a (s/conditional odd? s/Str)} [:a 0])))
      (is (= s/Int (st/get-in {:a (s/conditional odd? s/Str even? s/Int)} [:a 1])))
      (is (= s/Str (st/get-in (s/conditional odd? {:a s/Str} even? {:a s/Int}) [0 :a])))
      (is (= s/Int (st/get-in (s/conditional odd? {:a s/Str} even? {:a s/Int}) [1 :a]))))

  #_(testing "cond-pre"
      (is (= [{:a s/Str} {:a s/Int}] (st/schema-value (s/cond-pre {:a s/Str} {:a s/Int}))))
      (is (= s/Str (st/get-in {:a (s/cond-pre s/Str)} [:a 0])))
      (is (= s/Int (st/get-in {:a (s/cond-pre s/Str s/Int)} [:a 1])))
      (is (= s/Str (st/get-in (s/cond-pre {:a s/Str} {:a s/Int}) [0 :a])))
      (is (= s/Int (st/get-in (s/cond-pre {:a s/Str} {:a s/Int}) [1 :a]))))

  #_(testing "enum"
      (is (= #{:a :b} (st/schema-value (s/enum :a :b))))))

(def assoc-in-schema
  {:a {(s/optional-key [1 2 3]) {(s/required-key "d") {}}}})

(deftest assoc-in-test
  (testing "assoc-in"
    (is (= {:a {(s/optional-key [1 2 3]) {(s/required-key "d") {:e {:f s/Str}}}}}
           (st/assoc-in assoc-in-schema [:a [1 2 3] "d" :e :f] s/Str))))
  (testing "schema records in path are walked over as normal records"
    (let [schema {:a (s/maybe {:b s/Str})}]
      (is (= {:a s/Str} (st/update-in schema [:a] (constantly s/Str))))
      (is (= {:a (s/maybe {:b s/Int})} (st/update-in schema [:a :schema :b] (constantly s/Int))))
      (is (= {:a (s/map->Maybe {:schema {:b s/Str} :b s/Int})} (st/update-in schema [:a :b] (constantly s/Int))))))
  (testing "make anonymous if value changed"
    (is (not (nil? (meta (st/assoc-in Kikka [:a] s/Str)))))
    (is (nil? (meta (st/assoc-in Kikka [:c :d] s/Str))))))

(def update-in-schema
  {:a {(s/optional-key [1 2 3]) {(s/required-key "d") s/Str}}})

(deftest update-in-test
  (testing "update-in"
    (is (= {:a {(s/optional-key [1 2 3]) {(s/required-key "d") s/Int}}}
           (st/update-in update-in-schema [:a [1 2 3] "d"] (constantly s/Int))))
    (is (= {:a {:b s/Str, :c s/Int}} (st/update-in {:a {:b s/Str}} [:a :c] (constantly s/Int)))))
  (testing "schema records in path are walked over as normal records"
    (let [schema {:a (s/maybe {:b s/Str})}]
      (is (= {:a s/Str} (st/update-in schema [:a] (constantly s/Str))))
      (is (= {:a (s/maybe {:b s/Int})} (st/update-in schema [:a :schema :b] (constantly s/Int))))
      (is (= {:a (s/map->Maybe {:schema {:b s/Str} :b s/Int})} (st/update-in schema [:a :b] (constantly s/Int))))))
  (testing "make anonymous if value changed"
    (is (not (nil? (meta (st/update-in Kikka [:a] (constantly s/Str))))))
    (is (nil? (meta (st/update-in Kikka [:c :d] (constantly s/Str)))))))

(def dissoc-in-schema
  {:a {(s/optional-key [1 2 3]) {(s/required-key "d") s/Str
                                 :kikka s/Str}}})

(def dissoc-in-schema-2
  (s/schema-with-name {:a {:b {:c s/Str}}} 'Kikka))

(deftest dissoc-in-test
  (testing "dissoc-in"
    (is (= {:a {(s/optional-key [1 2 3]) {:kikka s/Str}}}
           (st/dissoc-in dissoc-in-schema [:a [1 2 3] "d"]))))
  (testing "resulting empty maps are removed"
    (is (= {} (st/dissoc-in dissoc-in-schema [:a [1 2 3]]))))
  (testing "schema records in path are walked over as normal records"
    (let [schema {:a (s/maybe {:b s/Str})}]
      (is (= {} (st/dissoc-in schema [:a])))
      (is (= {} (st/dissoc-in schema [:a :schema])))
      (is (= schema (st/dissoc-in schema [:a :b])))))
  (testing "make anonymous if value changed"
    (is (not (nil? (meta (st/dissoc-in dissoc-in-schema-2 [:a :b :d])))))
    (is (nil? (meta (st/dissoc-in dissoc-in-schema-2 [:a :b :c]))))))

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

(deftest default-test
  (let [schema {:a (st/default s/Str "a") (s/optional-key :b) [{:c (st/default s/Int 42)}]}
        coerce (sc/coercer! schema stc/default-coercion-matcher)]
    (testing "missing keys are not added"
      (is (thrown? #?(:clj Exception :cljs js/Error) (coerce {}))))
    (testing "defaults are applied"
      (is (= {:a "a"} (coerce {:a nil})))
      (is (= {:a "a", :b [{:c 42}]} (coerce {:a nil :b [{:c nil}]}))))))

(deftest default-key-test
  (let [schema {:a (st/default s/Str "a")
                (s/optional-key :b)
                [{:c (st/default s/Int 42)}]}
        coerce (sc/coercer! schema stc/default-key-matcher)]
    (testing "missing keys are added"
      (is (= {:a "a"} (coerce {})))
      (is (= {:a "b"} (coerce {:a "b"})))
      (is (= {:a "a" :b [{:c 42}]} (coerce {:b [{}]}))))
    (testing "nils are not punned"
      (is (thrown? #?(:clj Exception :cljs js/Error) (coerce {:a nil}))))))

(def optional-keys-schema
  {(s/optional-key :a) s/Str
   (s/required-key :b) s/Str
   :c s/Str
   (s/required-key "d") s/Str})

(def optional-keys-schema-2
  (s/schema-with-name {(s/optional-key :a) s/Str, :b s/Str} 'Kikka))

(deftest optional-keys-test
  (testing "without extra arguments makes all top-level keys optional"
    (is (= (keys (st/optional-keys optional-keys-schema))
           [(s/optional-key :a) (s/optional-key :b) (s/optional-key :c) (s/optional-key "d")])))
  (testing "invalid input"
    (is (thrown-with-msg? #?(:clj AssertionError :cljs js/Error)
                          #"input should be nil or a vector of keys."
                          (st/optional-keys optional-keys-schema :ANY))))
  (testing "makes all given top-level keys are optional, ignoring missing keys"
    (is (= optional-keys-schema (st/optional-keys optional-keys-schema [:NON-EXISTING])))
    (is (= [(s/optional-key :a) (s/optional-key :b) :c (s/optional-key "d")]
           (keys (st/optional-keys optional-keys-schema [:a :b "d" :NON-EXISTING])))))
  (testing "make anonymous if value changed"
    (is (not (nil? (meta (st/optional-keys optional-keys-schema-2 [])))))
    (is (nil? (meta (st/optional-keys optional-keys-schema-2 [:b]))))))

(def required-keys-schema
  {(s/required-key :a) s/Str
   (s/optional-key :b) s/Str
   :c s/Str
   (s/optional-key "d") s/Str})

(def required-keys-schema-2
  (s/schema-with-name {(s/optional-key :a) s/Str, :b s/Str} 'Kikka))

(deftest required-keys-test
  (testing "without extra arguments makes all top-level keys required"
    (is (= [:a :b :c (s/required-key "d")]
           (keys (st/required-keys required-keys-schema)))))
  (testing "invalid input"
    (is (thrown-with-msg? #?(:clj AssertionError :cljs js/Error)
                          #"input should be nil or a vector of keys."
                          (st/required-keys required-keys-schema :ANY))))
  (testing "makes all given top-level keys are required, ignoring missing keys"
    (is (= required-keys-schema (st/required-keys required-keys-schema [:NON-EXISTING])))
    (is (= [:a :b :c (s/required-key "d")]
           (keys (st/required-keys required-keys-schema [:b [1 2 3] "d" :NON-EXISTING])))))
  (testing "make anonymous if value changed"
    (is (not (nil? (meta (st/required-keys required-keys-schema-2 [])))))
    (is (nil? (meta (st/required-keys required-keys-schema-2 [:a]))))))

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

(deftest schema-test
  (is (= 1 (s/validate (st/schema s/Int {}) 1)))
  (is (= 1 (stc/coerce "1" (st/schema s/Int {}) stc/string-coercion-matcher))))

(deftest optional-keys-schema-test
  (let [coercer (fn [schema matcher {:keys [open? loose?]}]
                  (let [f (comp (if loose? st/optional-keys-schema identity)
                                (if open? st/open-schema identity))]
                    (schema.coerce/coercer (f schema) matcher)))
        schema {:a s/Int, :b [(s/maybe {:a s/Int, s/Keyword s/Keyword})]}
        schema-coercer (coercer schema (constantly nil) {:open? true, :loose? true})]

    (testing "coerces values correctly"
      (is (= {:a 1, :b [{:a 1, "kikka" "kukka"}], "kukka" "kakka"}
             (schema-coercer {:a 1, :b [{:a 1, "kikka" "kukka"}], "kukka" "kakka"}))))

    (testing "returns coerced data even if missing keys/errors"
      (is (= {:a 1}
             (schema-coercer {:a 1}))))

    (testing "leaves extra keys"
      (is (= {:a 1 :z "extra"}
             (schema-coercer {:a 1
                              :z "extra"}))))

    (testing "coerces nested data"
      (is (= {:a 1, :b [{:a 1, "kikka" "kukka"}], "kukka" "kakka"}
             (schema-coercer {:a 1, :b [{:a 1, "kikka" "kukka"}], "kukka" "kakka"}))))

    (testing "leaves extra nested data"
      (is (= {:a 1, :b [{:a 1, "kikka" "kukka"
                         :nested "keep-me-please"}], "kukka" "kakka"}
             (schema-coercer {:a 1, :b [{:a 1, "kikka" "kukka"
                                         :nested "keep-me-please"}], "kukka" "kakka"}))))

    (testing "leave data"
      (is (= {:a 1, :b [{:a 1, "kikka" "kukka"}
                        {:b "keep-me-too"}], "kukka" "kakka"}
             (schema-coercer {:a 1, :b [{:a 1, "kikka" "kukka"}
                                        {:b "keep-me-too"}], "kukka" "kakka"}))))))
