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

======================================
Federation Gateway Configuration Guide
======================================

The Acumos Federation Gateway Server is configured by setting properties using the
SPRING_APPLICATION_JSON environment variable.  See the Spring-Boot documentation
on Externalized Configuration for information on how the
SPRING_APPLICATION_JSON environment variable is parsed and on other methods for
setting configuration property values.

Configuration Properties
------------------------

Example (with syntactically valid but completely made up values)::
  export SPRING_APPLICATION_JSON='
    "federation.address": "externalname.example.com",
    "federation.server.port": 8443,
    "federation.ssl": {
      "key-store": "/somedirectory/externalname.jks",
      "key-store-password": "some value 1"
    },
    "local": {
      "address": "internalname.example.com",
      "server": {
        "port": 9443
      },
      "ssl.key-store": "classpath:internalname.jks",
      "ssl.key-store-password": "some value 2"
    },
    "cdms.client": {
      "url": "http://cdshost:8080/ccds",
      "username": "theuser",
      "password": "some value 3",
    },
    "docker": {
      "registry-url": "myregistry:10443",
      "registry-username": "myregistryuser",
      "registry-password": "some value 4",
      "registry-email": "someuser@somehost",
    },
    "nexus": {
      "url": "https://mynexus:7443/repository/myrepo",
      "username": "nexususer",
      "password": "nexuspass",
      "nexus.group-id": "myorg"
    },
    "verification.url": "http://securityserver:9999"
  }'

Note that::
  { "a.b": "x", "a.c": "y" }
and::
  { "a": { "b": "x", "c": "y" }}
are equivalent, in SPRING_APPLICATION_JSON.

federation.address
  Optional.  FQDN or IP address.

  This specifies which IP interface, on the federation host machine, listens
  for incoming requests from peers.  Defaults to listening
  on all interfaces.

federation.registration.enabled
  Optional.  Default False.

  When true, federation will accept registration requests from peers.

federation.server.port
  Required.

  This specifies which TCP/IP port, on the interface(s) specified by
  federation.address, listens for incoming requests from peers.

federation.client-auth
  Optional.  Allowed values "NEED", "WANT", "NONE".  Default "WANT".

  This specifies whether to request or require 2-way TLS authentication
  of incoming connections from peers.

federation.ssl.key-alias
  Required if key store contains multiple private keys.

  This specifies which private key/certificate pair, in the key store
  is used, by federation, to authenticate to peers.

federation.ssl.key-store
  Required.

  This specifies the path of the file containing the certificate and
  private key used, by federation, to authenticate to peers.

federation.ssl.key-store-password
  Required.

  This specifies the password for decrypting the key store file.

federation.ssl.key-store-type
  Allowed values: JKS or PKCS12.

  This specifies the format of the key store file.

federation.ssl.trust-store
  This specifies the path of the file containing the certificates of
  accepted certificate authorities for authenticating peers.

federation.ssl.trust-store-password
  Required.

  This specifies the password for decrypting the trust store file.

federation.ssl.trust-store-type
  Allowed values: JKS or PKCS12.

local.address
  Optional.  FQDN or IP address.

  This specifies which IP interface, on the federation host machine, listens
  for incoming requests from the local Acumos marketplace portal (the portal).
  Defaults to listening on all interfaces.

local.server.port
  Required.

  This specifies which TCP/IP port, on the interface(s) specified by
  federation.address, listens for incoming requests from the portal.

local.client-auth
  Optional.  Allowed values "NEED", "WANT", "NONE".  Default "WANT".

  This specifies whether to request or require 2-way TLS authentication
  of incoming connections from the portal.

local.ssl.key-alias
  Required if key store contains multiple private keys.

  This specifies which private key/certificate pair, in the key store
  is used, by federation, to authenticate to the portal.

local.ssl.key-store
  Required.

  This specifies the path of the file containing the certificate and
  private key used, by federation, to authenticate to the portal.

local.ssl.key-store-password
  Required.

  This specifies the password for decrypting the key store file.

local.ssl.key-store-type
  Allowed values: JKS or PKCS12.

  This specifies the format of the key store file.

