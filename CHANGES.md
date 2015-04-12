## 0.4.0 (12.4.2015)

- implemented `assoc`
- dissoc away schema-name from meta-data (key `:name`) if the transforming functions have changed the schema.
  - `assoc`, `dissoc`, `select-keys`, `assoc-in`, `update-in`, `dissoc-in`, `update`, `merge`, `optional-keys`, `required-keys`

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
