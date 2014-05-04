#!/bin/sh
mvn exec:java\
 -Dexec.mainClass=com.esri.Main\
 -Dexec.args="-13.05 26.13 -5.47 30.40"
