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

Selectors and Finding Solutions
-------------------------------

The Acumos federation gateway supports retrieving solutions from a peer
instance of Acumos.  Usually, though, what is desired is to retrieve a subset
of the solutions, and this is done by specifying a "selector" as a query
parameter on the HTTP GET from the peer federation gateway.  The value of the
selector is the Base64 encoding of a JSON object, with keys specifying
constraints on the set of solutions to be returned.  For example, to specify
that the name of the solution must be "hello," the JSON object might look like::

    {"name":"hello"}

The Base64 encoding of this is::

    eyJuYW1lIjoiaGVsbG8ifQ==

And the URL for an HTTP GET with this selector might be::

    https://example.org/solutions?selector=eyJuYW1lIjoiaGVsbG8ifQ%3D%3D

The keys supported in the selector object are:

* active

  Boolean, either true or false.  Defaults to true.  If true, only active
  solutions will be returned.  If false, only inactive solutions will be
  returned.

* catalogId

  String.  If specified, only solutions from the specified catalog will be
  returned.

* modelTypeCode

  String or array of strings.  If specified, only solutions with one of the
  specified modelTypeCodes will be returned.

* modified

  Integer.  Defaults to 1.  A timestamp specified as the number of seconds since
  January 1, 1970, 00:00:00 GMT.  Only solutions modified at or after the
  specified timestamp will be returned.

* name

  String.  If specified, only solutions with the specified name will be
  returned.

* solutionId

  String.  If specified, only the specified solution will be returned.

* toolkitTypeCode

  String or array of strings.  If specified, only solutions with one of the
  specified toolkitTypeCodes will be returned.

* tags

  String or array of strings.  If specified, only solutions that have at
  least one of the specified tags will be returned.

Note: String comparison uses an exact match.

Note: Only solutions that meet all of the specified constraints will be returned.

Note: A federation gateway can be configured with additional default values as
well as overrides of user specified values.

Note: To get "all" solutions, don't specify a selector on the HTTP request (or
specify one without any of the above keys).  Default constraints will still
be applied, so only active solutions modified after
January 1st, 1970 at 00:00:00 GMT will be returned.