local.ssl.trust-store
  This specifies the path of the file containing the certificates of
  accepted certificate authorities for authenticating to the portal.

local.ssl.trust-store-password
  Required.

  This specifies the password for decrypting the trust store file.

local.ssl.trust-store-type
  Allowed values: JKS or PKCS12.

  This specifies the format of the trust store file.

cdms.client.url
  Required.

  Base URL for accessing the common data service.

cdms.client.username
  Required.

  User name for authenticating to the common data service.

cdms.client.password
  Required.

  Password for authenticating to the common data service.

peer.jobchecker.interval
  Optional.  Default 400.

  The time, in seconds, between checking for changes to the set of active
  subscriptions.

docker.api-version
  Optional.

  The version of the Docker API to use when communicating with the Docker host.
  Version values should be of the form X.Y where X is the major version number
  and Y is the minor version number of the Docker API protocol.  The Docker API
  version matrix can be found
  `here. <https://docs.docker.com/develop/sdk/#api-version-matrix>`_

docker.host
  Optional.  Default unix:///var/run/docker.sock.

  The URL of the unix or IP socket for accessing the local Docker host in
  the form tcp://hostname:port or unix://path.  The local Docker host is used
  to pull and push Docker image artifacts from the Docker repository and to
  serialize and deserialize those artifacts for transmission between peers.

docker.docker-tls-verify
  Optional.  Default False.

  If True, use TLS encryption when connecting to the local Docker host

docker.docker-cert-path
  Required when docker.docker-tls-verify is True.

  If the connection to the local Docker host is encrypted, using TLS, the path
  the directory for the PEM files containing the trust store (ca.pem), private
  private key (key.pem), and certificate (cert.pem) used by federation's Docker
  client to connect to the local Docker host.

docker.docker-config
  Optional.  Default $HOME/.docker

  Path to the directory containing the user's Docker configuration file
  (config.json).

docker.registry-url
  Required.

  The hostport for accessing the Docker registry in the form hostname:port.
  The registry is used to store Docker image artifacts, in response to
  "docker pull" and "docker push" requests sent to the Docker host.

docker.registry-username
  Required.

  The username for authenticating to the Docker registry for pushing images.

docker.registry-password
  Required.

  The password for authenticating to the Docker registry for pushing images.

docker.registry-email
  The email address associated with the username and password for
  authenticating to the Docker Registry.

nexus.url
  Required.

  The URL for the Nexus repository used to store (non-Docker) artifacts and
  documents, of the form https://host:port/repository/reponame/.

nexus.username
  Required.

  The user name for authenticating to the nexus server.

nexus.password
  Required.

  The password for authenticating to the nexus server.

nexus.group-id
  Required.

  Per Acumos instance component of the path prefix within the Nexus repository.

nexus.name-separator
  Optional.  Default ".".

  Separator between components of the path prefix within the Nexus repository.
  The prefix is of the form groupid separator solutionid separator revisionid.

verification.url
  Required.

  URL for the Acumos security-verification server used to perform security
  verification scans on solution revisions.

=========================================
Federation Gateway Certificate Generation
=========================================

This document explains the steps required to configure two Acumos
instances to be peers so that they can communicate via their
Federation Gateway components.  Gateways use certificates for mutual
SSL authentication.

An overview of the general process is here:
`Mutual SSL Authentication
<https://www.codeproject.com/Articles/326574/An-Introduction-to-Mutual-SSL-Authentication/>`_

Assistance with the detailed process is here:
`How to setup your own CA with OpenSSL
<https://gist.github.com/Soarez/9688998>`_

Background
----------

The asymmetric encryption technique used here is based on two keys: a
message that gets encrypted with one key can be decrypted with the
other key. We call one the private key and the other the public key,
because when used in two-party communication we keep one (the private
key) and we give one away (the public key). The one we give away needs
to be certified; i.e., others need to be sure the key can be
trusted. For that we send the public key to a certificate authority
(CA) in the form of a certificate signing request (CSR).  The CA signs
this (creates some hash) with their private key. Then everyone who has
the CA public key (who trusts the CA) will accept our signed-by-the-CA
public key, and this chain of trust can go on recursively.  The result
is that our public key gets packed in a certificate signed by that CA
and now we can use it/share it with others.

