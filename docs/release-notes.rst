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

===============================
Federated Gateway Release Notes
===============================

The Federated Gateway server is available as a Docker image in a Docker registry.

Version 1.1.5, 2018-07-12
-------------------------

* Aligns with the data model changes from CDS 1.15 (ACUMOS-1330)

Version 1.1.4.1, 2018-07-11
---------------------------

* Fix handling of docker images with no tags (ACUMOS-1015)

Version 1.1.4, 2018-06-20
-------------------------

* Fix result size test when retrieving 'self' peer
* Fix handling of null solutions filter in the service. Fix the handling of no such item errors in catalog controller.

Version 1.1.3, 2018-05-10
-------------------------

* Upgrade to CDS 1.14.4

Version 1.1.2, 2018-04-19
-------------------------

* Revise code for Sonar warnings (ACUMOS-672)

Version 1.1.1, 2018-04-13
-------------------------

* Unit tests for local interface
* Separate federation and local service interfaces (ACUMOS-276)

Version 1.1.0, 2018-03-09
-------------------------

* Separate between federation and local interface with respect to network configuration, authorization and available REST API.
* Upgrade to CDS 1.14.0

Version 1.0.0, 2018-02-12
-------------------------

* Use release (not snapshot) versions of acumos-nexus-client and common-dataservice libraries
* Limit JVM memory use via Docker start command
* Revise docker projects to deploy images to nexus3.acumos.org
* Make aspectjweaver part of runtime
* Add dependency copy plugin

Version 0.2.0, 2017-11-28
-------------------------

* Support to CDS 1.9.0
* 2-Way SSL Support
* X509 Subject Principal Authentication
