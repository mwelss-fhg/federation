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

================================
Federation Gateway Release Notes
================================

This server is available as a Docker image in a Docker registry at the Linux Foundation.
The image name is "federation-gateway" and the tag is a version string as shown below. 

Version 2.0.0, 2019-02-20
-------------------------

* Use Boreas log pattern; remove EELF (`ACUMOS-2329 <https://jira.acumos.org/browse/ACUMOS-2329>`_)
* Fix repeated update of metadata (`ACUMOS-2399 <https://jira.acumos.org/browse/ACUMOS-2399>`_)
* Update to CDS 2.0.7

Version 1.18.7, 2018-10-30
--------------------------

* Fix the subscription task early cancellation (`ACUMOS-1937 <https://jira.acumos.org/browse/ACUMOS-1937>`_)
* Fix the preemptive authentication (`ACUMOS-1952 <https://jira.acumos.org/browse/ACUMOS-1952>`_)

Version 1.18.6, 2018-10-08
--------------------------

* Fix for the handling of mis-represented content uris (`ACUMOS-1780 <https://jira.acumos.org/browse/ACUMOS-1780>`_)
* Adds subscription option directing the handling of error in content retrieval with respect to catalog updates

Version 1.18.5, 2018-10-02
--------------------------

* Fix for loss of file name prefix/suffix (`ACUMOS-1780 <https://jira.acumos.org/browse/ACUMOS-1780>`_)
* Fix for processing of docker artifacts, push to the local registry (`ACUMOS-1781 <https://jira.acumos.org/browse/ACUMOS-1781>`_)
* Add peer 'isActive' as controller calls pre-authorization check
* Fix the artifact content processing condition in the gateway

Version 1.18.4, 2018-09-21
--------------------------

* Fix download of large artifacts
* Upgrade Spring-Boot to 1.5.16.RELEASE (`ACUMOS-1754 <https://jira.acumos.org/browse/ACUMOS-1754>`_)

Version 1.18.3, 2018-09-14
--------------------------

* Increase max heap size
* configuration changes:
  new top level docker configuration block::

    "docker": {
        "host": "tcp://your_host:port",
        "registryUrl": "your_registry:port",
        "registryUsername": "docker_username",
        "registryPassword": "docker_password",
        "registryEmail": ""
    }

Version 1.18.2, 2018-09-13
--------------------------

* Rely on solution detail API for mapping (`ACUMOS-1690 <https://jira.acumos.org/browse/ACUMOS-1690>`_)
* Add binary stream to resource http content mapper (`ACUMOS-1690 <https://jira.acumos.org/browse/ACUMOS-1690>`_)
* Allow configuration of underlying executor and scheduler
* Do not overwrite user during mapping for local solutions

Version 1.18.1, 2018-09-05
--------------------------

* Simplified catalog solutions lookup
* Fix 'self' peer not found (`ACUMOS-1694 <https://jira.acumos.org/browse/ACUMOS-1694>`_)
* Fix task scheduler initialization (`ACUMOS-1690 <https://jira.acumos.org/browse/ACUMOS-1690>`_)
* Fix solution tag handling
* Move solution and revision updates to service interface

Version 1.18.0, 2018-09-05
--------------------------

* Align with data model changes from CDS 1.18.x
* Fix subscription update processing (`ACUMOS-1693 <https://jira.acumos.org/browse/ACUMOS-1693>`_)

Version 1.17.1, 2018-09-04
--------------------------

* Spread the use of configuration beans (`ACUMOS-1692 <https://jira.acumos.org/browse/ACUMOS-1692>`_)

Version 1.17.0, 2018-08-14
--------------------------

* Align with data model changes from CDS 1.17.x
* Add revision document federation (`ACUMOS-1606 <https://jira.acumos.org/browse/ACUMOS-1606>`_)
* Add tag federation (`ACUMOS-1544 <https://jira.acumos.org/browse/ACUMOS-1544>`_)
* Fix authorship federation (`ACUMOS-626 <https://jira.acumos.org/browse/ACUMOS-626>`_)
* The federation API for access to artifact and document content access have changed 
  to /solutions/{solutionId}/revisions/{revisionId}/artifacts/{artifactId}/content 
  and /solutions/{solutionId}/revisions/{revisionId}/documents/{documentId}/content

Version 1.16.1, 2018-08-08
--------------------------

* Temporary patch for tag handling during federation procedures

Version 1.16.0, 2018-08-01
--------------------------

* Aligns with the data model changes from CDS 1.16.x
* Minor fixes in order to adhere to project coding standards.

Version 1.15.1, 2018-07-31
--------------------------

* Fixes catalog solution lookup strategy due to used criteria moving to other entities (solution -> revision)
* Fixes some Sonar complaints
* Adds more unit tests for CDS based service implementations
* Align version numbers with CDS

Version 1.1.5, 2018-07-12
-------------------------

* Aligns with the data model changes from CDS 1.15 (`ACUMOS-1330 <https://jira.acumos.org/browse/ACUMOS-1330>`_)

Version 1.1.4.1, 2018-07-11
---------------------------

* Fix handling of docker images with no tags (`ACUMOS-1015 <https://jira.acumos.org/browse/ACUMOS-1015>`_)

Version 1.1.4, 2018-06-20
-------------------------

* Fix result size test when retrieving 'self' peer
* Fix handling of null solutions filter in the service. Fix the handling of no such item errors in catalog controller.

Version 1.1.3, 2018-05-10
-------------------------

* Upgrade to CDS 1.14.4

Version 1.1.2, 2018-04-19
-------------------------

* Revise code for Sonar warnings (`ACUMOS-672 <https://jira.acumos.org/browse/ACUMOS-672>`_)

Version 1.1.1, 2018-04-13
-------------------------

* Unit tests for local interface
* Separate federation and local service interfaces (`ACUMOS-276 <https://jira.acumos.org/browse/ACUMOS-276>`_)

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
