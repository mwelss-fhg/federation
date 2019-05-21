/*-
 * ===============LICENSE_START=======================================================
 * Acumos
 * ===================================================================================
 * Copyright (C) 2017-2019 AT&T Intellectual Property & Tech Mahindra. All rights reserved.
 * ===================================================================================
 * This Acumos software file is distributed by AT&T and Tech Mahindra
 * under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ===============LICENSE_END=========================================================
 */
package org.acumos.federation.gateway;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import javax.naming.ldap.LdapName;
import javax.naming.InvalidNameException;
import javax.naming.ldap.Rdn;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import org.acumos.cds.domain.MLPPeer;

import org.acumos.federation.client.config.TlsConfig;
import org.acumos.federation.client.FederationClient;

/**
 * Service bean implementing authentication and peer identification services
 * on requests to the Federation Gateway.
 */
@Configuration
@EnableWebSecurity
public class Security extends WebSecurityConfigurerAdapter {
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	/**
	 * The role indicating a peer is allowed to register.
	 */
	public static final String ROLE_REGISTER = "ROLE_REGISTRATION";
	/**
	 * The role indicating a peer is allowed to cancel their registration.
	 */
	public static final String ROLE_UNREGISTER = "ROLE_UNREGISTRATION";
	/**
	 * The role indicating a peer has normal access to the gateway.
	 */
	public static final String ROLE_PEER = "ROLE_PEER";
	/**
	 * The role indicating a peer has trusted access to the gateway.
	 */
	public static final String ROLE_PARTNER = "ROLE_PARTNER";
	/**
	 * The role for access from within Acumos itself.
	 */
	public static final String ROLE_INTERNAL = "ROLE_INTERNAL";

	/**
	 * Fake peer status code for errors while determining peer status
	 */
	private static final String PSC_UNKNOWN = "?";

	/**
	 * Principal specifying identity and permissions of a user.
	 */
	public static class Peer extends User {
		private String peerId;
		private boolean local;

		/**
		 * Get the ID of the peer
		 *
		 * @return The peer ID.
		 */
		public String getPeerId() {
			return peerId;
		}

		/**
		 * Is the peer local?
		 *
		 * @return Whether the peer is local.
		 */
		public boolean isLocal() {
			return local;
		}

		/**
		 * Specify identity and permissions of a user.
		 *
		 * @param username The X.509 certificate subject DN of the user.
		 * @param perms The roles the user has.
		 * @param peerId The peer ID of the user.
		 * @param local Whether the peer is local.
		 */
		public Peer(String username, List<GrantedAuthority> perms, String peerId, boolean local) {
			super(username, "", perms);
			this.peerId = peerId;
			this.local = local;
		}
	}

	@Autowired
	private PeerService peerService;

	@Autowired
	private FederationConfig federation;

	private static final String[] SWAGGER_URLS = {
		"/v2/api-docs",
		"/v2/api-docs/**",
		"/swagger-resources/**",
		"/swagger-ui.html",
		"/error",
		"/webjars/**"
	};

	private static List<GrantedAuthority> ladd(List<GrantedAuthority> parent, String plus) {
		List<GrantedAuthority> ret = new ArrayList<>(parent);
		ret.add(new SimpleGrantedAuthority(plus));
		return ret;
	}

	private static String getLdapNameField(LdapName name, String field) {
		for (Rdn rdn: name.getRdns()) {
			if (field.equalsIgnoreCase(rdn.getType())) {
				return rdn.getValue().toString();
			}
		}
		return null;
	}

	private static final List<GrantedAuthority> anon = Collections.emptyList();
	private static final List<GrantedAuthority> unknown = ladd(anon, ROLE_REGISTER);
	private static final List<GrantedAuthority> inactive = ladd(unknown, ROLE_UNREGISTER);
	private static final List<GrantedAuthority> peer = ladd(inactive, ROLE_PEER);
	private static final List<GrantedAuthority> partner = ladd(peer, ROLE_PARTNER);
	private static final List<GrantedAuthority> self = ladd(partner, ROLE_INTERNAL);

