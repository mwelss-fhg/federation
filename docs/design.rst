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
Federation Gateway Design Notes
===============================

The federation gateway is an optional component of an Acumos system whose role
is to facilitate communication with other Acumos systems (i.e. with their gateways)
or compatible systems (through adapters). Its role is to facilitate the exchange
of models and their related information between Acumos instances.
The federation gateway occupies the borderline of an Acumos system, from a logical
and deployment perspective. From a logical perspective it is the point of control
for the flow of model information in and out of an Acumos instance. From a deployment
perspective (within an enterprise environment), the federation gateway will be deployed
at the edge of the network (DMZ) with communication interfaces towards the enterprise
network (where the rest of the Acumos instance components are deployed) and towards
the outside world (where other Acumos instances are deployed).

We call the external interface (towards the gateways of other Acumos instances) the
*federation interface* (or public interface) and we call the internal interface (towards
the other components of the same Acumos instance) the *local interface* (or private interface).
The design of the gateway reflects this duality: the gateway defines/offers a set of
REST APIs on its federation interface for gateway-to-gateway (or gateway-to-adapter)
communication and another set on the local interface for component-to-gateway communication.


The public "E5" interface
-------------------------

The federation (public) interface is also known in the Acumos project as the E5 interface.
This is a public, REST-based API specification: any system that decides to federate needs to implement it.
This interface assumes a pull-based mechanism. 
As such, only the ‘server’ side is defined by E5.

The client side is based on a set of subscriptions, where each subscription defines a set of solutions
the client is interested in, through a selector (see :ref:`selecting`), and employs periodic polling to detect new material.
This interface defines no shared state, nothing to synchronize; all responsibility resides with the interested party.
Requires a pre-provisioned peer on the server side, and uses both client and server authentication (CA based),
principal to certificate matching.


The private interface
---------------------

The private (local) interface is system specific, because a localized mapping of information must be done.
In Acumos, this includes interactions with the Common Data Service and Portal components and represents
is a one-to-one mapping of solution information and related artifacts.
In ONAP, this interacts with the SDC component based on its REST API and
requires additional processing, for example transforming an Acumos artifact into a set of SDC Asset Artifacts.


Federation concepts
-------------------

Peer
~~~~

A federation peer is another system supporting the E5 interface; i.e., another Acumos gateway or an adapter for another system.
Peer information is provisioned in CDS.

Handshake
~~~~~~~~~

Establishment of a peer relationship (known as a handshake) is done by out-of-band information exchange
to obtain and share certificates.  Site administrators with adequate permissions exchange required information: 
the E5 REST endpoint coordinates and the expected principal info.  They then proceed with local peer provisioning
through the Acumos Portal federation administration page.

Subscriptions
~~~~~~~~~~~~~

A federation subscription defines which models an Acumos system is interested in importing from a peer.
The subscription is primarily a selector over a peer’s catalog, and can support different scopes,
ranging from pinpointing one particular solution to all available solutions.
Subscriptions are subject to policies in the peer regarding which models are exposed to whom.
Current options on a subscription include the refresh period, 
and full (copy everything) vs. by reference (no artifact content is transferred).

Federating models
~~~~~~~~~~~~~~~~~

The federation gateway behavior is driven by the peer and subscription information provisioned
in the CDS. Through the local interface API other components can trigger gateway
behavior, i.e. trigger interactions with peers. 
The peer information represents all other Acumos systems (or other through adapters) this system
has agreed to communicate/exchange information with. The 'handshake' procedure by which two systems
agree to communicate and provision the required information can take place 'out-of-band' (email etc.
plus provisioning) or 'in-band' (a combination of federation REST API and provisioning actions).

When enabling federation an Acumos system agrees to share its public, validated models (their
revisions) with its peers.
(A discussion on ACL driven/selective sharing control will come here later.)
Establishing a relationship does not in itself imply that any exchange of information takes place.
Information exchange of (models is driven by subscriptions provisioned in the local CDS.
In Acumos every peer is responsible for pulling from its peers the models it is interested in
(such an interaction goes through the peer's federation gateway which controls/filters access to its local models).
A subscription towards a peer represents a subset of that peer's model set that this Acumos is interested in.
The subscription information is there to drive the behavior of the federation gateway (which does
the actual peer polling and local provisioning of the retrieved information); no subscription
information is shared between peers. An Acumos instance can have multiple subscriptions towards another
peer. A subscription can range from one specific model to all the models a peer exposes (with any
combination of model level selection criteria in between). A subscription further specifies
options such as the frequency with each the federation gateway should check for updates, how much
model information should be retrieved every time,etc.

It is important to notice that the federation gateway mechanisms for model information exchange
does not impose an overall peer organization/deployment architecture: tree like structures, fully or sparse
connected graphs, etc are all possible.


Federation mechanisms
---------------------

Before any interaction with a peer can take place the peer information needs to be provisioned
in the local CDS. A federation gateway has a dual role, as a server when responding to requests
from its peers and as a client when requesting information from them. The federation gateway
uses mutual authentication (https, tls), i.e. when a connection is established between two gateways
both sides need to present their certificates (signed by accepted CAs and so on). The subjectName
entry in a certificate received from a peer serves to identify the peer against the locally (CDS)
provisioned peer collection (the are no passwords or other credentials provisioned/exchanged).

The gateway periodically processes the list of locally provisioned peers; where subscriptions
towards a peer are found they are assigned to tasks who will query the peer with the given
subscription selector. Each resulting model will be compared against locally available
model information (in CDS) and new model/new revisions+artifacts will be fetched and provisioned.

In addition to the model information exchange APIs the federation gateway offers APIs for:

- status information (ping)
- in-band registration
- peer information sharing


Dependencies
------------

At this point the federation gateway relies on only one Acumos component, the Common Data Service.
