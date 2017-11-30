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

package org.acumos.federation.gateway.adapter.onap.sdc;

import java.net.URI;
import java.net.URISyntaxException;
import javax.net.ssl.HttpsURLConnection;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;
import java.util.Collections;

import java.util.function.BiFunction;
import java.util.function.UnaryOperator;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpEntity;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.AsyncClientHttpRequestExecution;
import org.springframework.http.client.AsyncClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.http.converter.HttpMessageConverter;

import org.springframework.util.Base64Utils;
//import org.springframework.util.DigestUtils;
import org.apache.commons.codec.digest.DigestUtils;

import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.boot.context.properties.ConfigurationProperties;

import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ImmutableMap;

import org.acumos.federation.gateway.util.JSONHttpMessageConverter;
import org.acumos.federation.gateway.util.Action;
import org.acumos.federation.gateway.util.Future;
import org.acumos.federation.gateway.util.Futures;

import org.json.JSONObject;
import org.json.JSONArray;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;



@Component("asdc")
@Scope("singleton")
@ConfigurationProperties(prefix="asdc")
public class ASDC {
	
	public static enum AssetType {
		resource,
		service,
		product
	}

	public static enum ArtifactType {
		DCAE_TOSCA,
		DCAE_JSON,
		DCAE_POLICY,
		DCAE_DOC,
		DCAE_EVENT,
		DCAE_INVENTORY_TOSCA,
		DCAE_INVENTORY_JSON,
		DCAE_INVENTORY_POLICY,
		DCAE_INVENTORY_DOC,
		DCAE_INVENTORY_BLUEPRINT,
		DCAE_INVENTORY_EVENT,
		HEAT,
		HEAT_VOL,
		HEAT_NET,
		HEAT_NESTED,
		HEAT_ARTIFACT,
		HEAT_ENV,
		OTHER
	}

	public static enum ArtifactGroupType {
		DEPLOYMENT,
		INFORMATIONAL
	}

	public static enum LifecycleState {
		Checkin,
		Checkout,
		Certify,
		undocheckout
	}

	
//	@Retention(RetentionPolicy.RUNTIME)
//	@Target(ElementType.METHOD)
//	public @interface Mandatory {
//	}

	private Logger log = Logger.getLogger(ASDC.class.getName());

	private URI			rootUri;
	private String	rootPath = "/asdc/"; //"/sdc1/feproxy/"; //"/sdc/v1/catalog/";
	private String	user,
									passwd;
	private String	instanceId;

	public void setUri(URI theUri) {
		String userInfo = theUri.getUserInfo();
		if (userInfo != null) {
			String[] userInfoParts = userInfo.split(":");
			setUser(userInfoParts[0]);
			if (userInfoParts.length > 1)
				setPassword(userInfoParts[1]);
		}
		String fragment = theUri.getFragment();
		if (fragment == null)
			throw new IllegalArgumentException("The URI must contain a fragment specification, to be used as ASDC instance id");
		setInstanceId(fragment);

		try {
			this.rootUri = new URI(theUri.getScheme(), null, theUri.getHost(), theUri.getPort(), theUri.getPath(), theUri.getQuery(), null);
		}
		catch (URISyntaxException urix) {
			throw new IllegalArgumentException("Invalid uri", urix);
		}
 	}

	public URI getUri() {
		return this.rootUri;	
	}

	public void setUser(String theUser) {
		this.user = theUser;
	}

	public String getUser() {
		return this.user;
	}

	public void setPassword(String thePassword) {
		this.passwd = thePassword;
	}

	public String getPassword() {
		return this.passwd;
	}

	public void setInstanceId(String theId) {
		this.instanceId = theId;
	}

	public String getInstanceId() {
		return this.instanceId;
	}
	
	public void setRootPath(String thePath) {
		this.rootPath = thePath;
	}

	public String getRootPath() {
		return this.rootPath;
	}

  @Scheduled(fixedRateString = "${beans.context.scripts.updateCheckFrequency?:60000}")
	public void checkForUpdates() { 
	}

	@PostConstruct
	public void initASDC() {
	}

	public <T> Future<T> getResources(Class<T> theType) {
		return getAssets(AssetType.resource, theType);
	}
	
	public Future<JSONArray> getResources() {
		return getAssets(AssetType.resource, JSONArray.class);
	}
	
	public <T> Future<T> getResources(Class<T> theType, String theCategory, String theSubCategory) {
		return getAssets(AssetType.resource, theType, theCategory, theSubCategory);
	}
	
	public Future<JSONArray> getResources(String theCategory, String theSubCategory) {
		return getAssets(AssetType.resource, JSONArray.class, theCategory, theSubCategory);
	}

	public <T> Future<T> getServices(Class<T> theType) {
		return getAssets(AssetType.service, theType);
	}
	
	public Future<JSONArray> getServices() {
		return getAssets(AssetType.service, JSONArray.class);
	}
	
	public <T> Future<T> getServices(Class<T> theType, String theCategory, String theSubCategory) {
		return getAssets(AssetType.service, theType, theCategory, theSubCategory);
	}
	
	public Future<JSONArray> getServices(String theCategory, String theSubCategory) {
		return getAssets(AssetType.service, JSONArray.class, theCategory, theSubCategory);
	}

	public <T> Future<T> getAssets(AssetType theAssetType, Class<T> theType) {
		return fetch(refAssets(theAssetType), theType);
	}
	
	public <T> Action<T> getAssetsAction(AssetType theAssetType, Class<T> theType) {
		return (() -> fetch(refAssets(theAssetType), theType));
	}
	
	public <T> Future<T> getAssets(AssetType theAssetType, Class<T> theType,
																 String theCategory, String theSubCategory) {
		return getAssets(theAssetType, theType, theCategory, theSubCategory, null);
	}

	public <T> Future<T> getAssets(AssetType theAssetType, Class<T> theType,
																 String theCategory, String theSubCategory, String theResourceType) {
		return fetch(refAssets(theAssetType) + filter(theCategory, theSubCategory, theResourceType), theType);
	}
	