Each peer gateway is provisioned with a PKCS12 key store holding a
private key and a certificate, which is the matching public key signed
by a certificate authority.  The mutual authentication process
proceeds as follows.  A federation peer C (playing the client role in
this example) attempts a connection to peer S (playing the server role
in this example).  To establish a secure communication channel, peer S
first sends its certificate.  The receipt by C of the S certificate
allows C to verify S's identity.  After this step is successful, peer
S asks peer C for C's certificate.  Peer S then checks the identity of
peer C based on the certificate.  If that succeeds, the channel is
secure.  After this TLS handshake process has completed, peer S
searches its peer repository (internal configuration) for the fully
qualified host name from C's certificate, and allows the exchange of
information if a match is found.


Overview
--------

The following tasks are required for configuration of each Acumos host:

* Create a certificate signing request
* Obtain a signed certificate, either by purchasing it or signing the requset with a local authority
* Install the signed certificate in the gateway deployment environment
* Configure the gateway using the Portal administration interface.


Create Certificates
-------------------

These instructions create appropriate certificates suitable for
development and testing environments ONLY, not for production
environments.  To avoid the delay and expense of purchasing a signed
certificate from a well-known certificate authority, this creates a
new certificate authority (CA) and adds the appropriate certificate to
a trust store.

These following instructions use the ``openssl`` command-line tool,
which is available on Linux hosts.  This scenario was developed using
Ubuntu version 16.04.  The instructions use shell-style variables
(e.g., ``$VAR``) to indicate where a value must be supplied and
reused.

Step 1: Determine the fully qualified domain name of the peer (FQDN)
and choose a password (6 characters or more). Store these values in
shell variables ``ACUMOS_HOST`` and ``ACUMOS_PASS`` for use in the
commands below.  For example::

  export ACUMOS_HOST="myserver.mymodels.org"
  export ACUMOS_PASS="mykey123456"

Step 2: Because a new certificate authorithy (CA) will be created
here, openssl requires a configuration file ``openssl.cnf``.  Create
this file using the template below, and in the ``[alt_names]``
section replace the string ``<acumos-host>`` with the FQDN you chose
above.

Step 3: Create the Acumos CA private key::

  openssl genrsa -des3 -out acumosCA.key -passout pass:$ACUMOS_PASS 4096

Step 4: Create the Acumos CA certificate. You may wish to use
different values (i.e., not "Unspecified") in this command, just be
consistent in later commands::

  openssl req -x509 -new -nodes -key acumosCA.key -sha256 -days 1024 \
    -config openssl.cnf -out acumosCA.crt -passin pass:$ACUMOS_PASS \
    -subj "/C=US/ST=Unspecified/L=Unspecified/O=Acumos/OU=Acumos/CN=$ACUMOS_HOST"

Step 5: Create a JKS-format truststore with the Acumos CA certificate::

  keytool -import -file acumosCA.crt -alias acumosCA -keypass $ACUMOS_PASS \
      -keystore acumosTrustStore.jks -storepass $ACUMOS_PASS -noprompt

The recommended practice here is to import the self-signed Acumos CA
certificate into an existing trust store. For example you can extend
the file "cacerts" that is included with a Java Runtime Engine (JRE)
distribution below directory "jre/lib/security" which usually uses the
password "changeit".

Step 6: Create the server private key::

  openssl genrsa -out acumos.key -passout pass:$ACUMOS_PASS 4096

Step 7: Create a certificate signing request (CSR) for your FQDN.
Please note the C, ST, L, O, OU and CN key-value pairs must match what
was used above::

  openssl req -new -key acumos.key -passin pass:$ACUMOS_PASS -out acumos.csr \
    -subj "/C=US/ST=Unspecified/L=Unspecified/O=Acumos/OU=Acumos/CN=$ACUMOS_HOST"

