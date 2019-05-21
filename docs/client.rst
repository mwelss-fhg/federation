.. ===============LICENSE_START=======================================================
.. Acumos CC-BY-4.0
.. ===================================================================================
.. Copyright (C) 2019 AT&T Intellectual Property & Tech Mahindra. All rights reserved.
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

==================================================
Developer Guide for the Federation Gateway Clients
==================================================

The acumos-fgw-client library provides clients for using the
public "E5" and private interfaces of the Acumos machine-learning platform's
Federation Gateway.

This document offers guidance for both client developers and client users
(developers who want to use the clients in their Java projects).

Maven Dependency
----------------

The client jar is deployed to these Nexus repositories at the Linux Foundation::


	<repository>
		<id>releases</id>
		<url>https://nexus.acumos.org/content/repositories/releases</url>
	</repository>

Use this dependency information, ideally with the latest version number shown in the release notes::

	<dependency>
		<groupId>org.acumos.federation</groupId>
		<artifactId>acumos-fgw-client</artifactId>
		<version>2.x.x</version>
	</dependency>
	<dependency>
		<groupId>org.acumos.federation</groupId>
		<artifactId>acumos-fgw-client-config</artifactId>
		<version>2.x.x</version>
	</dependency>

Building and Packaging
----------------------

As of this writing the build (continuous integration) process is fully automated in the Linux Foundation system
using Gerrit and Jenkins.  This section describes how to perform local builds for development and testing.

Prerequisites
~~~~~~~~~~~~~

The build and test machine needs the following:

1. Java version 1.8
2. Maven version 3
3. Connectivity to Maven Central to download required jars

Use maven to build and package the client jar using this command::

    mvn package

Client Packages
---------------

The client consists of several Maven sub-projects each defining one or more
Java packages.


acumos-fgw-client-config
  Dependency::
	<dependency>
		<groupId>org.acumos.federation</groupId>
		<artifactId>acumos-fgw-client-config</artifactId>
		<version>2.x.x-SNAPSHOT</version>
	</dependency>

  Packages:
    org.acumos.federation.client.config
      This contains pure bean classes for specifying TLS (SSL) and authentication
      parameters to be used by a client.  These beans use Project Lombok for
      automatic generation of code for their setter, getter, constructor,
      equals, and hashcode methods.  Their code may be found under
      src/main/lombok.

acumos-fgw-client-test
  Dependency::
	<dependency>
		<groupId>org.acumos.federation</groupId>
		<artifactId>acumos-fgw-client-test</artifactId>
		<version>2.x.x-SNAPSHOT</version>
		<scope>test</scope>
	</dependency>

  Packages:
    org.acumos.federation.client.test
      This contains classes for mocking out client responses in junit testing
      in order to test applications using the clients (such as the Federation
      Gateway itself) as well as test key- and trust-store files for using the
      clients to test servers supporting the REST APIs.  It also contains
      convenience routines for generating configuration beans using those
      key- and trust-store files and for mapping single quotes to double quotes
      for writing JSON strings in Java code with a minimum of backslashes.

acumos-fgw-client
  Dependency::
	<dependency>
		<groupId>org.acumos.federation</groupId>
		<artifactId>acumos-fgw-client</artifactId>
		<version>2.x.x-SNAPSHOT</version>
	</dependency>

  Packages:
    org.acumos.federation.client.data
      This contains pure bean classes for messages sent between the clients
      and the Federation Gateway server that are unique to those APIs.
      These beans use Project Lombok for automatic generation of code
      for their setter, getter, constructor, equals, and hashcode methods.
      Their code may be found under src/main/lombok.
    org.acumos.federation.client
      This contains the actual client code, itself, consisting of a ClientBase
      class used as the common superclass for both interfaces, and
      FederationClient and GatewayClient for the public "E5" and private
      interfaces, respectively.

Client Usage Example
--------------------

A Java class named "ClientDemo" demonstrates use of the clients.
Please browse for this file in the client project test area using this link:
`ClientDemo.java <https://gerrit.acumos.org/r/gitweb?p=federation.git;a=blob;f=acumos-fgw-client/src/test/java/org/acumos/federation/client/ClientDemo.java;hb=refs/heads/master>`_.
