## 0.5.0

- remove `safe-coercer`
- new `map-filter-matcher` to strip illegal keys from non-record maps
  - original code by [abp](https://gist.github.com/abp/0c4106eba7b72802347b)
- **breaking**: `select-schema` signature and functionality has changed
  - fn argument order has changed to be consistent with other fns
    - `[schema value]` -> `[value schema]`
    - `[matcher schema value]` -> `[value schema matcher]`
    - to help migration: throws ex-info with message `"Illegal argument order - breaking change in 0.5.0."` if second argument is not a schema
  - uses schema coercion (`map-filter-matcher`) to drop illegal keys
    - fixes [#4](https://github.com/metosin/schema-tools/issues/4) - works now also with predicate keys
    - if a value can't be coerced, Exception is thrown - just like from `schema.core/validate`

```clojure
(st/select-schema
  {:a "a"
   :z "disallowed key"
   :b "disallowed key"
   :x-kikka "x-kikka"
   :x-kukka "x-kukka"
   :y-kikka "invalid key"}
  {(s/pred #(re-find #"x-" (name %)) ":x-.*") s/Any, :a String})
; {:a "a", :x-kikka "x-kikka", :x-kukka "x-kukka"}
```

```clojure
(st/select-schema {:beer "ipa" :taste "good"} {:beer (s/enum :ipa :apa)} )
; clojure.lang.ExceptionInfo: Could not coerce value to schema: {:beer (not (#{:ipa :apa} "ipa"))}
;     data: {:type :schema.core/error,
;            :schema {:beer {:vs #{:ipa :apa}}},
;            :value {:beer "ipa", :taste "good"},
;            :error {:beer (not (#{:ipa :apa} "ipa"))}}
           
(require '[schema.coerce :as sc])

(st/select-schema {:beer "ipa" :taste "good"} {:beer (s/enum :ipa :apa)} sc/json-coercion-matcher)
; {:beer :ipa}
```

## 0.4.3 (11.6.2015)

- `select-schema` takes now optional coercion matcher - to coerce values safely in a single sweep
- `or-matcher`

## 0.4.2 (26.5.2015)

- fix for [#7](https://github.com/metosin/schema-tools/issues/7)
- updated dependencies:

```clojure
[prismatic/schema "0.4.3"] is available but we use "0.4.2"
[org.clojure/clojurescript "0.0-3297"] is available but we use "0.0-3269"
```

## 0.4.1 (17.5.2015)

- meta-data helpers: `schema-with-description` `schema-description`, `resolve-schema` (clj only), `resolve-schema-description` (clj only)
- updated dependencies:

```clojure
[prismatic/schema "0.4.2"] is available but we use "0.4.0"
[codox "0.8.12"] is available but we use "0.8.11"
[org.clojure/clojurescript "0.0-3269"] is available but we use "0.0-3196"
```

## 0.4.0 (13.4.2015)

- implemented `assoc`
- dissoc away schema-name from meta-data (key `:name`) if the transforming functions have changed the schema.
  - `assoc`, `dissoc`, `select-keys`, `assoc-in`, `update-in`, `dissoc-in`, `update`, `merge`, `optional-keys`, `required-keys`
  - fixes [#2](https://github.com/metosin/schema-tools/issues/2)

## 0.3.0 (21.3.2015)

- Added `schema-tools.walk` namespace
  - Implements `clojure.walk/walk` like `walk` function which knows how to
  traverse through Schemas.
- Updated to `[prismatic/schema "0.4.0"]`

## 0.2.0 (1.2.2015)

- **BREAKING**: `with-optional-keys` and `with-required-keys` are renamed to `optional-keys` and `required-keys` and take vector now of keys instead of vararg keys
- implemented `merge`, `update`
- updated deps:
```clojure
[prismatic/schema "0.3.6"] is available but we use "0.3.3"
[org.clojure/clojurescript "0.0-2740"] is available but we use "0.0-2665"
```

## 0.1.2 (21.1.2015)

- ClojureScript tests
- Fixed warning about `Object` on Cljs.