Step 8: Sign the CSR with the Acumos CA certificate to yield a server certificate::

  openssl ca -config openssl.cnf -passin pass:$ACUMOS_PASS -in acumos.csr -out acumos.crt

Step 9: Copy the server private key and certificate to a plain text
file ``acumos.txt``. The private key should appear first, followed by
the certificate. The finished file should have this structure::

  -----BEGIN RSA PRIVATE KEY-----
  (Private Key: acumos.key contents)
  -----END RSA PRIVATE KEY-----
  -----BEGIN CERTIFICATE-----
  (SSL certificate: acumos.crt contents)
  -----END CERTIFICATE-----

Step 10: Create a PKCS12 format keystore with the server key and certificate::

  openssl pkcs12 -export -in acumos.txt -passout pass:$ACUMOS_PASS -out acumos.pkcs12

Step 11: Copy the JKS and PKCS12 files to the machine where the
federation component runs and configure them:

* Enter the path to the JKS file in key ``trust-store``
* Enter the password for the JKS file in key ``trust-store-password``
* Enter the path to the PKCS12 file in key ``key-store``
* Enter the password for the  PKCS12 file in key ``key-store-password``
* Enter the key store type in key ``key-store-type`` with value ``PKCS12``


Final Checklist
---------------

These are the prerequisites for Acumos instance A (``hostA.name.org``)
to pull models from its Acumos peer B (``hostB.name.org``):

#. Federation gateways are running on both instances
#. Gateway A has a PKCS12 file containing a certificate for ``hostA.name.org`` and signed by authority CA-1
#. Gateway A deployment configuration has the path to the PKCS12 file in key ``federation.ssl.key-store``
#. Gateway A has a trust store file that includes the signing certificate for authority CA-2
#. Gateway A deployment configuration has the path to the trust store file in key ``federation.ssl.trust-store``
#. Gateway A is configured with peer B's FQDN (``hostB.name.org``) and public gateway URL (``https://hostB.name.org:12345``)
#. Gateway B has with a PCKS12 file containing a certificate for ``hostB.name.org`` and signed by authority CA-2
#. Gateway B deployment configuration has the path to the PKCS12 file in key ``federation.ssl.key-store``
#. Gateway B has a trust store file that includes the signing certificate for authority CA-1
#. Gateway B deployment configuration has the path to the trust store file in key ``federation.ssl.trust-store``
#. Gateway B is configured with peer A's FQDN (``hostA.name.org``) and public gateway URL (``https://hostA.name.org:54321``)

Please note that a PKCS12 file is a store, i.e. it contains private
key and associated certificates in a binary form (and not just
certificates).

Troubleshooting
---------------

Inspect the certificate advertised by your server using this command::

  openssl s_client -connect yourserver.yourmodels.org:9084

Look carefully at the "Certificate chain" section.  In case of error
you may see a message like this::

  Verify return code: 21 (unable to verify the first certificate)

For advanced troubleshooting, use the following steps to extract
certificates and keys to test connections manually.

Extract the CA certificate created above in PEM format::

  keytool -export -alias acumos -file acumos-ca.crt -keystore acumosTrustStore.jks
  openssl x509 -inform der -in acumos-ca.crt -out acumos-ca.pem

Extract the signed certificate for the client host attempting the
connection in PEM format::

  openssl pkcs12 -in acumos.p12 -clcerts -nokeys -out acumos.pem

Look at the signed certificate details, for example the expiration date::

  openssl x509 -in acumos.pem -text -noout

Extract the private key for the client host attempting the connection::

  openssl pkcs12 -in acumos.p12 -nocerts -out acumos.key

Next run the following command to test the certificates used to
establish a connection to remote peer ``yourserver.yourmodels.org`` at
port 9084 from server ``myserver.mymodels.org``. The certificate files
used below were created by the procedure above for host
``myserver.mymodels.org``::

  openssl s_client -connect yourserver.yourmodels.org:9084 -cert acumos.pem -key acumos.key -CAfile acumos-ca.pem

You must enter the key phrase, then the connection attempt can begin.

