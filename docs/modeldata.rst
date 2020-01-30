.. ===============LICENSE_START=======================================================
.. Acumos CC-BY-4.0
.. ===================================================================================
.. Copyright (C) 2017 AT&T Intellectual Property & Tech Mahindra. All rights reserved.
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

======================
Model Data Admin Guide
======================

Overview
--------

The Model data api allows model data such as parameters to flow from a running model in
a subscriber's instance of Acumos to a supplier's instance of Acumos. In addition to 
the federation gateway /peer/{peerId}/modeldata api we must connect logstash to send 
the updated model parameters.

Example Model Data Parameters
-----------------------------

.. code-block:: javascript

    {
        "@version": "1",
        "@timestamp": "2020-02-17T21:21:09.338Z",
        "tags": [
            "acumos-model-param-logs",
            "beats_input_raw_event"
        ],
        "model": {
            "userId": "12345678-abcd-90ab-cdef-1234567890ab",
            "revisionId": "1c0a4ea4-e822-4fb3-bef1-11f92958c315",
            "solutionId": "149ea34c-44fc-4329-8189-52d3ae523a15"
        },
        "value": {
            "B": "121",
            "C": "270",
            "A": "601"
        }
    }



Example Log stash configuration
-------------------------------

Log stash has 2 important configuration changes

1. http output plugin sending logs from model runner to gateway service

.. code-block:: javascript

  output
     ...
     if "acumos-model-param-logs" in [tags] {
      elasticsearch {
        hosts => ["elasticsearch:9200"]
        index => "acumos-model-param-logs"
      }

      http {
        keystore => "/app/certs/acumos-keystore.p12"
        keystore_password => "[KEYSTORE_PASSWORD]"
        keystore_type => "PKCS12"
        truststore => "/app/certs/acumos-truststore.jks"
        truststore_password => "[TRUSTSTORE_PASSWORD]"
        retry_failed => false
        http_method => "post"
        url => "https://[GATEWAY_SERVICE]:[GATEWAY_PORT]/peer/USE_SOLUTION_SOURCE/modeldata"
      }


2. http input plugin for accepting log entries from federation service.

.. code-block:: javascript

      input
      ...
        http {
            port => 5043
        }

