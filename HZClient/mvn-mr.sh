#!/bin/sh
mvn exec:java -Dexec.mainClass=com.esri.Main -Dexec.args="mr density.csv"
