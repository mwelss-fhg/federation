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

.. _selecting:

Selectors and Subscriptions
-------------------------------

The Acumos federation gateway supports polling other Acumos instances for
solutions using a subscription mechanism.  This subscription contains a
selector specifying which catalogs should be imported.

The form of the selector value can be:

    { "catalogId": "some-catalog-id" }

or:

    { "catalogId": [ "first-catalog-id", "second-catalog-id", ... ] }

where a catalog ID is the UUID of the catalog: something like
70c19e97-b37d-4738-b363-2d352b2d0f05.
