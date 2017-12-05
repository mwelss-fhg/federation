#!/bin/bash

#java -Djavax.net.ssl.trustStore=src/test/resources/acumosTrustStore.jks -Djavax.net.ssl.trustStorePassword=acumos -Djavax.net.debug=all -jar target/federated-gateway-1.0.0-SNAPSHOT.jar --spring.profiles.active="default,acumosa" 
java -Djavax.net.ssl.trustStore=src/test/resources/acumosTrustStore.jks -Djavax.net.ssl.trustStorePassword=acumos -Djava.security.egd=file:/dev/./urandom -jar target/gateway-1.0.0-SNAPSHOT.jar --spring.profiles.active="default,acumosa" 