	public <T> Action<T> getAssetsAction(AssetType theAssetType, Class<T> theType,
																 			 String theCategory, String theSubCategory, String theResourceType) {
		return (() -> fetch(refAssets(theAssetType) + filter(theCategory, theSubCategory, theResourceType), theType));
	}
	
	protected String refAssets(AssetType theAssetType) {
		return this.rootPath + theAssetType + "s/";
	}

	private String filter(String theCategory, String theSubCategory, String theResourceType) {
		StringBuilder filter = null;
		if (theCategory != null) {
			filter = new StringBuilder();
			filter.append("?category=")
						.append(theCategory);
			if (theSubCategory != null) {
				filter.append("&subCategory=")
							.append(theSubCategory);
				if (theResourceType != null) {
					filter.append("&resourceType=")
								.append(theResourceType);
				}
			}
		}
		return filter == null ? "" : filter.toString();
	}

	protected String refAsset(AssetType theAssetType, UUID theId) {
		return this.rootPath + theAssetType + "s/" + theId;
	}
	
	public <T> Future<T> getResource(UUID theId, Class<T> theType) {
		return getAsset(AssetType.resource, theId, theType);
	}
	
	public Future<JSONObject> getResource(UUID theId) {
		return getAsset(AssetType.resource, theId, JSONObject.class);
	}

	public <T> Future<T> getService(UUID theId, Class<T> theType) {
		return getAsset(AssetType.service, theId, theType);
	}
	
	public Future<JSONObject> getService(UUID theId) {
		return getAsset(AssetType.service, theId, JSONObject.class);
	}

	public <T> Future<T> getAsset(AssetType theAssetType, UUID theId, Class<T> theType) {
		return fetch(refAsset(theAssetType, theId) + "/metadata", theType);
	}
	
	public <T> Action<T> getAssetAction(AssetType theAssetType, UUID theId, Class<T> theType) {
		return (() -> fetch(refAsset(theAssetType, theId) + "/metadata", theType));
	}

	public Future<byte[]> getResourceArchive(UUID theId) {
		return getAssetArchive(AssetType.resource, theId);
	}

	public Future<byte[]> getServiceArchive(UUID theId) {
		return getAssetArchive(AssetType.service, theId);
	}

	public Future<byte[]> getAssetArchive(AssetType theAssetType, UUID theId) {
		return fetch(refAsset(theAssetType, theId) + "/toscaModel", byte[].class);
	}

	public Action<byte[]> getAssetArchiveAction(AssetType theAssetType, UUID theId) {
		return (() -> fetch(refAsset(theAssetType, theId) + "/toscaModel", byte[].class));
	}

	public Future<JSONObject> checkinResource(UUID theId, String theUser, String theMessage) {
		return cycleAsset(AssetType.resource, theId, LifecycleState.Checkin, theUser, theMessage);
	}

	public Future<JSONObject> checkinService(UUID theId, String theUser, String theMessage) {
		return cycleAsset(AssetType.service, theId, LifecycleState.Checkin, theUser, theMessage);
	}

	public Future<JSONObject> checkoutResource(UUID theId, String theUser, String theMessage) {
		return cycleAsset(AssetType.resource, theId, LifecycleState.Checkout, theUser, theMessage);
	}

	public Future<JSONObject> checkoutService(UUID theId, String theUser, String theMessage) {
		return cycleAsset(AssetType.service, theId, LifecycleState.Checkout, theUser, theMessage);
	}
	
	public Future<JSONObject> certifyResource(UUID theId, String theUser, String theMessage) {
		return cycleAsset(AssetType.resource, theId, LifecycleState.Certify, theUser, theMessage);
	}

	public Future<JSONObject> certifyService(UUID theId, String theUser, String theMessage) {
		return cycleAsset(AssetType.service, theId, LifecycleState.Certify, theUser, theMessage);
	}

	/* Normally theMessage is mandatory (and we'd use put instead of putOpt) but .. not so for undocheckout ..
	 */
	public Future<JSONObject> cycleAsset(AssetType theAssetType, UUID theId, LifecycleState theState,
																			 String theUser, String theMessage) {
		return post(refAsset(theAssetType, theId)	+ "/lifecycleState/" + theState,
							  (headers) -> prepareHeaders(headers)
															.header("USER_ID", theUser),
								new JSONObject().putOpt("userRemarks", theMessage));
	}

	protected String refAssetInstanceArtifact(AssetType theAssetType, UUID theAssetId, String theAssetInstance, UUID theArtifactId) {
		return refAsset(theAssetType, theAssetId) + "/resourceInstances/" + theAssetInstance + "/artifacts" + (theArtifactId == null ? "" : ("/" + theArtifactId));
	}

	protected String refAssetArtifact(AssetType theAssetType, UUID theAssetId, UUID theArtifactId) {
		return refAsset(theAssetType, theAssetId) + "/artifacts" + (theArtifactId == null ? "" : ("/" + theArtifactId));
	}
	
	public <T> Future<T> getResourceArtifact(UUID theAssetId, UUID theArtifactId, Class<T> theType) {
		return getAssetArtifact(AssetType.resource, theAssetId, theArtifactId, theType);
	}
	
	public <T> Future<T> getServiceArtifact(UUID theAssetId, UUID theArtifactId, Class<T> theType) {
		return getAssetArtifact(AssetType.service, theAssetId, theArtifactId, theType);
	}
	
	public <T> Future<T> getResourceInstanceArtifact(UUID theAssetId, UUID theArtifactId, String theInstance, Class<T> theType) {
		return getAssetInstanceArtifact(AssetType.resource, theAssetId, theInstance, theArtifactId, theType);
	}
	
	public <T> Future<T> getServiceInstanceArtifact(UUID theAssetId, UUID theArtifactId, String theInstance, Class<T> theType) {
		return getAssetInstanceArtifact(AssetType.service, theAssetId, theInstance, theArtifactId, theType);
	}

	public <T> Future<T> getAssetArtifact(AssetType theAssetType, UUID theAssetId, UUID theArtifactId, Class<T> theType) {
		return fetch(refAssetArtifact(theAssetType, theAssetId, theArtifactId), theType);
	}
	
