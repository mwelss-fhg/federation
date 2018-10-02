/*-
 * ===============LICENSE_START=======================================================
 * Acumos
 * ===================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property & Tech Mahindra. All rights reserved.
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

package org.acumos.federation.gateway.security;

import java.util.List;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.servlet.http.HttpServletResponse;

import org.acumos.cds.domain.MLPPeer;
import org.acumos.federation.gateway.config.EELFLoggerDelegate;
import org.acumos.federation.gateway.service.PeerService;
import org.acumos.federation.gateway.util.Utils;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.access.AccessDeniedHandler;

/**
 * 
 * X.509 certificate authentication : verifying the identity of a communication
 * peer when using the HTTPS (HTTP over SSL) protocol.
 *
 * EnableWebSecurity would probably be sufficient but needs some more work as PreAuthorized would
 * not be accessible.
 */

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class AuthenticationConfiguration extends WebSecurityConfigurerAdapter {

	private final EELFLoggerDelegate log = EELFLoggerDelegate.getLogger(getClass().getName());

	@Autowired
	private PeerService peerService;

	@Value("${federation.enablePeerAuthentication:true}")
	private boolean securityEnabled;

	public AuthenticationConfiguration() {
	}

	/*
	 * public X509AuthenticationFilter(boolean disableDefaults) {
	 * super(disableDefaults); }
	 */
	/**
	 * subjectPrincipalRegex("CN=(.*?)(?:,|$)") :- The regular expression used to
	 * extract a username from the client certificates subject name. (CN value of
	 * the client certificate)
	 */
	@Override
	protected void configure(HttpSecurity http) throws Exception {

		http
				.csrf()
					.disable();
		//when the user is a valid user but does not have the right priviledges the accessDeniedHandler 
		//is called
		http
				.exceptionHandling()
					.accessDeniedHandler(accessDeniedHandler());
		http
				.authorizeRequests()
					.anyRequest()
						.authenticated()
					.and()
						.sessionManagement()
							.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
					.and()
						.x509()
							.subjectPrincipalRegex("(.*)")  //select whole subject line
							.userDetailsService(userDetailsService());
	}

	/** */
	@Bean
	public AccessDeniedHandler accessDeniedHandler() {
		return ((request, response, exception) -> {
			log.info(EELFLoggerDelegate.debugLogger, "accessDeniedHandler : " + exception);
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		});
	}

	@Bean
	@Lazy
	public Peer self() {
		MLPPeer self = peerService.getSelf();
		if (self == null)
			throw new BeanCreationException("Unable to find 'self' peer");
		return new Peer(self, Role.SELF.priviledges());
	}

	/** */
	@Bean
	public UserDetailsService userDetailsService() {
		return (subject -> {
			log.info(EELFLoggerDelegate.debugLogger, " X509 subject {}", subject);
			LdapName x500subject = null;
			try {
				x500subject = new LdapName(subject);
			}
			catch (InvalidNameException inx) {
				log.warn(EELFLoggerDelegate.errorLogger, "Failed to parse subject information {}", subject);
				return new Peer(new MLPPeer(), Role.ANY);
			}

			String cn = null,
						 email = null,
						 ou = null, o = null, st = null, c = null;
			for (Rdn rdn :  x500subject.getRdns()) {
				if ("CN".equalsIgnoreCase(rdn.getType())) {
					cn = rdn.getValue().toString();
				}
				else if ("emailaddress".equalsIgnoreCase(rdn.getType())) {
					email = rdn.getValue().toString();
				}
				else if ("OU".equalsIgnoreCase(rdn.getType())) {
					ou = rdn.getValue().toString();
				}
				else if ("O".equalsIgnoreCase(rdn.getType())) {
					o = rdn.getValue().toString();
				}
				else if ("ST".equalsIgnoreCase(rdn.getType())) {
					st = rdn.getValue().toString();
				}
				else if ("C".equalsIgnoreCase(rdn.getType())) {
					c = rdn.getValue().toString();
				}
			}

			List<MLPPeer> mlpPeers = peerService.getPeerBySubjectName(cn);
			log.info(EELFLoggerDelegate.debugLogger, "Peers matching X509 subject {}", mlpPeers);
			if (!Utils.isEmptyList(mlpPeers)) {
				MLPPeer mlpPeer = mlpPeers.get(0);
				//!!here we create other instances of 'self'
				return new Peer(mlpPeer, mlpPeer.isSelf() ? Role.SELF : Role.PEER);
			}
			else {
				MLPPeer unknown = new MLPPeer();
				// set it up with available info
				unknown.setSubjectName(cn);
				unknown.setName(cn);
				unknown.setDescription(
					(ou == null ? "" : ou + ",") + (o == null ? "" : o + ",") +
					(st == null ? "" : st + ",") + (c == null ? "" : c + ","));
				unknown.setApiUrl("https://" + cn);
					//lookup SRV record
				unknown.setContact1(email);
				unknown.setLocal(false);
				unknown.setSelf(false);

				return new Peer(unknown, Role.ANY);
			}
		});
	}
}
