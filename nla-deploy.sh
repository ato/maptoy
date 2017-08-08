#!/bin/bash
dest="$1"
mvn package
mkdir -p "$dest/lib"
mv -v target/*.jar "$dest/lib"
mvn dependency:copy-dependencies -DoutputDirectory="$dest/lib"

