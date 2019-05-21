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
package org.acumos.federation.gateway;

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;

import com.github.dockerjava.api.DockerClient;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.ListImagesCmd;
import com.github.dockerjava.api.command.LoadImageCmd;
import com.github.dockerjava.api.command.PullImageCmd;
import com.github.dockerjava.api.command.PushImageCmd;
import com.github.dockerjava.api.command.RemoveImageCmd;
import com.github.dockerjava.api.command.SaveImageCmd;
import com.github.dockerjava.api.command.TagImageCmd;
import com.github.dockerjava.api.model.AuthConfig;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.api.model.PullResponseItem;
import com.github.dockerjava.core.command.PullImageResultCallback;
import com.github.dockerjava.core.command.PushImageResultCallback;

public class SimulatedDockerClient {
	private byte[] saveResult;
	private DockerClient client;
	private boolean doPullTimeout;
	private ArrayList<Image> images = new ArrayList<>();

	public SimulatedDockerClient() {
		PullResponseItem pullResponseItem = mock(PullResponseItem.class);
		when (pullResponseItem.getStatus()).thenReturn("some status");
		when (pullResponseItem.isPullSuccessIndicated()).thenReturn(true);
		PullImageCmd pullImageCmd = mock(PullImageCmd.class);
		when (pullImageCmd.exec(any(PullImageResultCallback.class))).thenAnswer(invoke -> {
			PullImageResultCallback cb = (PullImageResultCallback)invoke.getArguments()[0];
			cb.onStart(null);
			cb.onNext(pullResponseItem);
			if (this.doPullTimeout) {
				Thread.currentThread().interrupt();
			} else {
				cb.onComplete();
			}
			return null;
		});

		SaveImageCmd saveImageCmd = mock(SaveImageCmd.class);
		when (saveImageCmd.exec()).thenAnswer(invoke -> new ByteArrayInputStream(this.saveResult));

		LoadImageCmd loadImageCmd = mock(LoadImageCmd.class);

		ListImagesCmd listImagesCmd = mock(ListImagesCmd.class);
		when (listImagesCmd.exec()).thenAnswer(invoke -> this.images);

		TagImageCmd tagImageCmd = mock(TagImageCmd.class);

		RemoveImageCmd removeImageCmd = mock(RemoveImageCmd.class);
		when (removeImageCmd.withForce(true)).thenReturn(removeImageCmd);

		PushImageResultCallback pushImageResultCallback = mock(PushImageResultCallback.class);

		PushImageCmd pushImageCmd = mock(PushImageCmd.class);
		when (pushImageCmd.withTag(any(String.class))).thenReturn(pushImageCmd);
		when (pushImageCmd.withAuthConfig(any(AuthConfig.class))).thenReturn(pushImageCmd);
		when (pushImageCmd.exec(any(PushImageResultCallback.class))).thenReturn(pushImageResultCallback);

		client = mock(DockerClient.class);
		when (client.pullImageCmd(any(String.class))).thenReturn(pullImageCmd);
		when (client.saveImageCmd(any(String.class))).thenReturn(saveImageCmd);
		when (client.loadImageCmd(any(InputStream.class))).thenReturn(loadImageCmd);
		when (client.listImagesCmd()).thenReturn(listImagesCmd);
		when (client.tagImageCmd(any(String.class), any(String.class), any(String.class))).thenReturn(tagImageCmd);
		when (client.removeImageCmd(any(String.class))).thenReturn(removeImageCmd);
		when (client.pushImageCmd(any(String.class))).thenReturn(pushImageCmd);
	}

	public DockerClient getClient() {
		return client;
	}

	public void clearImages() {
		this.images.clear();
	}

	public void addImage(String id, String... tags) {
		Image image = mock(Image.class);
		when(image.getId()).thenReturn(id);
		when(image.getRepoTags()).thenReturn(tags);
		this.images.add(image);
	}

	public void setSaveResult(byte[] saveResult) {
		this.saveResult = saveResult;
	}

	public void setDoPullTimeout(boolean doPullTimeout) {
		this.doPullTimeout = doPullTimeout;
	}
}
