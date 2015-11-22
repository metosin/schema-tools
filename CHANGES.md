## Unreleased

- Add `postwalk` and `prewalk` to `schema-tools.walk`

## 0.7.0 (8.11.2015)

- Fixed problem with `walk` losing metadata for IMapEntries (or vectors) on
Clojure 1.8
- Converted source from Cljx to cljc
- Dropped support for Clojure 1.6
- Updated dependencies:

```clojure
[prismatic/schema "1.0.3"] is available but we use "1.0.2"
```

## 0.6.2 (28.10.2015)

- Fix select-schema bug introduced in 0.6.0: [#21](https://github.com/metosin/schema-tools/issues/21)
- `schema-tools.walk`
    - Added support for `s/constrained`
- Updated dependencies:

```clojure
[prismatic/schema "1.0.2"] is available but we use "1.0.1"
```

## 0.6.1 (29.9.2015)

- Fixed `walk` for `ConditionalSchema`

## 0.6.0 (9.9.2015)

- **BREAKING**: Supports and depends on Schema 1.0.0
- `schema-tools.walk`
    - Added support for walking `Conditional` and `CondPre` schemes.
    - Made sure leaf schemes (such as enum, pred, eq) are walked properly,
    i.e. `inner` is not called for them as they don't have sub-schemas.
    - Added `schema-tools.experimental.walk` which provides support for
    `schema.experimental.abstract-map`
- Updated dependencies:

```clojure
[prismatic/schema "1.0.1"] is available but we use "0.4.4"
```

## 0.5.2 (19.8.2015)

- fixed `select-schema` WARNING on ClojureScript.
- updated dependencies:

```clojure
[prismatic/schema "0.4.4"] is available but we use "0.4.3"
[org.clojure/clojurescript "1.7.107"] is available but we use "1.7.28"
```

## 0.5.1 (5.8.2015)

- new functions in `schema-tools.coerce` (idea by [Michael Griffiths](https://github.com/metosin/schema-tools/issues/10#issuecomment-124976346) & ring-swagger)
   - `coercer` to create a coercer, which throws exception if the value can't be coerced to match the schema.
   - `coerce` to create and apply a coercer, which throws exception if the value can't be coerced to match the schema.
   - error `:type` can overridden, defaulting to `:schema-tools.coerce/error`

## 0.5.0 (29.7.2015)

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