	public <T> Action<T> getAssetArtifactAction(AssetType theAssetType, UUID theAssetId, UUID theArtifactId, Class<T> theType) {
		return (() -> fetch(refAssetArtifact(theAssetType, theAssetId, theArtifactId), theType));
	}
	
	public <T> Future<T> getAssetInstanceArtifact(AssetType theAssetType, UUID theAssetId, String theInstance, UUID theArtifactId, Class<T> theType) {
		return fetch(refAssetInstanceArtifact(theAssetType, theAssetId, theInstance, theArtifactId), theType);
	}
	
	public <T> Action<T> getAssetInstanceArtifactAction(AssetType theAssetType, UUID theAssetId, String theInstance, UUID theArtifactId, Class<T> theType) {
		return (() -> fetch(refAssetInstanceArtifact(theAssetType, theAssetId, theInstance, theArtifactId), theType));
	}
	
	public ArtifactUploadAction createResourceArtifact(UUID theAssetId) {
		return createAssetArtifact(AssetType.resource, theAssetId);
	}
	
	public ArtifactUploadAction createServiceArtifact(UUID theAssetId) {
		return createAssetArtifact(AssetType.service, theAssetId);
	}
	
	public ArtifactUploadAction createResourceInstanceArtifact(UUID theAssetId, String theInstance) {
		return createAssetInstanceArtifact(AssetType.resource, theAssetId, theInstance);
	}
	
	public ArtifactUploadAction createServiceInstanceArtifact(UUID theAssetId, String theInstance) {
		return createAssetInstanceArtifact(AssetType.service, theAssetId, theInstance);
	}

	public ArtifactUploadAction createAssetArtifact(AssetType theAssetType, UUID theAssetId) {
		return new ArtifactUploadAction()
									.ofAsset(theAssetType, theAssetId);
	}
	
	public ArtifactUploadAction createAssetInstanceArtifact(AssetType theAssetType, UUID theAssetId, String theInstance) {
		return new ArtifactUploadAction()
									.ofAssetInstance(theAssetType, theAssetId, theInstance);
	}

	public ArtifactUpdateAction updateResourceArtifact(UUID theAssetId, JSONObject theArtifactInfo) {
		return updateAssetArtifact(AssetType.resource, theAssetId, theArtifactInfo);
	}
	
	public ArtifactUpdateAction updateResourceInstanceArtifact(UUID theAssetId, String theInstance, JSONObject theArtifactInfo) {
		return updateAssetInstanceArtifact(AssetType.resource, theAssetId, theInstance, theArtifactInfo);
	}
	
	public ArtifactUpdateAction updateServiceArtifact(UUID theAssetId, JSONObject theArtifactInfo) {
		return updateAssetArtifact(AssetType.service, theAssetId, theArtifactInfo);
	}
	
	public ArtifactUpdateAction updateServiceInstanceArtifact(UUID theAssetId, String theInstance, JSONObject theArtifactInfo) {
		return updateAssetInstanceArtifact(AssetType.service, theAssetId, theInstance, theArtifactInfo);
	}

	public ArtifactUpdateAction updateAssetArtifact(AssetType theAssetType, UUID theAssetId, JSONObject theArtifactInfo) {
		return new ArtifactUpdateAction(theArtifactInfo)
									.ofAsset(theAssetType, theAssetId);
	}
	
	public ArtifactUpdateAction updateAssetInstanceArtifact(AssetType theAssetType, UUID theAssetId, String theInstance, JSONObject theArtifactInfo) {
		return new ArtifactUpdateAction(theArtifactInfo)
									.ofAssetInstance(theAssetType, theAssetId, theInstance);
	}

	public ArtifactDeleteAction deleteResourceArtifact(UUID theAssetId, UUID theArtifactId) {
		return deleteAssetArtifact(AssetType.resource, theAssetId, theArtifactId);
	}
	
	public ArtifactDeleteAction deleteResourceInstanceArtifact(UUID theAssetId, String theInstance, UUID theArtifactId) {
		return deleteAssetInstanceArtifact(AssetType.resource, theAssetId, theInstance, theArtifactId);
	}
	
	public ArtifactDeleteAction deleteServiceArtifact(UUID theAssetId, UUID theArtifactId) {
		return deleteAssetArtifact(AssetType.service, theAssetId, theArtifactId);
	}
	
	public ArtifactDeleteAction deleteServiceInstanceArtifact(UUID theAssetId, String theInstance, UUID theArtifactId) {
		return deleteAssetInstanceArtifact(AssetType.service, theAssetId, theInstance, theArtifactId);
	}

	public ArtifactDeleteAction deleteAssetArtifact(AssetType theAssetType, UUID theAssetId, UUID theArtifactId) {
		return new ArtifactDeleteAction(theArtifactId)
									.ofAsset(theAssetType, theAssetId);
	}
	
	public ArtifactDeleteAction deleteAssetInstanceArtifact(AssetType theAssetType, UUID theAssetId, String theInstance, UUID theArtifactId) {
		return new ArtifactDeleteAction(theArtifactId)
									.ofAssetInstance(theAssetType, theAssetId, theInstance);
	}

	
	public abstract class ASDCAction<A extends ASDCAction<A, T>, T> implements Action<T> { 

		protected JSONObject 	info; 				//info passed to asdc as request body
		protected String			operatorId;		//uid of the user performing the action: only required in the updatr

		protected ASDCAction(JSONObject theInfo) {
			this.info = theInfo;
		}
		
		protected abstract A self(); 

		protected ASDC asdc() {
			return ASDC.this;
		}
	
		protected A withInfo(JSONObject theInfo) {
			merge(this.info, theInfo);
			return self();
		}
	
		public A with(String theProperty, Object theValue) {
			info.put(theProperty, theValue);
			return self();
		}

		public A withOperator(String theOperator) {
			this.operatorId = theOperator;
			return self();			
		}
		
		protected abstract String[] mandatoryInfoEntries();
	
		protected void checkOperatorId() {
			if (this.operatorId == null) {
				throw new IllegalStateException("No operator id was provided");
			}
		}

		protected void checkMandatoryInfo() {
			for (String field: mandatoryInfoEntries()) {
				if (!info.has(field))			
					throw new IllegalStateException("No '" + field + "' was provided");
			}
		}
		
