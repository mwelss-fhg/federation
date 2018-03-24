.. ===============LICENSE_START=======================================================
.. Acumos CC-BY-4.0
.. ===================================================================================
.. Copyright (C) 2017-2018 AT&T Intellectual Property & Tech Mahindra. All rights reserved.
.. ===================================================================================
.. This Acumos documentation file is distributed by AT&T and Tech Mahindra
.. under the Creative Commons Attribution 4.0 International License (the "License");
.. you may not use this file except in compliance with the License.
.. You may obtain a copy of the License at
..
.. http://creativecommons.org/licenses/by/4.0
..
.. This file is distributed on an "AS IS" BASIS,
.. WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
.. See the License for the specific language governing permissions and
.. limitations under the License.
.. ===============LICENSE_END=========================================================

=================================
Federated Gateway Developer Guide
=================================

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

This section provides information for developing and testing the federaton gateway locally. We will run two instances of the gateway to depict 2 instance of acumos federated to each other.
In below scenario, we are going to run Acumos A and Acumos B for testing locally.

Launching
~~~~~~~~~

Start the microservice for development and testing like this::

	java -Djavax.net.ssl.trustStore=src/test/resources/acumosTrustStore.jks -Djavax.net.ssl.trustStorePassword=acumos -jar target/federated-gateway-1.0.0-SNAPSHOT.jar --spring.profiles.active="default,acumosa" 

	java -Djavax.net.ssl.trustStore=src/test/resources/acumosTrustStore.jks -Djavax.net.ssl.trustStorePassword=acumos -jar target/federated-gateway-1.0.0-SNAPSHOT.jar --spring.profiles.active="default,acumosb"

REST interface
--------------

The federation interface allows access via the federation gateway to informationavailable in an Acumos system. The main category of information that is exposed via the gateway is solution information: solution/revision/artifact and artifact content.
The federation gateway allows access from pre-registered peers via a REST interface running over HTTPS/SSL/TLS. The gateway requires mutual authentication, i.e. the client will be required to present a certificate. The gateway identifies a client as a pre-registered peer based on the certificates' subjectName (which implies that the subjectName must be communicated to the Acumos system administrator when the peer is provisioned).

API
~~~

All APIs encode the response in JSON. There is a top level envelope containing error information, and under the entry 'responseBody' it contains the actual content. All identifiers are UUIDs.

* /solutions
   List all public solutions. Accepts a query parameter, 'selector', which contains a JSON object with selection criteria, base64 encoded. Acceptable selection criteria are the solution object attributes. The entries are ANDed.

* /solutions/{solutionId}
  Retrieve one solution details.

* /solutions/{solutionId}/revisions
  List all revisions for a given solution.

* /solutions/{solutionId}/revisions/{revisionId}
  Retrieve one revision details

* /solutions/{solutionId}/revisions/{revisionId}/artifacts
  List all artifacts attached to a particular revision

* /artifacts/{artifactId}
  Retrieve one artifact details

* /artifacts/{artifactId}/download
  Download the artifact content.

Example of solutions selector:

 { "modelTypeCode":"CL" } will select all CLassifiers

Multiple values for a solution attribute are allowed and ORed

 { "modelTypeCode":["CL","PR"] } will select all CLassifiers and PRedictors