	/**
	 * Get the peer ID of the user for the current request.
	 *
	 * @return The peer ID.
	 */
	public static String getCurrentPeerId() {
		return ((Peer)SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getPeerId();
	}

	/**
	 * Is the peer of the user for the current request local?
	 *
	 * @return Whether the peer is local.
	 */
	public static boolean isCurrentPeerLocal() {
		return ((Peer)SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isLocal();
	}

	/**
	 * Generate an MLPPeer from the current user's subject DN.
	 */
	public static MLPPeer getCertificatePeer() {
		try {
			String subject = ((Peer)SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUsername();
			LdapName lname = new LdapName(subject);
			String cn = getLdapNameField(lname, "CN");
			String email = getLdapNameField(lname, "EMAILADDRESS");
			MLPPeer ret = new MLPPeer(cn, cn, "https://" + cn, false, false, email, FederationClient.PSC_REQUESTED);
			ret.setDescription(subject);
			return ret;
		} catch (InvalidNameException ine) {
			return null;
		}
	}

	private static InputStream openResource(String source) throws IOException {
		try {
			return (new DefaultResourceLoader()).getResource(source).getURL().openStream();
		} catch (FileNotFoundException fnfe) {
			return new FileInputStream(source);
		}
	}

	private MLPPeer myself;

	/**
	 * Get the full peer information for the Federation Gateway, itself.
	 */
	public MLPPeer getSelf() {
		if (myself == null) {
			initSelf();
		}
		return myself;
	}

	private void initSelf() {
		try {
			TlsConfig tls = federation.getSsl();
			try (InputStream is = openResource(tls.getKeyStore())) {
				KeyStore ks = KeyStore.getInstance(tls.getKeyStoreType());
				ks.load(is, tls.getKeyStorePassword().toCharArray());
				String alias = tls.getKeyAlias();
				if (alias == null) {
					Enumeration<String> aliases = ks.aliases();
					while (aliases.hasMoreElements()) {
						alias = aliases.nextElement();
						if (ks.entryInstanceOf(alias, KeyStore.PrivateKeyEntry.class)) {
							break;
						}
					}
				}
				myself = peerService.getSelf(getLdapNameField(new LdapName(((X509Certificate)ks.getCertificate(alias)).getSubjectX500Principal().getName()), "CN"));
			}
		} catch (Exception e) {
			myself = new MLPPeer();
			myself.setStatusCode(PSC_UNKNOWN);
			log.error("Cannot determine 'self' peer", e);
		}
	}

	@Override
	public void configure(WebSecurity web) throws Exception {
		web.ignoring().antMatchers(SWAGGER_URLS);
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.csrf().disable();
		http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
		http.authorizeRequests().anyRequest().authenticated()
		    .and().exceptionHandling().accessDeniedHandler((request, response, exception) -> response.setStatus(HttpServletResponse.SC_FORBIDDEN));
		http.x509().subjectPrincipalRegex("(.*)").userDetailsService(subject -> {
			String peerId;
			boolean local = false;
			List<GrantedAuthority> perms = anon;
			try {
				LdapName lname = new LdapName(subject);
				String cn = getLdapNameField(lname, "CN");
				MLPPeer mlppeer = peerService.getPeerBySubject(cn);
				if (mlppeer == null) {
					peerId = null;
					perms = unknown;
				} else {
					peerId = mlppeer.getPeerId();
					local = mlppeer.isLocal();
					perms = inactive;
					if (FederationClient.PSC_ACTIVE.equals(mlppeer.getStatusCode())) {
						perms = peer;
						if (mlppeer.isSelf()) {
							perms = self;
						}
					}
				}
			} catch (InvalidNameException ine) {
				peerId = null;
			}
			return new Peer(subject, perms, peerId, local);
		});
	}
}
