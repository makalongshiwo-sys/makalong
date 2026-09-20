#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p out/domain
java com.sun.tools.javac.Main -encoding UTF-8 -d out/domain app/src/main/java/com/tide/journal/domain/Market.java app/src/main/java/com/tide/journal/domain/Lessons.java tests/DomainTest.java
java -cp out/domain DomainTest
