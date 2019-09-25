.. ===============LICENSE_START=======================================================
.. Acumos CC-BY-4.0
.. ===================================================================================
.. Copyright (C) 2017-2019 AT&T Intellectual Property & Tech Mahindra. All rights reserved.
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

Version 3.0.1, 2019-09-26
-------------------------
* When a model has been federated, register it with the license manager (`ACUMOS-3484 <https://jira.acumos.org/browse/ACUMOS-3484>`_)
  * This adds a new required configuration value, "license-manager.url" for the
    license management service.

Version 3.0.0, 2019-09-13
-------------------------
* Upgrade server to Java 11.  Compile client for Java 8 (`ACUMOS-3334 <https://jira.acumos.org/browse/ACUMOS-3334>`_)
  * Compile and run with Java 11, but keep client library compliance level at Java 8.

* Add "acumos/" prefix to container image name

* Update to CDS 3.0.0

Version 2.3.0, 2019-09-06
-------------------------
* Portal to show details of federation actions (`ACUMOS-1778 <https://jira.acumos.org/browse/ACUMOS-1778>`_)

* Run SV license scan when a model has been federated (`ACUMOS-3396 <https://jira.acumos.org/browse/ACUMOS-3396>`_)
  * This adds a new required configuration value, "verification.url" for the
    security verification service.

* Java code upgrade to Java 11 (`ACUMOS-3334 <https://jira.acumos.org/browse/ACUMOS-3334>`_)

* Update to CDS 2.2.6

* Fix DI artifact create fail due to Federation use of a stale TCP stream (`ACUMOS-3193 <https://jira.acumos.org/browse/ACUMOS-3193>`_)

* Federated model DI name to include model name - same as source peer DI name (`ACUMOS-3195 <https://jira.acumos.org/browse/ACUMOS-3195>`_)

* Publish E5 Federation client library (`ACUMOS-2760 <https://jira.acumos.org/browse/ACUMOS-2760>`_)

  3 new sub-projects are introduced, in addition to the existing "gateway" sub-project.
  * "acumos-fgw-client-config" contains bean classes used to specify properties
    of a client's connection to its server, including basic authentication and
    TLS (SSL) related properties.

  * "acumos-fgw-client-test" contains classes for providing mock responses to
    a client for testing applications that make calls to a server, as well as
    dummy key store and trust store files to enable a client to be used to
    test a server.

  * "acumos-fgw-client" contains implementations of clients for both the
    external "E5" and private interfaces to the Acumos Federation Gateway
    as well as bean classes for the JSON wire formats used by those interfaces.

  The existing "gateway" project is modified to use the client subproject when
  making requests to a peer Acumos instance, when sending or receiving
  artifacts from the Nexus server, and for creating the rest template used
  to communicate with CDS.

* Access to the Swagger API is fixed and now gives responses appropriate to
  the interface being queried (external "E5" or private).

* Some configuration is simplified.
  * The federation.ssl.client-auth configuration parameter is now named
    federation.client-auth and defaults to WANT, enabling access to the
    Swagger specification on the external "E5" interface without requiring
    a client certificate.  Attempts to access the REST API endpoints without
    providing a client certificate will return a 403 Forbidden error.
  * The local.ssl.client-auth configuration parameter is now named
    local.client-auth and defaults to WANT, enabling access to the
    Swagger specification on the private interface without requiring
    a client certificate.  Attempts to access the REST API endpoints without
    providing a client certificate will return a 403 Forbidden error.
  * The federation.registration.enabled configuration parameter is now named
    federation.registration-enabled.  It still defaults to False.
  * The federation.instance configuration parameter no longer needs to be set to
    "gateway" and no longer has any effect.
  * The value "local" in the spring.profiles.active configuration parameter no
    longer has any effect.
  * The catalog.catalogs-selector configuration parameter no longer has any effect.
  * The various task.* configuration parameters no longer have any effect.
  * The cdms.client.page-size configuration parameter no longer has any effect.
  * The catalog-local.source, catalog-local.catalogs, codes-local.source,
    peers-local.source, and peer-local.interval configuration parameters no
    longer have any effect.

* Documentation is updated to reflect these changes.

Version 2.2.1, 2019-07-18
-------------------------
* Fix Boreas branch Jenkins build not working (`ACUMOS-3244 <https://jira.acumos.org/browse/ACUMOS-3244>`_)

* Fix DI artifact create fail due to Federation use of a stale TCP stream (`ACUMOS-3193 <https://jira.acumos.org/browse/ACUMOS-3193>`_)

* Federated model DI name to include model name - same as source peer DI name (`ACUMOS-3195 <https://jira.acumos.org/browse/ACUMOS-3195>`_)

Version 2.2.0, 2019-04-16
-------------------------
* Increase Spring async task timeout value (spring.mvc.async.request-timeout)
  to 10 minutes (`ACUMOS-2749 <https://jira.acumos.org/browse/ACUMOS-2749>`_)

  This prevents timeouts during retrieval of large docker image artifacts.

* Update to CDS 2.2.x with subscription by catalogs (`ACUMOS-2732 <https://jira.acumos.org/browse/ACUMOS-2732>`_)

  This makes changes to the REST api for accessing Federation on both the
  public and private interfaces:

  * When listing solutions, the optional selector query parameter is replaced
    by a required catalogId query parameter

  * When getting revision details an optional catalogId query parameter is
    added, used to retrieve descriptions and documents, from that catalog, for
    the revision.  If not specified, no descriptions or documents are returned.

  * When getting artifact and document content, the form of the URI is changed
    to eliminate the unused solution and revision IDs.

  * When getting documents for a revision, the form of the URI is changed
    to eliminate the unused solution ID and a required catalogID query parameter
    is added.

  Solution revisions in CDS no longer have access type codes, so the (optional)
  catalog.default-access-type-code configuration parameter has been removed.

* Eliminate vulnerabilities and many "code smells" identified by SONAR.

Version 2.1.2, 2019-03-27
-------------------------
* Add JUnit test cases to reach 50% or better code coverage (`ACUMOS-2584 <https://jira.acumos.org/browse/ACUMOS-2584>`_)
* Add API to list remote catalogs to support subscribing (`ACUMOS-2575 <https://jira.acumos.org/browse/ACUMOS-2575>`_)
  API to list catalogs is /catalogs
* Refactor code to avoid duplication related to implementing listing remote catalogs.
* Documentation configuration parameters (`ACUMOS-2661 <https://jira.acumos.org/browse/ACUMOS-2661>`_)

Version 2.1.1, 2019-03-07
-------------------------
* Solution picture should be copied (`ACUMOS-2570 <https://jira.acumos.org/browse/ACUMOS-2570>`_)

Version 2.1.0, 2019-03-05
-------------------------
* Update to CDS 2.1.2

Version 2.0.1, 2019-02-26
-------------------------

* Add catalogId field in solution search selector (`ACUMOS-2285 <https://jira.acumos.org/browse/ACUMOS-2285>`_)
* Normalize configured Nexus URL to have exactly one trailing slash (`ACUMOS-2554 <https://jira.acumos.org/browse/ACUMOS-2554>`_)
* Allow server to run as unprivileged user (`ACUMOS-2551 <https://jira.acumos.org/browse/ACUMOS-2551>`_)
* Various problems found with version 2.0.0 (`ACUMOS-2570 <https://jira.acumos.org/browse/ACUMOS-2570>`_)
  - List dependency on jersey-hk2 for spring-boot
  - Instant rendered as JSON object rather than seconds since epoch
  - Seconds since epoch may parse as Integer instead of Long

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
