/*-
 * ===============LICENSE_START=======================================================
 * Acumos
 * ===================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property & Tech Mahindra. All rights reserved.
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
package org.acumos.federation.gateway.test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import org.junit.Before;
import org.junit.FixMethodOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.AuthConfig;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.api.model.PushResponseItem;
import com.github.dockerjava.api.command.ListImagesCmd;
import com.github.dockerjava.api.command.LoadImageCmd;
import com.github.dockerjava.api.command.PullImageCmd;
import com.github.dockerjava.api.command.PushImageCmd;
import com.github.dockerjava.api.command.RemoveImageCmd;
import com.github.dockerjava.api.command.SaveImageCmd;
import com.github.dockerjava.api.command.TagImageCmd;
import com.github.dockerjava.core.command.PullImageResultCallback;

import org.acumos.federation.gateway.config.DockerConfiguration;
import org.acumos.federation.gateway.config.LocalInterfaceConfiguration;
import org.acumos.federation.gateway.config.NexusConfiguration;
import org.acumos.federation.gateway.service.ContentService;
import org.acumos.federation.gateway.service.ServiceException;
import org.acumos.federation.gateway.cds.Artifact;
import org.acumos.federation.gateway.cds.Document;


@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = org.acumos.federation.gateway.Application.class,
    webEnvironment = WebEnvironment.RANDOM_PORT,
    properties = {
	"spring.main.allow-bean-definition-overriding=true",
	"federation.instance=gateway",
	"federation.instance.name=test",
	"federation.operator=admin",
	"codes-local.source=classpath:test-codes.json",
	"peers-local.source=classpath:test-peers.json",
	"catalog-local.source=classpath:test-catalog.json",
	"catalog-local.catalogs=classpath:test-catalogs.json",
	"federation.ssl.key-store=classpath:acumosa.pkcs12",
	"federation.ssl.key-store-password=acumosa",
	"federation.ssl.key-store-type=PKCS12",
	"federation.ssl.key-password = acumosa",
	"federation.ssl.trust-store=classpath:acumosTrustStore.jks",
	"federation.ssl.trust-store-password=acumos",
	"federation.ssl.client-auth=need",
	"local.addr=127.0.0.1",
	"local.server.port=9011",
	"local.ssl.key-store=classpath:acumosa.pkcs12",
	"local.ssl.key-store-password=acumosa",
	"local.ssl.key-store-type=PKCS12",
	"local.ssl.key-password=acumosa",
	"cdms.client.url=http://localhost:8000/ccds",
	"cdms.client.username=username",
	"cdms.client.password=password"
})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ContentServiceTest	extends ServiceTest {

	@MockBean
	private NexusConfiguration nexusConfig;

	@Mock
	private RestTemplate nexusClient;

	@MockBean
	private DockerConfiguration dockerConfig;

	@Autowired
	private LocalInterfaceConfiguration localIfConfig;

	@Autowired
	private ContentService contentService;

	@Mock
	private DockerClient dockerClient;

	@Mock
	private ListImagesCmd listImagesCmd;

	@Mock
	private LoadImageCmd loadImageCmd;

	@Mock
	private PullImageCmd pullImageCmd;

	@Mock
	private PushImageCmd pushImageCmd;

	@Mock
	private RemoveImageCmd removeImageCmd;

	@Mock
	private SaveImageCmd saveImageCmd;

	@Mock
	private TagImageCmd tagImageCmd;

	private String dockerRegistryUrl = "dockerhost.example.com";

	private static Image makeImage(String id, String... tags) {
		Image ret = new Image();
		ReflectionTestUtils.setField(ret, "id", id);
		ReflectionTestUtils.setField(ret, "repoTags", tags);
		return(ret);
	}

	private List<Image> dockerImages = Arrays.asList(new Image[] {
	    makeImage("abc", "repo2.example.org/name:1.0"),
	    makeImage("def", "repo1.example.org/thatone:latest", "repo1.example.org/thatone:1.0")
	});
	private AuthConfig dockerAuthConfig = new AuthConfig()
	    .withUsername("username")
	    .withPassword("password")
	    .withEmail("someone@example.com")
	    .withRegistryAddress("http://" + dockerRegistryUrl + "/v2/");

	protected void initMockResponses() throws IOException {
		registerMockResponse("GET /ccds/peer/search?self=true&_j=a&page=0&size=100", MockResponse.success("mockCDSPeerSearchSelfResponse.json"));
		registerMockResponse("GET /ccds/code/pair/ARTIFACT_TYPE", MockResponse.success("mockCDSArtifactTypeResponse.json"));

		when(dockerConfig.getAuthConfig()).thenReturn(dockerAuthConfig);
		when(dockerConfig.getRegistryUrl()).thenReturn(dockerRegistryUrl);

		when(dockerConfig.getDockerClient()).thenReturn(dockerClient);
		  when(dockerClient.listImagesCmd()).thenReturn(listImagesCmd);
		    when(listImagesCmd.exec()).thenReturn(dockerImages);
		  when(dockerClient.loadImageCmd(any(InputStream.class))).thenReturn(loadImageCmd);
		    //when(loadImageCmd.exec()).doNothing();
		  when(dockerClient.pullImageCmd(any(String.class))).thenReturn(pullImageCmd);
		    when(pullImageCmd.exec(any(PullImageResultCallback.class))).thenAnswer(invoke -> {
			PullImageResultCallback cb = (PullImageResultCallback)invoke.getArguments()[0];
			cb.onStart(null);
			cb.onComplete();
			return(null);
		    });
		  when(dockerClient.pushImageCmd(any(String.class))).thenReturn(pushImageCmd);
		    when(pushImageCmd.withAuthConfig(any(AuthConfig.class))).thenReturn(pushImageCmd);
		    when(pushImageCmd.withTag(any(String.class))).thenReturn(pushImageCmd);
		    when(pushImageCmd.exec(any(ResultCallback.class))).thenAnswer(invoke -> {
			ResultCallback cb = (ResultCallback)invoke.getArguments()[0];
			cb.onStart(null);
			PushResponseItem pri = new PushResponseItem();
			cb.onNext(pri);
			cb.onComplete();
			return(cb);
		    });
		  when(dockerClient.removeImageCmd(any(String.class))).thenReturn(removeImageCmd);
		    when(removeImageCmd.withForce(any(Boolean.class))).thenReturn(removeImageCmd);
		    // when(removeImageCmd.exec()) do nothing
		  when(dockerClient.saveImageCmd(any(String.class))).thenAnswer(invoke -> {
			String uri = (String)invoke.getArguments()[0];
			if (uri.contains("Rainy_Day_Case")) {
				throw new IOException("Rainy Day Case");
			}
		        return(saveImageCmd);
		    });
		    when(saveImageCmd.exec()).thenAnswer(invoke -> new ByteArrayInputStream("Hello World".getBytes()));
		  when(dockerClient.tagImageCmd(any(String.class), any(String.class), any(String.class))).thenReturn(tagImageCmd);
		    // when(tagImageCmd.exec()) do nothing
	
		when(nexusConfig.getNexusClient()).thenReturn(nexusClient);
		when(nexusConfig.getGroupId()).thenReturn("com.artifact");
		when(nexusConfig.getNameSeparator()).thenReturn(".");
		when(nexusConfig.getUrl()).thenReturn("http://somehost.example.org/");
		when(nexusClient.exchange(any(RequestEntity.class),any(Class.class))).thenReturn(new ResponseEntity<Resource>(new InputStreamResource(new ByteArrayInputStream("hello".getBytes())), HttpStatus.OK));
	}

	@Test
	public void testNexusArtifacts() throws Exception {
		Artifact artifact = new Artifact();
		artifact.setUri("com/artifact/6793411f-c7a1-4e93-85bc-f91d267541d8/2c7e4481-6e6f-47d9-b7a4-c4e674d2b341/artifact/1.0/artifact-1.0.log");
		artifact.setArtifactTypeCode("DS");
		contentService.getArtifactContent(artifact).getInputStream().close();
		artifact = new Artifact();
		artifact.setArtifactTypeCode("DS");
		artifact.setName("artifact.log");
		artifact.setVersion("1.0");
		contentService.putArtifactContent("sid", artifact, new InputStreamResource(new ByteArrayInputStream("xxx".getBytes())));
	}


	@Test
	public void testDockerArtifacts() throws Exception {
		System.out.println(contentService);
		Artifact artifact = new Artifact();
		artifact.setArtifactTypeCode("DI");
		try {
			contentService.getArtifactContent(artifact).getInputStream().close();
			fail("Expected ServiceException");
		} catch (ServiceException se) {
			// We want this
		}
		try {
			artifact.setUri("Rainy_Day_Case");
			contentService.getArtifactContent(artifact).getInputStream().close();
			fail("Expected ServiceException");
		} catch (ServiceException se) {
			// We want this
		}
		artifact.setUri("com/artifact/6793411f-c7a1-4e93-85bc-f91d267541d8/2c7e4481-6e6f-47d9-b7a4-c4e674d2b341/artifact/1.0/artifact-1.0.log");
		contentService.getArtifactContent(artifact).getInputStream().close();
		artifact = new Artifact();
		artifact.setArtifactTypeCode("DI");
		artifact.setName("thatone");
		artifact.setVersion("1.0");
		artifact.setArtifactId("AnArtifactId");
		artifact.setDescription("repo1.example.org/thatone:1.0");
		contentService.putArtifactContent("sid", artifact, new InputStreamResource(new ByteArrayInputStream("xxx".getBytes())));
		assertEquals("dockerhost.example.com/AnArtifactId:1.0", artifact.getUri());
		try {
			artifact.setDescription("repo1.example.org/notthere:9.9");
			contentService.putArtifactContent("sid", artifact, new InputStreamResource(new ByteArrayInputStream("xxx".getBytes())));
			fail("Expected ServiceException");
		} catch (ServiceException se) {
			// We want this
		}
	}


	@Test
	public void testDocuments() throws Exception {
		Document document = new Document();
		try {
			contentService.getDocumentContent(document).getInputStream().close();
			fail("Expected ServiceException");
		} catch (ServiceException se) {
			// We want this
		}
		document.setUri("com/artifact/6793411f-c7a1-4e93-85bc-f91d267541d8/2c7e4481-6e6f-47d9-b7a4-c4e674d2b341/document/PB/document-PB.txt");
		contentService.getDocumentContent(document).getInputStream().close();
		document = new Document();
		document.setName("thatone");
		contentService.putDocumentContent("sid", document, new InputStreamResource(new ByteArrayInputStream("xxx".getBytes())));
		assertEquals("com/artifact/sid/thatone/na/thatone-na.", document.getUri());
	}

	@Test
	public void testDockerConfig() throws Exception {
		DockerConfiguration dc = new DockerConfiguration();
		dc.setHost("tcp://dockerhost.example.com:4243");
		dc.setApiVersion("v2");
		dc.setRegistryUsername("username");
		dc.getRegistryUsername();
		dc.setRegistryPassword("password");
		dc.setRegistryEmail("username@example.com");
		dc.setRegistryUrl("someregistry:1234");
		dc.getRegistryUrl();
		dc.setDockerCertPath(null);
		dc.setDockerConfig(null);
		dc.setDockerTlsVerify(false);
		dc.getAuthConfig();
		dc.getDockerClient();
	}

	@Test
	public void testNexusConfig() throws Exception {
		NexusConfiguration nc = new NexusConfiguration();
		ReflectionTestUtils.setField(nc, "localIfConfig", localIfConfig);
		nc.setId("someid");
		nc.setUrl("https://somenexus.example.org");
		assertEquals("https://somenexus.example.org/", nc.getUrl());
		nc.setUrl("https://somenexus.example.org/");
		assertEquals("https://somenexus.example.org/", nc.getUrl());
		nc.setUrl("https://somenexus.example.org//");
		assertEquals("https://somenexus.example.org/", nc.getUrl());
		nc.setUrl("https://somenexus.example.org///");
		assertEquals("https://somenexus.example.org/", nc.getUrl());
		nc.setUsername("somename");
		nc.setPassword("pw");
		nc.setProxy("deprecated");
		nc.setGroupId("a.b");
		nc.getGroupId();
		nc.setNameSeparator(".");
		nc.getNameSeparator();
		nc.getNexusClient();
	}
}
