#!/bin/bash

java -Djavax.net.ssl.trustStore=src/test/resources/acumosTrustStore.jks -Djavax.net.ssl.trustStorePassword=acumos -Djava.security.egd=file:/dev/./urandom  -jar target/gateway-1.0.0-SNAPSHOT.jar --spring.profiles.active="default,acumosb"
