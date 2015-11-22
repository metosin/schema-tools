(ns schema-tools.walk-test
  (:refer-clojure :exclude [map-entry?])
  (:require #?(:clj  [clojure.test :refer [deftest testing is are]]
               :cljs [cljs.test :as test :refer-macros [deftest testing is are]])
            [schema-tools.walk :as sw]
            [schema-tools.core :as st]
            [schema.core :as s]))

(deftest walk-test
  (testing "identity doesn't change the schema"
    (let [s {:a s/Str
             :b s/Int
             :s (s/maybe s/Str)}]
      (is (= s (sw/walk identity identity s)))))

  (testing "inner is called with the MapEntries"
    (let [k (atom [])]
      (sw/walk (fn [x]
                 (swap! k conj x)
                 x)
               identity
               {:a s/Str :b s/Str})
      (is (= [[:a s/Str] [:b s/Str]] @k))))

  (testing "elements can be replaced"
    (is (= {:a (s/maybe s/Str)
            :b (s/maybe s/Str)}
           (sw/walk (fn [[k v]]
                      [k (s/maybe v)])
                    identity
                    {:a s/Str :b s/Str}))))

  (testing "Insides of schemas are walked and can be replaced"
    (letfn [(replace-str [s]
              (sw/walk (fn [x]
                         (if (= x s/Str)
                           s/Int
                           (replace-str x)))
                       identity
                       s))]
      (is (= {:a (s/maybe s/Int)}
             (replace-str {:a (s/maybe s/Str)}))))))

(deftest legacy-walk-arguments-test
  (testing "inner is called with the MapEntries"
    (let [k (atom [])]
      (sw/walk {:a s/Str :b s/Str}
               (fn [x]
                 (swap! k conj x)
                 x)
               identity)
      (is (= [[:a s/Str] [:b s/Str]] @k)))))

(defn map-entry? [x]
  #?(:clj  (instance? clojure.lang.IMapEntry x)
     :cljs (satisfies? IMapEntry x)))

(defn name-schemas [names schema]
  (sw/walk (fn [x]
             (if (map-entry? x)
               [(key x) (name-schemas (conj names (s/explicit-schema-key (key x))) (val x))]
               (name-schemas names x)))
           (fn [x]
             (if (map? x)
               (if-not (s/schema-name x)
                 (with-meta x {:name names})
                 x)
               x))
           schema))

(deftest name-schemas-test
  (let [named (name-schemas [:root] {:a {:b s/Str}
                                     :b {:c {:d s/Int}}})]
    (is (= [:root :a] (-> named :a meta :name)))
    (is (= [:root :b] (-> named :b meta :name)))
    (is (= [:root :b :c] (-> named :b :c meta :name))))

  (let [named (name-schemas [:root] {:a {:b (with-meta [s/Str s/Int s/Bool] {:foo "bar"})}})]
    (is (= [:root :a] (-> named :a meta :name)))
    (is (= [s/Str s/Int s/Bool] (-> named :a :b))
        "IMapEntry walk doesn't lose entries for other vectors")
    (is (= "bar" (-> named :a :b meta :foo))
        "IMapEntry walk keeps metadata")))

(deftest leaf-schema-test
  (are [schema]
       (testing (pr-str schema)
         (let [fail (atom false)
               success (atom false)]
           (sw/walk (fn [x] (reset! fail true) x) (fn [x] (reset! success true) x) schema)
           (is (not @fail))
           (is @success)))

       s/Any
       (s/eq 5)
       (s/isa ::parent)
       (s/enum :parent :child)
       (s/pred odd? 'odd)
       (s/protocol schema.core.Schema)))

; Records

(defrecord Test [a b])

(deftest walk-record-test
  (let [named (name-schemas [:root] (Test. {:a s/Str} {:c {:d s/Int}}))]
    (is (= [:root :a] (-> named .-a meta :name)))
    (is (= [:root :b] (-> named .-b meta :name)))
    (is (instance? Test named))))

(deftest conditional-test
  (let [k (atom [])]
    (sw/walk (fn [x] (swap! k conj x) x)
             identity
             (s/conditional :a {:a s/Str} :b {:b s/Num} (constantly true) {:c s/Bool}))
    (is (= [{:a s/Str} {:b s/Num} {:c s/Bool}] @k))))

(deftest condpre-test
  (let [k (atom [])]
    (sw/walk (fn [x] (swap! k conj x) x)
             identity
             (s/cond-pre [s/Str] s/Str))
    (is (= [[s/Str] s/Str] @k))))

(deftest constrained-test
  (let [k (atom [])]
    (sw/walk (fn [x] (swap! k conj x) x)
             identity
             (s/constrained s/Int even?))
    (is (= [s/Int] @k))))

(defn recursive-optional-keys [m]
  (sw/postwalk (fn [s]
                 ; FIXME: Should a helper fn be provided to check if value is a map schema?
                 (if (and (map? s) (not (record? s)))
                   (st/optional-keys s)
                   s))
               m))

(deftest recursive-optional-keys-test
  (is (= {(s/optional-key :a) s/Str
          (s/optional-key :b) {(s/optional-key :c) s/Str}}
         (recursive-optional-keys {:a s/Str
                                   :b {:c s/Str}})))

  (is (= (s/constrained {(s/optional-key :a) s/Str
                         (s/optional-key :b) {(s/optional-key :c) s/Str}}
                        map?)
         (recursive-optional-keys (s/constrained {:a s/Str
                                                  :b {:c s/Str}}
                                                 map?)))))