		protected void checkMandatory() {
			checkOperatorId();
			checkMandatoryInfo();
		}
	}

	protected static final String[] artifactMandatoryEntries = new String[] {};

	/**
   * We use teh same API to operate on artifacts attached to assets or to their instances
	 */
	public abstract class ASDCArtifactAction<A extends ASDCArtifactAction<A>> extends ASDCAction<A, JSONObject> {

		protected AssetType		assetType;
		protected UUID				assetId;
		protected String			assetInstance;

		protected ASDCArtifactAction(JSONObject theInfo) {
			super(theInfo);
		}
		
		protected A ofAsset(AssetType theAssetType, UUID theAssetId) {
			this.assetType = theAssetType;
			this.assetId = theAssetId;
			return self();			
		}
		
		protected A ofAssetInstance(AssetType theAssetType, UUID theAssetId, String theInstance) {
			this.assetType = theAssetType;
			this.assetId = theAssetId;
			this.assetInstance = theInstance;
			return self();			
		}
		
		protected String normalizeInstanceName(String theName) {
			return StringUtils.removePattern(theName, "[ \\.\\-]+").toLowerCase();
		}
		
		protected String[] mandatoryInfoEntries() {
			return ASDC.this.artifactMandatoryEntries;
		}

		protected String ref(UUID theArtifactId) {
			return (this.assetInstance == null) ?
								refAssetArtifact(this.assetType, this.assetId, theArtifactId) :
								refAssetInstanceArtifact(this.assetType, this.assetId, normalizeInstanceName(this.assetInstance), theArtifactId);
		}
	} 

	protected static final String[] uploadMandatoryEntries = new String[] { "artifactName",
																																					 "artifactType",
																																					 "artifactGroupType", 
																																					 "artifactLabel",
																																					 "description",
																																					 "payloadData" };

	public class ArtifactUploadAction extends ASDCArtifactAction<ArtifactUploadAction> {
		
		protected ArtifactUploadAction() {
			super(new JSONObject());
		}

		protected ArtifactUploadAction self() {
			return this;
		}
		
		public ArtifactUploadAction withContent(byte[] theContent) {
			return with("payloadData", Base64Utils.encodeToString(theContent));
		}

		public ArtifactUploadAction withContent(File theFile) throws IOException {
			return withContent(FileUtils.readFileToByteArray(theFile));
		}

		public ArtifactUploadAction withLabel(String theLabel) {
			return with("artifactLabel", theLabel);
		}
		
		public ArtifactUploadAction withName(String theName) {
			return with("artifactName", theName);
		}
		
		public ArtifactUploadAction withDisplayName(String theName) {
			return with("artifactDisplayName", theName);
		}

		public ArtifactUploadAction withType(ArtifactType theType) {
			return with("artifactType", theType.toString());
		}

		public ArtifactUploadAction withGroupType(ArtifactGroupType theGroupType) {
			return with("artifactGroupType", theGroupType.toString());
		}

		public ArtifactUploadAction withDescription(String theDescription) {
			return with("description", theDescription);
		}
		
		protected String[] mandatoryInfoEntries() {
			return ASDC.this.uploadMandatoryEntries;
		}

		public Future<JSONObject> execute() {
			checkMandatory();
			return ASDC.this.post(ref(null),
														(headers) -> prepareHeaders(headers)
																					.header("USER_ID", this.operatorId),
														this.info);
		}
	}

	protected static final String[] updateMandatoryEntries = new String[] { "artifactName",
																																					 "artifactType",
																																					 "artifactGroupType", 
																																					 "artifactLabel",
																																					 "description",
																																					 "payloadData" };

	/**
	 * In its current form the update relies on a previous artifact retrieval. One cannot build an update from scratch.
	 * The label, tye and group type must be submitted but cannot be updated
	 */
	public class ArtifactUpdateAction extends ASDCArtifactAction<ArtifactUpdateAction> {

		
		protected ArtifactUpdateAction(JSONObject theInfo) {
			super(theInfo);
		}
		
		protected ArtifactUpdateAction self() {
			return this;
		}
		
		public ArtifactUpdateAction withContent(byte[] theContent) {
			return with("payloadData", Base64Utils.encodeToString(theContent));
		}

		public ArtifactUpdateAction withContent(File theFile) throws IOException {
			return withContent(FileUtils.readFileToByteArray(theFile));
		}

		public ArtifactUpdateAction withDescription(String theDescription) {
			return with("description", theDescription);
		}
		
		public ArtifactUpdateAction withName(String theName) {
			return with("artifactName", theName);
		}
		
		protected String[] mandatoryInfoEntries() {
			return ASDC.this.updateMandatoryEntries;
		}

		/* The json object originates (normally) from a get so it will have entries we need to cleanup */
		protected void cleanupInfoEntries() {
			this.info.remove("artifactChecksum");
			this.info.remove("artifactUUID");
			this.info.remove("artifactVersion");
			this.info.remove("artifactURL");
			this.info.remove("artifactDescription");
		}
		
		public Future<JSONObject> execute() {
			UUID artifactUUID = UUID.fromString(this.info.getString("artifactUUID"));
			checkMandatory();
			cleanupInfoEntries();
			return ASDC.this.post(ref(artifactUUID),
														(headers) -> prepareHeaders(headers)
																					.header("USER_ID", this.operatorId),
														this.info);
		}
	}

	public class ArtifactDeleteAction extends ASDCArtifactAction<ArtifactDeleteAction> {

		private UUID		artifactId;
		
		protected ArtifactDeleteAction(UUID theArtifactId) {
			super(null);
			this.artifactId = theArtifactId;
		}
		
		protected ArtifactDeleteAction self() {
			return this;
		}
		
		public Future<JSONObject> execute() {
			checkMandatory();
			return ASDC.this.delete(ref(this.artifactId),
														  (headers) -> prepareHeaders(headers)
																					.header("USER_ID", this.operatorId));
		}
	}




	public VFCMTCreateAction createVFCMT() {
		return new VFCMTCreateAction();
	}
	
	protected static final String[] vfcmtMandatoryEntries = new String[] { "name",
																																				 "vendorName",
																																	 			 "vendorRelease",
																																				 "contactId" };


