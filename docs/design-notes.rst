..  ===============LICENSE_START=======================================================
..  Acumos
..  ===================================================================================
..  Copyright (C) 2017 AT&T Intellectual Property & Tech Mahindra. All rights reserved.
..  ===================================================================================
..  This Acumos software file is distributed by AT&T and Tech Mahindra
..  under the Apache License, Version 2.0 (the "License");
..  you may not use this file except in compliance with the License.
..  You may obtain a copy of the License at
..   
..       http://www.apache.org/licenses/LICENSE-2.0
..   
..  This file is distributed on an "AS IS" BASIS,
..  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
..  See the License for the specific language governing permissions and
..  limitations under the License.
..  ===============LICENSE_END=========================================================


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
*federation interface* and we call the internal interface (towards the other components
of the same Acumos instance) the *local interface*.
The design of the gateway reflects this duality: the gateway defines/offers a set of
REST APIs on its federation interface for gateway-2-gateway (or gateway-2-adapter)
communication and another set on the local interface for component-2-gateway communication.

Federation concepts

The federation gateway behaviour is driven by the peer and subscription information provisioned
in the CDS. Through the local interface API other components can trigger gateway
behaviour, i.e. trigger interactions with peers. 
The peer information represents all other Acumos systems (or other through adapters) this system
has agreed to communicate/exchange information with. The 'handshake' procedure by which 2 systems
agreed to communicate and provision the required information can take place 'out-of-band' (email,etc
+ provisining) or 'in-band' (a combination of federation REST API and provisioning actions).

When enabling federation an Acumos system agrees to share its public, validated models (their
revisions) with its peers. (a discussion on ACL driven/selective sharing control will come here later).
Establishing a relationship does not in itself imply that any exchange of information takes place.
Information exchange (models) is driven by subscriptions provisioned in the local CDS. In Acumos every
peer is responsible for pulling from its peers the models it is interested in (such an interaction
.goes through the peers' federation gateway who controls/filters access to its local models).
A subscription towards a peer represents a subset of that peers' model set that this Acumos is interested in.
The subscription information is there to drive the behaviour of the federation gateway (who does
the actual peer polling and local provisining of the retrieved information); there is no subscription
information shared between peers. An Acumos instance can have multiple subscriptions towards another
peer. A subscription can range from one specific model to all the models a peer exposes (with any
combination of model level selection criteria in between). A subscription further specifies
options such as the frequency with each the federation gateway should check for updates, how much
model information should be retrieved every time,etc.

It is important to notice that the federation gateway mechanisms for model information exchange
does not impose an overall peer organization/deployment architecture: tree like structures, fully or sparse
connected graphs, etc are all possible.



Federation mechanisms

Before any interaction with a peer can take place the peer information needs to be provisoned
in the local CDS. A federation gateway has a dual role, as a server when responding to requests
from its peers and as a client when requesting information from them. The federation gateway
uses mutual authentication (https,tls), i.e. when a connection is established between 2 gateways
both sides need to present their certificates (signed by accepred CAs and so on). The subjectName
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

Other

At this point the federation gateway relies on only one Acumos component, the CDS.
 
