(ns schema-tools.walk-test
  (:require #+clj  [clojure.test :refer [deftest testing is]]
            #+cljs [cljs.test :as test :refer-macros [deftest testing is]]
            [schema-tools.walk :as sw]
            [schema.core :as s]))

(deftest walk-test
  (testing "identity doesn't change the schema"
    (let [s {:a s/Str
             :b s/Int
             :s (s/maybe s/Str)}]
      (is (= (sw/walk s identity identity) s))))

  (testing "inner is called with the MapEntries"
    (let [k (atom [])]
      (sw/walk {:a s/Str :b s/Str}
               (fn [x]
                 (swap! k conj x)
                 x)
               identity)
      (is (= @k [[:a s/Str] [:b s/Str]]))))

  (testing "elements can be replaced"
    (is (= (sw/walk {:a s/Str :b s/Str}
                    (fn [[k v]]
                      [k (s/maybe v)])
                    identity)
           {:a (s/maybe s/Str)
            :b (s/maybe s/Str)})))

  (testing "Insides of schemas are walked and can be replaced"
    (letfn [(replace-str [s]
              (sw/walk s
                       (fn [x]
                         (if (= x s/Str)
                           s/Int
                           (replace-str x)))
                       identity))]
      (is (= (replace-str {:a (s/maybe s/Str)})
             {:a (s/maybe s/Int)})))))

(defn map-entry? [x]
  #+clj
  (instance? clojure.lang.IMapEntry x)
  #+cljs
  (satisfies? IMapEntry x))

(defn name-schemas [names schema]
  (sw/walk schema
           (fn [x]
             (if (map-entry? x)
               [(key x) (name-schemas (conj names (s/explicit-schema-key (key x))) (val x))]
               (name-schemas names x)))
           (fn [x]
             (if (map? x)
               (if-not (s/schema-name x)
                 (with-meta x {:name names})
                 x)
               x))))

(deftest name-schemas-test
  (let [named (name-schemas [:root] {:a {:b s/Str}
                                     :b {:c {:d s/Int}}})]
    (is (= (-> named :a meta :name) [:root :a]))
    (is (= (-> named :b meta :name) [:root :b]))
    (is (= (-> named :b :c meta :name) [:root :b :c]))))

; Records

(defrecord Test [a b])

(deftest walk-record-test
  (let [named (name-schemas [:root] (Test. {:a s/Str} {:c {:d s/Int}}))]
    (is (= (-> named .-a meta :name) [:root :a]))
    (is (= (-> named .-b meta :name) [:root :b]))
    (is (instance? Test named))))

(deftest condpre-test
  (let [k (atom [])]
    (sw/walk (s/cond-pre [s/Str] s/Str)
             (fn [x] (swap! k conj x) x)
             identity)
    (is (= [[s/Str] s/Str] @k))))
