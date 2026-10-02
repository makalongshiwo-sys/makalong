#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p out/domain
java com.sun.tools.javac.Main -encoding UTF-8 -d out/domain app/src/main/java/com/tide/journal/domain/Market.java app/src/main/java/com/tide/journal/domain/Lessons.java app/src/main/java/com/tide/journal/domain/EtfHistory.java app/src/main/java/com/tide/journal/data/HttpRequests.java app/src/main/java/com/tide/journal/domain/PriceWindow.java tests/PriceWindowTest.java tests/DomainTest.java tests/EtfHistoryTest.java tests/HttpRequestsTest.java
java -cp out/domain DomainTest

java -cp out/domain EtfHistoryTest
java -cp out/domain com.tide.journal.data.HttpRequestsTest

java -cp out/domain PriceWindowTest
