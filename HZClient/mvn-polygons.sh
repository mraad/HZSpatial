#!/bin/sh
mvn exec:java\
 -Dexec.mainClass=com.esri.Main\
 -Dexec.args="polygons ../data/ACLEDHex.shp"