Finally use the command-line tool ``curl`` to test whether the remote
host is ready to accept connections.  This command uses the ``-k``
option to allow insecure connections, so the certificate authority is
not required here::

  curl -vk --cert acumos.pem:mykey123456 --key acumos.key https://yourserver.yourmodels.org:9084/ping


Template openssl.cnf
--------------------

::

  # This is a customized OpenSSL configuration file. Commented out sections below
  # are included for testing/clarity for now, and will be removed later once the
  # specific comments that need to be retained for clarity are determined.
  #

  # This definition stops the following lines choking if HOME isn't
  # defined.
  HOME                    = .
  RANDFILE                = $ENV::HOME/.rnd

  # Extra OBJECT IDENTIFIER info:
  #oid_file               = $ENV::HOME/.oid
  oid_section             = new_oids

  # To use this configuration file with the "-extfile" option of the
  # "openssl x509" utility, name here the section containing the
  # X.509v3 extensions to use:
  extensions            = v3_req
  # (Alternatively, use a configuration file that has only
  # X.509v3 extensions in its main [= default] section.)

  [ new_oids ]

  # We can add new OIDs in here for use by 'ca', 'req' and 'ts'.
  # Add a simple OID like this:
  # testoid1=1.2.3.4
  # Or use config file substitution like this:
  # testoid2=${testoid1}.5.6

  # Policies used by the TSA examples.
  tsa_policy1 = 1.2.3.4.1
  tsa_policy2 = 1.2.3.4.5.6
  tsa_policy3 = 1.2.3.4.5.7

  ####################################################################
  [ ca ]
  default_ca      = CA_default            # The default ca section

  ####################################################################
  [ CA_default ]

  dir             = .                     # Where everything is kept
  certs           = $dir/certs            # Where the issued certs are kept
  crl_dir         = $dir/crl              # Where the issued crl are kept
  database        = $dir/index.txt        # database index file.
  #unique_subject = no                    # Set to 'no' to allow creation of
					  # several ctificates with same subject.
  new_certs_dir   = $dir/newcerts         # default place for new certs.

  certificate     = $dir/certs/acumos_ca.crt     # The CA certificate
  serial          = $dir/serial           # The current serial number
  crlnumber       = $dir/crlnumber        # the current crl number
					  # must be commented out to leave a V1 CRL
  crl             = $dir/crl.pem          # The current CRL
  private_key     = $dir/private/acumos_ca.key   # The private key
  RANDFILE        = $dir/private/.rand    # private random number file

  x509_extensions = usr_cert              # The extentions to add to the cert

  # Comment out the following two lines for the "traditional"
  # (and highly broken) format.
  name_opt        = ca_default            # Subject Name options
  cert_opt        = ca_default            # Certificate field options

  # Extension copying option: use with caution.
  copy_extensions = copy

  # Extensions to add to a CRL. Note: Netscape communicator chokes on V2 CRLs
  # so this is commented out by default to leave a V1 CRL.
  # crlnumber must also be commented out to leave a V1 CRL.
  # crl_extensions        = crl_ext

  default_days    = 365                   # how long to certify for
  default_crl_days= 30                    # how long before next CRL
  default_md      = default               # use public key default MD
  preserve        = no                    # keep passed DN ordering

  # A few difference way of specifying how similar the request should look
  # For type CA, the listed attributes must be the same, and the optional
  # and supplied fields are just that :-)
  policy          = policy_match

  # For the CA policy
  [ policy_match ]
  countryName             = match
  stateOrProvinceName     = match
  organizationName        = match
  organizationalUnitName  = optional
  commonName              = supplied
  emailAddress            = optional

  # For the 'anything' policy
  # At this point in time, you must list all acceptable 'object'
  # types.
  [ policy_anything ]
  countryName             = optional
  stateOrProvinceName     = optional
  localityName            = optional
  organizationName        = optional
  organizationalUnitName  = optional
  commonName              = supplied
  emailAddress            = optional

  ####################################################################
  [ req ]
  default_bits            = 2048
  default_keyfile         = privkey.pem
  distinguished_name      = req_distinguished_name
  attributes              = req_attributes
  x509_extensions = v3_ca # The extentions to add to the self signed cert

  # Passwords for private keys if not present they will be prompted for
  # input_password = secret
  # output_password = secret

  # This sets a mask for permitted string types. There are several options.
  # default: PrintableString, T61String, BMPString.
  # pkix   : PrintableString, BMPString (PKIX recommendation before 2004)
  # utf8only: only UTF8Strings (PKIX recommendation after 2004).
  # nombstr : PrintableString, T61String (no BMPStrings or UTF8Strings).
  # MASK:XXXX a literal mask value.
  # WARNING: ancient versions of Netscape crash on BMPStrings or UTF8Strings.
  string_mask = utf8only

  req_extensions = v3_req # The extensions to add to a certificate request

  [ req_distinguished_name ]
  countryName                     = Country Name (2 letter code)
  countryName_default             = US
  countryName_min                 = 2
  countryName_max                 = 2

  stateOrProvinceName             = State or Province Name (full name)
  stateOrProvinceName_default     = Some-State

  localityName                    = Locality Name (eg, city)

  0.organizationName              = Organization Name (eg, company)
  0.organizationName_default      = Internet Widgits Pty Ltd

  # we can do this but it is not needed normally :-)
  #1.organizationName             = Second Organization Name (eg, company)
  #1.organizationName_default     = World Wide Web Pty Ltd

  organizationalUnitName          = Organizational Unit Name (eg, section)
  #organizationalUnitName_default =

  commonName                      = Common Name (e.g. server FQDN or YOUR name)
  commonName_max                  = 64

  emailAddress                    = Email Address
  emailAddress_max                = 64

  # SET-ex3                       = SET extension number 3

  [ req_attributes ]
  challengePassword               = A challenge password
  challengePassword_min           = 4
  challengePassword_max           = 20

  unstructuredName                = An optional company name

  [ usr_cert ]

  # These extensions are added when 'ca' signs a request.

  # This goes against PKIX guidelines but some CAs do it and some software
  # requires this to avoid interpreting an end user certificate as a CA.

  basicConstraints=CA:FALSE

  # Here are some examples of the usage of nsCertType. If it is omitted
  # the certificate can be used for anything *except* object signing.

  # This is OK for an SSL server.
  # nsCertType                    = server

  # For an object signing certificate this would be used.
  # nsCertType = objsign

  # For normal client use this is typical
  # nsCertType = client, email

  # and for everything including object signing:
  # nsCertType = client, email, objsign

  # This is typical in keyUsage for a client certificate.
  # keyUsage = nonRepudiation, digitalSignature, keyEncipherment

  # This will be displayed in Netscape's comment listbox.
  nsComment                       = "OpenSSL Generated Certificate"

  # PKIX recommendations harmless if included in all certificates.
  subjectKeyIdentifier=hash
  authorityKeyIdentifier=keyid,issuer

  # This stuff is for subjectAltName and issuerAltname.
  # Import the email address.
  # subjectAltName=email:copy
  # An alternative to produce certificates that aren't
  # deprecated according to PKIX.
  # subjectAltName=email:move

  # Copy subject details
  # issuerAltName=issuer:copy

  #nsCaRevocationUrl              = http://www.domain.dom/ca-crl.pem
  #nsBaseUrl
  #nsRevocationUrl
  #nsRenewalUrl
  #nsCaPolicyUrl
  #nsSslServerName

  # This is required for TSA certificates.
  # extendedKeyUsage = critical,timeStamping

  [ v3_req ]

  # Extensions to add to a certificate request

  basicConstraints = CA:FALSE
  keyUsage = nonRepudiation, digitalSignature, keyEncipherment
  subjectAltName = @alt_names
  # Included these for openssl x509 -req -extfile
  subjectKeyIdentifier=hash
  authorityKeyIdentifier=keyid,issuer

  [ alt_names ]

  DNS.1 = <acumos-host>
  # federation-service: for portal-be access to federation local port via expose
  DNS.2 = federation-service

  [ v3_ca ]


  # Extensions for a typical CA


  # PKIX recommendation.

  subjectKeyIdentifier=hash

  authorityKeyIdentifier=keyid:always,issuer

  # This is what PKIX recommends but some broken software chokes on critical
  # extensions.
  #basicConstraints = critical,CA:true
  # So we do this instead.
  basicConstraints = CA:true

  # Key usage: this is typical for a CA certificate. However since it will
  # prevent it being used as an test self-signed certificate it is best
  # left out by default.
  # keyUsage = cRLSign, keyCertSign

  # Some might want this also
  # nsCertType = sslCA, emailCA

  # Include email address in subject alt name: another PKIX recommendation
  # subjectAltName=email:copy
  # Copy issuer details
  # issuerAltName=issuer:copy

  # DER hex encoding of an extension: beware experts only!
  # obj=DER:02:03
  # Where 'obj' is a standard or added object
  # You can even override a supported extension:
  # basicConstraints= critical, DER:30:03:01:01:FF

  [ crl_ext ]

  # CRL extensions.
  # Only issuerAltName and authorityKeyIdentifier make any sense in a CRL.

  # issuerAltName=issuer:copy
  authorityKeyIdentifier=keyid:always

  [ proxy_cert_ext ]
  # These extensions should be added when creating a proxy certificate

  # This goes against PKIX guidelines but some CAs do it and some software
  # requires this to avoid interpreting an end user certificate as a CA.

  basicConstraints=CA:FALSE

  # Here are some examples of the usage of nsCertType. If it is omitted
  # the certificate can be used for anything *except* object signing.

  # This is OK for an SSL server.
  # nsCertType                    = server

  # For an object signing certificate this would be used.
  # nsCertType = objsign

  # For normal client use this is typical
  # nsCertType = client, email

  # and for everything including object signing:
  # nsCertType = client, email, objsign

  # This is typical in keyUsage for a client certificate.
  # keyUsage = nonRepudiation, digitalSignature, keyEncipherment

  # This will be displayed in Netscape's comment listbox.
  nsComment                       = "OpenSSL Generated Certificate"

  # PKIX recommendations harmless if included in all certificates.
  subjectKeyIdentifier=hash
  authorityKeyIdentifier=keyid,issuer

  # This stuff is for subjectAltName and issuerAltname.
  # Import the email address.
  # subjectAltName=email:copy
  # An alternative to produce certificates that aren't
  # deprecated according to PKIX.
  # subjectAltName=email:move

  # Copy subject details
  # issuerAltName=issuer:copy

  #nsCaRevocationUrl              = http://www.domain.dom/ca-crl.pem
  #nsBaseUrl
  #nsRevocationUrl
  #nsRenewalUrl
  #nsCaPolicyUrl
  #nsSslServerName

  # This really needs to be in place for it to be a proxy certificate.
  proxyCertInfo=critical,language:id-ppl-anyLanguage,pathlen:3,policy:foo

  ####################################################################
  [ tsa ]

  default_tsa = tsa_config1       # the default TSA section

  [ tsa_config1 ]

  # These are used by the TSA reply generation only.
  dir             = ./demoCA              # TSA root directory
  serial          = $dir/tsaserial        # The current serial number (mandatory)
  crypto_device   = builtin               # OpenSSL engine to use for signing
  signer_cert     = $dir/tsacert.pem      # The TSA signing certificate
					  # (optional)
  certs           = $dir/cacert.pem       # Certificate chain to include in reply
					  # (optional)
  signer_key      = $dir/private/tsakey.pem # The TSA private key (optional)

  default_policy  = tsa_policy1           # Policy if request did not specify it
					  # (optional)
  other_policies  = tsa_policy2, tsa_policy3      # acceptable policies (optional)
  digests         = md5, sha1             # Acceptable message digests (mandatory)
  accuracy        = secs:1, millisecs:500, microsecs:100  # (optional)
  clock_precision_digits  = 0     # number of digits after dot. (optional)
  ordering                = yes   # Is ordering defined for timestamps?
				  # (optional, default: no)
  tsa_name                = yes   # Must the TSA name be included in the reply?
				  # (optional, default: no)
  ess_cert_id_chain       = no    # Must the ESS cert id chain be included?
				  # (optional, default: no
