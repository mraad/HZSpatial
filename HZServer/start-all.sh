#!/bin/sh
java -server -Xms2G -Xmx16G -Djava.net.preferIPv4Stack=true -jar target/HZServer-1.0-SNAPSHOT.jar