	public class VFCMTCreateAction extends ASDCAction<VFCMTCreateAction, JSONObject> {

		protected VFCMTCreateAction() {

			super(new JSONObject());
			this
				.with("resourceType", "VFCMT")
				.with("category", "Template")
				.with("subcategory", "Monitoring Template")
				.with("icon", "defaulticon");
		}
		
		protected VFCMTCreateAction self() {
			return this;
		}

		public VFCMTCreateAction withName(String theName) {
			return with("name", theName);
		}

		public VFCMTCreateAction withDescription(String theDescription) {
			return with("description", theDescription);
		}
		
		public VFCMTCreateAction withVendorName(String theVendorName) {
			return with("vendorName", theVendorName);
		}
		
		public VFCMTCreateAction withVendorRelease(String theVendorRelease) {
			return with("vendorRelease", theVendorRelease);
		}
		
		public VFCMTCreateAction withTags(String... theTags) {
			for (String tag: theTags)
				this.info.append("tags", tag);
			return this;			
		}
		
		public VFCMTCreateAction withIcon(String theIcon) {
			return with("icon", theIcon);
		}
		
		protected String[] mandatoryInfoEntries() {
			return ASDC.this.vfcmtMandatoryEntries;
		}
		
		public VFCMTCreateAction withContact(String theContact) {
			return with("contactId", theContact);
		}
		
		public Future<JSONObject> execute() {
		
			this.info.putOnce("contactId", this.operatorId);	
			this.info.append("tags", info.optString("name"));
			checkMandatory();
			return ASDC.this.post(refAssets(AssetType.resource),
														(headers) -> prepareHeaders(headers)
																					.header("USER_ID", this.operatorId),
														this.info);
		}

	}
	

	public VFCreateAction createVF() {
		return new VFCreateAction();
	}
	
	protected static final String[] vfMandatoryEntries = new String[] {"category",
																																		 "subcategory",
																																		 "name",
																																		 "vendorName",
																															 			 "vendorRelease",
																																		 "contactId" };



	public class VFCreateAction extends ASDCAction<VFCreateAction, JSONObject> {

		protected VFCreateAction() {

			super(new JSONObject());
			this
				.with("resourceType", "VF")
				.with("icon", "defaulticon");
		}
		
		protected VFCreateAction self() {
			return this;
		}

		public VFCreateAction withCategory(String theCategory) {
			return with("category", theCategory);
		}

		public VFCreateAction withSubCategory(String theSubCategory) {
			return with("subcategory", theSubCategory);
		}

		public VFCreateAction withName(String theName) {
			return with("name", theName);
		}

		public VFCreateAction withDescription(String theDescription) {
			return with("description", theDescription);
		}
		
		public VFCreateAction withVendorName(String theVendorName) {
			return with("vendorName", theVendorName);
		}
		
		public VFCreateAction withVendorRelease(String theVendorRelease) {
			return with("vendorRelease", theVendorRelease);
		}
		
		public VFCreateAction withTags(String... theTags) {
			for (String tag: theTags)
				this.info.append("tags", tag);
			return this;			
		}
		
		public VFCreateAction withIcon(String theIcon) {
			return with("icon", theIcon);
		}
		
		protected String[] mandatoryInfoEntries() {
			return ASDC.this.vfMandatoryEntries;
		}
		
		public VFCreateAction withContact(String theContact) {
			return with("contactId", theContact);
		}
		
		public Future<JSONObject> execute() {
		
			this.info.putOnce("contactId", this.operatorId);	
			this.info.append("tags", info.optString("name"));
			checkMandatory();
			return ASDC.this.post(refAssets(AssetType.resource),
														(headers) -> prepareHeaders(headers)
																					.header("USER_ID", this.operatorId),
														this.info);
		}

	}


	public static JSONObject merge(JSONObject theOriginal, JSONObject thePatch) {
		for (String key: thePatch.keySet()) {
			if (!theOriginal.has(key))
				theOriginal.put(key, thePatch.get(key));
		}
		return theOriginal;
	}

	protected URI refUri(String theRef) {
		try {
			return new URI(this.rootUri + theRef);
		}
		catch(URISyntaxException urisx) {
			throw new UncheckedIOException(new IOException(urisx));
		}
	}

