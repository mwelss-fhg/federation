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

import org.acumos.cds.domain.MLPPeer;

import org.acumos.federation.gateway.config.EELFLoggerDelegate;
import org.acumos.federation.gateway.service.PeerService;
import org.acumos.federation.gateway.util.Utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * 
 * X.509 certificate authentication :  verifying the identity of a communication peer when using the HTTPS (HTTP over SSL) protocol.
 *
 */

@Configuration
@EnableWebSecurity
//@EnableGlobalMethodSecurity(prePostEnabled = true)
public class X509AuthenticationFilter extends WebSecurityConfigurerAdapter {
	
	private final EELFLoggerDelegate log = EELFLoggerDelegate.getLogger(getClass().getName());

	@Autowired
	private PeerService peerService;

	@Value("${federation.enablePeerAuthentication:true}")
	private boolean securityEnabled;

	public X509AuthenticationFilter() {
		// TODO Auto-generated constructor stub
	}

	public X509AuthenticationFilter(boolean disableDefaults) {
		super(disableDefaults);
		// TODO Auto-generated constructor stub
	}

	/**
     * subjectPrincipalRegex("CN=(.*?)(?:,|$)") :- The regular expression used to extract a username from the client certificates subject name.
     * (CN value of the client certificate)
     */
	@Override
	protected void configure(HttpSecurity http) throws Exception {

		http
		.authorizeRequests()
			.anyRequest().authenticated()
		.and()
			.x509()
				.subjectPrincipalRegex("CN=(.*?)(?:,|$)")
				.userDetailsService(userDetailsService());

	}
	
	@Bean
	public UserDetailsService userDetailsService() {
		return (username -> {
			log.info(EELFLoggerDelegate.debugLogger, " X509 subject : " + username);
			List<MLPPeer> mlpPeers = peerService.getPeer(username);
			log.info(EELFLoggerDelegate.debugLogger, " Peers matching X509 subject : " + mlpPeers);
      if(!Utils.isEmptyList(mlpPeers)) {
				log.info(EELFLoggerDelegate.debugLogger, " We are providing a matching Use ");
				return new User(username, "", AuthorityUtils.commaSeparatedStringToAuthorityList("PEER"));
			}
			else	{
				return null;
			}
		});
 	}
}

