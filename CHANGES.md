# Schema-tools CHANGELOG

We use [Break Versioning][breakver]. The version numbers follow a `<major>.<minor>.<patch>` scheme with the following intent:

| Bump    | Intent                                                     |
| ------- | ---------------------------------------------------------- |
| `major` | Major breaking changes -- check the changelog for details. |
| `minor` | Minor breaking changes -- check the changelog for details. |
| `patch` | No breaking changes, ever!!                                |

`-SNAPSHOT` versions are preview versions for upcoming releases.

[breakver]: https://github.com/ptaoussanis/encore/blob/master/BREAK-VERSIONING.md

## 0.12.0 (2019-06-14)

**[compare](https://github.com/metosin/schema-tools/compare/0.11.0...0.12.0)**

* Both String & JSON Coercion also coerce from keywords. This is useful as map keys are commonly keywordized in ring/http. Fixes [#54](https://github.com/metosin/schema-tools/issues/54). Thanks to [Mitchel Kuijpers](https://github.com/mitchelkuijpers)

```clj
(stc/coerce
  {:1 {:true "1", :false "2"}}
  {s/Int {s/Bool s/Any}}
  stc/json-coercion-matcher)
; {1 {true "1", false "2"}}
```

* Keys with `swagger` namespace in `st/schema` data contribute to Swagger Schema:

```clj
(require '[schema.core :as st])
(require '[schema-tools.core :as st])
(require '[schema-tools.swagger.core :as swagger])

(swagger/transform
  (st/schema 
    s/Str
    {:swagger/default "abba"
     :swagger/format "email"}))
; {:type "string"
;  :format "email"
;  :default "abba"}
```

* Updated deps:

```clj
[prismatic/schema "1.1.11"] is available but we use "1.1.9"
```

## 0.11.0 (2019-02-11)

**[compare](https://github.com/metosin/schema-tools/compare/0.10.5...0.11.0)**

- Prevent open-schema killing children ([#51](https://github.com/metosin/schema-tools/issues/51))
    - `open-schema` now uses `s/Keyword` as open key Schema, instead of `s/Any`

## 0.10.5 (2018-11-01)

**[compare](https://github.com/metosin/schema-tools/compare/0.10.4...0.10.5)**

- New options for handling default values ([#25](https://github.com/metosin/schema-tools/issues/25)):
  - `schema-tools.coerce/default-key-matcher` which adds missing map keys if they have default values specified.
  - `schema-tools.coerce/default-coercion-matcher` has been renamed to `stc/default-value-matcher`.
      - `default-coercion-matcher` is now a deprecated alias for `default-value-matcher`.
  - `schema-tools.coerce/default-matcher` combines the effects of `default-key-matcher` and `default-value-matcher`.

## 0.10.4 (2018-09-04)

**[compare](https://github.com/metosin/schema-tools/compare/0.10.3...0.10.4)**

- Fix ClojureScript (Closure) warning about reference to global RegExp object.
    - Using `js/RegExp` as Schema is no longer supported, instead one should use `schema.core/Regex`

## 0.10.3 (2018-05-23)

**[compare](https://github.com/metosin/schema-tools/compare/0.10.2...0.10.3)**

* `schema-tools.core/optional-keys-schema` to make all Map Schema keys optional (recursively)

## 0.10.2 (2018-05-08)

**[compare](https://github.com/metosin/schema-tools/compare/0.10.1...0.10.2)**

* Initial support of Schema->Swagger2, ported from [ring-swagger](https://github.com/metosin/ring-swagger) with added support for ClojureScript!
   * Few [issues](https://github.com/metosin/schema-tools/issues) still.
   * See [code](https://github.com/metosin/schema-tools/blob/master/src/schema_tools/swagger/core.cljc) for details.

## 0.10.1 (2018-03-27)

**[compare](https://github.com/metosin/schema-tools/compare/0.10.0...0.10.1)**

* **BUGFIX**: Works now with ClojureScript 1.10.238
    * MapEntry changes in the latest ClojureScript broke `walk`
* One Swagger, please.

## 0.10.0 (2018-02-19)

**[compare](https://github.com/metosin/schema-tools/compare/0.9.1...0.10.0)**

* **BREAKING**: Requires now Java1.8 (date coercions using `java.time`)
* **BREAKING**: `Default` record value is now `value`, not `default`, fixes [#34](https://github.com/metosin/schema-tools/issues/34)
* `schema-tools.coercion` contains now `json-coercion-matcher` and `string-coercion-matcher`, ported and polished from [Ring-swagger](https://github.com/metosin/ring-swagger)

## 0.9.1 (16.10.2017)

**[compare](https://github.com/metosin/schema-tools/compare/0.9.0...0.9.1)**

- `stc/corce` and `stc/coercer` default to `(constantly nil)` matcher
- `st/open-schema` transforms all nested Map Schemas to accept any extra keys
- Tested also against `[org.clojure/clojurescript "1.9.946"]` & `[org.clojure/clojure "1.9.0-beta2"]`
- Updated dependencies:

```clj
[prismatic/schema "1.1.7"] is available but we use "1.0.5"
[org.clojure/clojurescript "1.9.946"] is available but we use "1.9.562"
```

## 0.9.0 (20.4.2016)

**[compare](https://github.com/metosin/schema-tools/compare/0.8.0...0.9.0)**

- **BREAKING**: `schema-tools.walk/walk` argument order has been changed to match
`clojure.walk/walk`

## 0.8.0 (17.3.2016)

**[compare](https://github.com/metosin/schema-tools/compare/0.7.0...0.8.0)**

- Add `postwalk` and `prewalk` to `schema-tools.walk`
- `select-schema` migration helper has been dropped off
- Handle defaults via `(st/default Long 1)`& `stc/default-coercion-matcher`
- `stc/multi-matcher` for applying multiple coercions for same schemas & values
- Use Clojure 1.8 by default, test also with 1.7.0
- Updated dependencies:

```clj
[prismatic/schema "1.0.5"] is available but we use "1.0.3"
```

## 0.7.0 (8.11.2015)

**[compare](https://github.com/metosin/schema-tools/compare/0.6.0...0.7.0)**

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