	private HttpHeaders prepareHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.AUTHORIZATION, "Basic " + Base64Utils.encodeToString((this.user + ":" + this.passwd).getBytes()));
		headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
		headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_OCTET_STREAM_VALUE);
		headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE);
		headers.add("X-ECOMP-InstanceID", this.instanceId);

		return headers;
	}

	private RequestEntity.HeadersBuilder prepareHeaders(RequestEntity.HeadersBuilder theBuilder) {
		return theBuilder
			.header(HttpHeaders.AUTHORIZATION, "Basic " + Base64Utils.encodeToString((this.user + ":" + this.passwd).getBytes()))
			.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
			.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_OCTET_STREAM_VALUE)
			.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE)
			.header("X-ECOMP-InstanceID", this.instanceId);
	}

	public <T> Future<T> fetch(String theRef, Class<T> theContentType) {
		return exchange(theRef, HttpMethod.GET, new HttpEntity(prepareHeaders()), theContentType);
	}

	public Future<JSONObject> post(String theRef, JSONObject thePost) {
		return exchange(theRef, HttpMethod.POST, new HttpEntity<JSONObject>(thePost, prepareHeaders()), JSONObject.class);
	}
	
	public Future<JSONObject> post(String theRef, UnaryOperator<RequestEntity.HeadersBuilder> theHeadersBuilder, JSONObject thePost) {
		RequestEntity.BodyBuilder builder = RequestEntity.post(refUri(theRef));
		theHeadersBuilder.apply(builder);

		return exchange(theRef, HttpMethod.POST, builder.body(thePost), JSONObject.class);
	}
	
	public Future<JSONObject> delete(String theRef, UnaryOperator<RequestEntity.HeadersBuilder> theHeadersBuilder) {

		RequestEntity.HeadersBuilder builder = RequestEntity.delete(refUri(theRef));
		theHeadersBuilder.apply(builder);

		return exchange(theRef, HttpMethod.DELETE, builder.build(), JSONObject.class);
	}
	
	public <T> Future<T> exchange(String theRef, HttpMethod theMethod, HttpEntity theRequest, Class<T> theResponseType) {
		
		AsyncRestTemplate restTemplate = new AsyncRestTemplate();

		List<HttpMessageConverter<?>> converters = restTemplate.getMessageConverters();
		converters.add(0, new JSONHttpMessageConverter());
		restTemplate.setMessageConverters(converters);

		restTemplate.setInterceptors(Collections.singletonList(new ContentMD5Interceptor()));
/*
		restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
											public boolean	hasError(ClientHttpResponse theResponse) throws IOException {
												if (404 == theResponse.getRawStatusCode()) {
													System.out.println("Found a 404 !");
													return false;
												}
												return super.hasError(theResponse);
											}

											protected byte[] getResponseBody(ClientHttpResponse theResponse) {
												if (404 == theResponse.getRawStatusCode()) {
													return "[]".getBytes();
												}
												return super.getResponseBody(theResponse);
											}
									});
*/	
		//ResponseEntity<T> response = null;
		ASDCFuture<T> result = new ASDCFuture<T>();
		String uri = this.rootUri + theRef;
		try {
			restTemplate
				.exchange(uri, theMethod, theRequest, theResponseType)
					.addCallback(result.callback);
		}
		catch (RestClientException rcx) {
			log.log(Level.WARNING, "Failed to fetch " + uri, rcx);
			return Futures.failedFuture(rcx);
		}
		catch (Exception x) {
			log.log(Level.WARNING, "Failed to fetch " + uri, x);
			return Futures.failedFuture(x);
		}
	 
		return result;
	}



	public class ASDCFuture<T>
										extends Futures.BasicFuture<T> {

		private boolean http404toEmpty = false;

		ASDCFuture() {
		}

		public ASDCFuture setHttp404ToEmpty(boolean doEmpty) {
			this.http404toEmpty = doEmpty;
			return this;
		}

		ListenableFutureCallback<ResponseEntity<T>> callback = new ListenableFutureCallback<ResponseEntity<T>>() {

			public void	onSuccess(ResponseEntity<T> theResult) {
				ASDCFuture.this.result(theResult.getBody());
			}

			public void	onFailure(Throwable theError) {
				if (theError instanceof HttpClientErrorException) {
				//	if (theError.getRawStatusCode() == 404 && this.http404toEmpty)
				//		ASDCFuture.this.result(); //th eresult is of type T ...
				//	else
						ASDCFuture.this.cause(new ASDCException((HttpClientErrorException)theError));
				}
				else {
					ASDCFuture.this.cause(theError);
				}
			}
		};

	}

	public class ContentMD5Interceptor implements AsyncClientHttpRequestInterceptor {

    @Override
    public ListenableFuture<ClientHttpResponse> intercept(
            HttpRequest theRequest, byte[] theBody, AsyncClientHttpRequestExecution theExecution)
            																																					throws IOException {
				if (HttpMethod.POST == theRequest.getMethod()) {
	        HttpHeaders headers = theRequest.getHeaders();
  	      headers.add("Content-MD5", Base64Utils.encodeToString(
																				//DigestUtils.md5Digest(theBody)));
																				DigestUtils.md5Hex(theBody).getBytes()));
																					
				}
    	  return theExecution.executeAsync(theRequest, theBody);
    }
	}
	
