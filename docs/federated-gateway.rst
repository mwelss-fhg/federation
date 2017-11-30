Building and Packaging
----------------------

Prerequisites
~~~~~~~~~~~~~

The build machine needs the following:

1. Java version 1.8
2. Maven version 3
3. Connectivity to Maven Central (for most jars)

Use below maven command to build and package the gateway service into a single jar::

	mvn clean package

Development and Local Testing
-----------------------------

This section provides information for developing and testing the Fedrated Gateway locally. We will run two instances of the gateway to depict 2 instance of acumos federated to each other.
In below scenario, we are going to run Acumos A and Acumos B for testing locally.

Launching
~~~~~~~~~

Start the microservice for development and testing like this::

	java -Djavax.net.ssl.trustStore=src/test/resources/acumosTrustStore.jks -Djavax.net.ssl.trustStorePassword=acumos -jar target/federated-gateway-1.0.0-SNAPSHOT.jar --spring.profiles.active="default,acumosa" 

	java -Djavax.net.ssl.trustStore=src/test/resources/acumosTrustStore.jks -Djavax.net.ssl.trustStorePassword=acumos -jar target/federated-gateway-1.0.0-SNAPSHOT.jar --spring.profiles.active="default,acumosb"