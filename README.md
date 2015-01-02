# Schema-tools [![Build Status](https://travis-ci.org/metosin/schema-tools.png?branch=master)](https://travis-ci.org/metosin/schema-tools)

Common utilities on top of [Prismatic Schema](https://github.com/Prismatic/schema):
* common Schema definitions: `any-keys`, `any-keyword-keys`
* Schema-aware selectors: `get-in`, `select-keys`, `select-schema`
* Schema-aware transformers: `dissoc`, `assoc-in`, `update-in`, `dissoc-in`, `with-optional-keys`, `with-required-keys`

[API Docs](http://metosin.github.io/schema-tools/schema-tools.core.html).

## Latest version

[![Clojars Project](http://clojars.org/metosin/schema-tools/latest-version.svg)](http://clojars.org/metosin/schema-tools)

## Usage

See the [facts](https://github.com/metosin/schema-tools/blob/master/test/schema_tools/core_test.clj).

## Todo
- [ ] `update`, `merge`, `merge-with`
- [ ] Verify performance
- [ ] Context-aware Schemas
- [ ] XSD Generation(?)
- [ ] Common predicates missing from the schema.core
- [ ] Separate package for common finnish predicates, `YTunnus`, `Hetu` etc.

## License

Copyright © 2014 Metosin Oy

Distributed under the Eclipse Public License, the same as Clojure.