//amdocs: "http://dcaedt:dcae123@135.16.121.89:8080#demo"));
	public static void main(String[] theArgs) throws Exception {

		CommandLineParser parser = new BasicParser();
		
		Options options = new Options();
		options.addOption(OptionBuilder
														  .withArgName("target")
															.withLongOpt("target")
                               .withDescription("target asdc system")
															.hasArg()
															.isRequired()
															.create('t') );
			
		options.addOption(OptionBuilder
														  .withArgName("rootPath")
															.withLongOpt("rootpath")
                               .withDescription("asdc rootpath")
															.hasArg()
															.isRequired()
															.create('r') );

		options.addOption(OptionBuilder
														  .withArgName("action")
															.withLongOpt("action")
                              .withDescription("one of: list, get, getartifact, checkin, checkout")
															.hasArg()
															.isRequired()
															.create('a') );

		options.addOption(OptionBuilder
														  .withArgName("assetType")
															.withLongOpt("assetType")
                               .withDescription("one of resource, service, product")
															.hasArg()
															.isRequired()
															.create('k') ); //k for 'kind' ..

		options.addOption(OptionBuilder
														  .withArgName("assetId")
															.withLongOpt("assetId")
                               .withDescription("asset uuid")
															.hasArg()
															.create('u') ); //u for 'uuid'

		options.addOption(OptionBuilder
														  .withArgName("artifactId")
															.withLongOpt("artifactId")
                               .withDescription("artifact uuid")
															.hasArg()
															.create('s') ); //s for 'stuff'
		options.addOption(OptionBuilder
														  .withArgName("instance")
															.withLongOpt("instance")
                               .withDescription("asset instance name")
															.hasArg()
															.create('i') );
		options.addOption(OptionBuilder
														  .withArgName("listFilter")
															.withLongOpt("listFilter")
                               .withDescription("filter for list operations")
															.hasArg()
															.create('f') ); //u for 'uuid'

		CommandLine line = null;
		try {
   		line = parser.parse(options, theArgs);
		}
		catch(ParseException exp) {
			System.err.println(exp.getMessage());
			new HelpFormatter().printHelp("asdc", options);
			return;
		}

		ASDC asdc = new ASDC();
		asdc.setUri(new URI(line.getOptionValue("target")));
		asdc.setRootPath(line.getOptionValue("rootpath"));

		String instance = line.getOptionValue("instance");
		String action = line.getOptionValue("action");
		if (action.equals("list")) {
			JSONObject filterInfo = new JSONObject(
																			line.hasOption("listFilter") ?
																				line.getOptionValue("listFilter") : "{}");
			JSONArray assets = 
				asdc.getAssets(ASDC.AssetType.valueOf(line.getOptionValue("assetType")), JSONArray.class,
											 filterInfo.optString("category", null), filterInfo.optString("subCategory", null))
						.waitForResult();
			for (int i = 0; i < assets.length(); i++) {
				System.out.println("> " + assets.getJSONObject(i).toString(2));
			}
		}
		else if (action.equals("get")) {
			System.out.println(
				asdc.getAsset(ASDC.AssetType.valueOf(line.getOptionValue("assetType")),
											UUID.fromString(line.getOptionValue("assetId")),
											JSONObject.class)
						.waitForResult()
						.toString(2)
			);
		}
		else if (action.equals("getartifact")) {
			if (instance == null) {
				System.out.println(
					asdc.getAssetArtifact(ASDC.AssetType.valueOf(line.getOptionValue("assetType")),
																UUID.fromString(line.getOptionValue("assetId")),
																UUID.fromString(line.getOptionValue("artifactId")),
																String.class)
							.waitForResult()
				);
			}
			else {
				System.out.println(
					asdc.getAssetInstanceArtifact(
																ASDC.AssetType.valueOf(line.getOptionValue("assetType")),
																UUID.fromString(line.getOptionValue("assetId")),
																instance,
																UUID.fromString(line.getOptionValue("artifactId")),
																String.class)
							.waitForResult()
				);
			}
		}
		else if (action.equals("checkin")) {
			System.out.println(
					asdc.cycleAsset(ASDC.AssetType.valueOf(line.getOptionValue("assetType")),
													UUID.fromString(line.getOptionValue("assetId")),
													ASDC.LifecycleState.Checkin,
													"Admin",
													"cli op")
							.waitForResult()
			);
		}
		else if (action.equals("checkout")) {
			System.out.println(
					asdc.cycleAsset(ASDC.AssetType.valueOf(line.getOptionValue("assetType")),
													UUID.fromString(line.getOptionValue("assetId")),
													ASDC.LifecycleState.Checkout,
													"Admin",
													"cli op")
							.waitForResult()
			);
		}
		else if (action.equals("cleanup")) {
			JSONArray resources = asdc.getResources()
																	.waitForResult();
			System.out.println("Got " + resources.length() + " resources");


			// vfcmt cleanup 
			for (int i = 0; i < resources.length(); i++) {

				JSONObject resource = resources.getJSONObject(i);

				if (resource.getString("resourceType").equals("VFCMT") &&
						resource.getString("name").contains("test")) {

					System.out.println("undocheckout for " + resource.getString("uuid"));

					try {
						asdc.cycleAsset(AssetType.resource, UUID.fromString(resource.getString("uuid")), LifecycleState.undocheckout, "sj2381", null)
							.waitForResult();
					}
					catch (Exception x) {
						System.out.println("** " + x);
					}
				}
			}

		}
		else {
			try {
				System.out.println(
					asdc.createVF()
							.withInfo(new JSONObject()
													.put("category", "Generic")
													.put("subcategory", "Abstract"))
							.withName("Cognitor")
							.withDescription("Acumos import 07262017")
							.withVendorName("AcumosInc")
							.withVendorRelease("1.0")
							.withTags("acumos")
							.withOperator("sj2381")
							.execute()
							.waitForResult());
			}
			catch(Exception x) {
				System.out.println("Failed to create VF: " + x);
			}
	}	
/*
		else {
			UUID cid = UUID.fromString(line.getOptionValue("assetId"));
			JSONObject jsonObj = asdc.getResource(cid).waitForResult();
			System.out.println(jsonObj);
		
			JSONObject artifactObj = null;
			boolean flag = false;

			JSONArray resObj = jsonObj.optJSONArray("artifacts");
			if(resObj != null) {
				for(int i=0; i< resObj.length(); i++) {
					artifactObj = (JSONObject) resObj.get(i);
					System.out.println(artifactObj);
					if(artifactObj.get("artifactName").equals("superposition.yml")) {
						asdc.updateResourceArtifact(cid, artifactObj)
							.withContent("{}".getBytes())
							.withOperator("sj2381")
				//			.withLabel("Superposition")
				//			.withType(ArtifactType.DCAE_TOSCA)
				//			.withGroupType(ArtifactGroupType.DEPLOYMENT)
							.withDescription(" serban asdc api test")
							.execute()
							.waitForResult();
							flag = true;
					}
				}
			}

			if(!flag) {
					asdc.createResourceArtifact(cid)
							.withName("superposition.yml")
							.withDisplayName("composition")
							.withContent("{}".getBytes())
							.withOperator("sj2381")
							.withLabel("Superposition")
							.withType(ArtifactType.DCAE_TOSCA)
							.withGroupType(ArtifactGroupType.DEPLOYMENT)
							.withDescription(" serban asdc api test")
							.execute()
							.waitForResult();
			}

			System.out.println(">>> " + 
				asdc.checkinResource(cid, "sj2381", "done")
						.waitForResult());
 

		}
*/
/*
		System.out.println(
		asdc.deleteServiceInstanceArtifact(UUID.fromString("b8c40b18-a295-4f7d-905f-4ce18f939f9c"),
																			 "vMOG_for_DCAEDT 1",
																			 UUID.fromString("0992b5a9-75e0-4f0f-994c-8c7be6616e13"))
								.withOperator("sj2381")
								.execute()
								.waitForResult());
*/

/*
		JSONArray services = asdc.getServices()
//("DCAE Component", null)
//																.waitForResult();
//		System.out.println("Got " + services.length() + " services");
//		System.out.println(services.toString(2));

		for (int i = 0; i < services.length(); i++) {
//			if (services.getJSONObject(i).getString("name").equals("DCAEDT_vMOG_Srvc3")) {
//			if (services.getJSONObject(i).getString("name").equals("MonicaforBP")) {
				UUID serviceId = UUID.fromString(services.getJSONObject(i).getString("uuid"));

				System.out.println("Service " + serviceId);

				if (theArgs.length > 1 && !theArgs[1].equals(serviceId.toString()))
					continue;
	
				JSONObject service = null;
				try {
					service = asdc.getService(serviceId)
																	.waitForResult();
				}
				catch(Exception x) {
					System.out.println(x);
					continue;
				}
				System.out.println("Service details: " + service);
			
				asdc.checkinService(serviceId, "sj2381", "ready for update")
							.waitForResult();
	

				JSONArray instances = service.optJSONArray("resources");
				if (instances != null) {
					JSONObject instance = instances.getJSONObject(0);
					try {
*/
						//System.out.println("Found instances, processing artifact for " + instance);

/*
						System.out.println(
						asdc.createServiceInstanceArtifact(UUID.fromString(services.getJSONObject(i).getString("uuid")),
																									instance.getString("resourceInstanceName"))
								.withName("test.yaml")
								.withDisplayName("artificial")
								.withLabel("artificial")
								.withDescription("serban asdc api test")
								.withContent(new File(theArgs[theArgs.length-1]))
								//.withType(ArtifactType.DCAE_TOSCA)
								//.withType(ArtifactType.HEAT_ARTIFACT)
								.withType(ArtifactType.DCAE_INVENTORY_BLUEPRINT)
								.withGroupType(ArtifactGroupType.DEPLOYMENT)
								.withOperator("sj2381")
								.execute()
								.waitForResult());
							asdc.deleteServiceInstanceArtifact(UUID.fromString(services.getJSONObject(i).getString("uuid")),
																								 instance.getString("resourceInstanceName"),
																								 UUID.fromString(
																								 	instance.getJSONArray("artifacts").getJSONObject(0).getString("artifactUUID")))
								.withOperator("sj2381")
								.execute()
								.waitForResult());
*/

/*
					}
					catch(Exception x) {
						System.out.println("Failed to create resource instance artifact" + x);
						return;
					}
//				}
			}
		}	
*/



//		List resources = asdc.getResources(List.class, "DCAE Component", "Database")
//																.waitForResult();
//		System.out.println(resources.toString());


//		String artifact = asdc.getResourceArtifact(UUID.fromString(theArgs[1]),UUID.fromString(theArgs[2]),String.class)
//														.waitForResult();
//		System.out.println(artifact);

//		System.out.println(
//			asdc.checkinService(UUID.fromString(theArgs[1]), "sj2381", "testing")
//				.waitForResult());

//		System.out.println(
//			asdc.checkinService(UUID.fromString(theArgs[1]), "sj2381", "tested")
//				.waitForResult());

/*
		try {
			System.out.println(
				asdc.createResourceArtifact(UUID.fromString(theArgs[1]))
						.withName("test.yml")
						.withDisplayName("test")
						.withLabel("test")
						.withDescription("serban asdc api test")
						.withContent(new File(theArgs[2]))
						.withType(ArtifactType.DCAE_TOSCA)
						.withGroupType(ArtifactGroupType.DEPLOYMENT)
						.withOperator("sj2381")
						.execute()
						.waitForResult());
		}
		catch(Exception x) {
			System.out.println("Failed to create asset " + x);
			return;
		}	
*/

/*
		JSONObject resource = asdc.getResource(UUID.fromString(theArgs[1]), JSONObject.class)
																.waitForResult();
		System.out.println(resource.toString(2));
		JSONArray artifacts = resource.getJSONArray("artifacts");
		JSONObject artifact = artifacts.getJSONObject(0);
*/


/*		
		byte[] archive = asdc.getResourceArchive(UUID.fromString(theArgs[1]))
													.waitForResult();
		FileUtils.writeByteArrayToFile(new File("archive.jar"), archive);	
*/

/*
		String artifactInfo = asdc.getResourceArtifact(
																						UUID.fromString(theArgs[1]),
																						UUID.fromString(artifacts.getJSONObject(0).getString("artifactUUID")),
																						String.class)
														.waitForResult();
		System.out.println(artifactInfo);
*/

/*
		System.out.println(
				asdc.fetch(
					asdc.refAssetArtifact(AssetType.resource, UUID.fromString(theArgs[1]), UUID.fromString(artifact.getString("artifactUUID"))), JSONObject.class)
					.waitForResult()
					.toString(2));
*/

/*
		try {
			System.out.println(
				asdc.updateResourceArtifact(UUID.fromString(theArgs[1]), artifact)
						.withDescription("serban asdc api test update")
						.withContent(new File(theArgs[3]))
						.withLabel("test")
						.withOperator("sj2381")
						.execute()
						.waitForResult());
		}
		catch(Exception x) {
			System.out.println("Failed to update asset " + x);
			return;
		}
*/


/*
		try {
			asdc.deleteResourceArtifact(UUID.fromString(theArgs[1]),
																	UUID.fromString(artifacts.getJSONObject(0).getString("artifactUUID")))
					.withOperator("sj2381")
					.execute()
					.waitForResult();
		}
		catch(Exception x) {
			System.out.println("Failed to delete asset artifact " + x);
		}	
*/

/*
	try {
		JSONObject artifact = artifacts.getJSONObject(0);

		System.out.println(
			asdc.updateResourceArtifact(UUID.fromString(theArgs[1]),
																  UUID.fromString(artifacts.getJSONObject(0).getString("artifactUUID")))
						.withName(artifact.getString("artifactName"))
						.withDescription("serban asdc api test update")
						.withContent(new File(theArgs[2]))
						.withGroupType(ArtifactGroupType.DEPLOYMENT)
						.withLabel("test")
						.withType(ArtifactType.valueOf(artifact.getString("artifactType")))
						.withATTContact("sj2381")
						.post()
						.waitForResult());
	}
	catch(Exception x) {
		System.out.println("Failed to update asset artifact " + x);
	}	
*/
/*
	try {
		System.out.println(
			asdc.createVFCMT()
							.withName("SB4.5")
							.withDescription("All mountain")
							.withVendorName("Yeti")
							.withVendorRelease("1.0")
							.withTags("MTB", "FS")
							.withOperator("sj2381")
							.execute()
							.waitForResult());
	}
	catch(Exception x) {
		System.out.println("Failed to create VFCMT: " + x);
	}	
*/
/*
		asdc.updateAssetArtifact(AssetType.resource, UUID.fromString(theArgs[1]), UUID.fromString(theArgs[2]))
					.withDescription("serban asdc api test")
					.post();	
*/
	}
}
