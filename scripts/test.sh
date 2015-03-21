#!/bin/bash
set -e
lein trampoline run -m clojure.main scripts/build.clj
node scripts/runner.js
