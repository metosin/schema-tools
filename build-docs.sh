#!/bin/bash

rev=$(git rev-parse HEAD)

if [[ ! -d doc ]]; then
    git clone --branch gh-pages git@github.com:metosin/schema-tools.git doc
fi
(
cd doc
git pull
)

lein doc
cd doc
git add --all
git commit -m "Build docs from ${rev}."
git push origin gh-pages
