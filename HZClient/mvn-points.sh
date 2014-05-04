#!/bin/sh
mvn exec:java\
 -Dexec.mainClass=com.esri.Main\
 -Dexec.args="points ${ACELD_HOME}/Full1997-2013Africa.shp"
