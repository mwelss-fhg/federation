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

package org.acumos.federation.gateway.util;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.acumos.federation.gateway.config.EELFLoggerDelegate;

import org.acumos.cds.domain.MLPAccessType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Utils {

	private final static EELFLoggerDelegate logger = EELFLoggerDelegate.getLogger(Utils.class);
	
	public static ObjectMapper objectMapper = new ObjectMapper();
	
	
	@Autowired
	private Environment env;
	
	public Utils() {
	}
	
	public static boolean isEmptyOrNullString(String input) {
		boolean isEmpty = false;
		if (null == input || 0 == input.trim().length()) {
			isEmpty = true;
		}
		return isEmpty;
	}
	
	public static boolean isEmptyList(List input) {
		boolean isEmpty = false;
		if (null == input || 0 == input.size()) {
			isEmpty = true;
		}
		return isEmpty;
	}
	
	public static Map<String, Object> jsonStringToMap(String jsonString) {
		Map< String, Object> map = new HashMap<>();
		
		if(!isEmptyOrNullString(jsonString)) {
			try {
				map = objectMapper.readValue(jsonString, new TypeReference<Map< String, Object>>() {
				});
			}
			catch (IOException x) {
				throw new IllegalArgumentException("Argument not a map", x);
			}
		}
		return map;
	}

	public static String mapToJsonString(Map<String,?> theMap) {

		try {
			return objectMapper.writeValueAsString(theMap);
		}
		catch (JsonProcessingException x) {
			throw new IllegalArgumentException("Failed to convert", x);
		}
	}	
	
	/**
	 * 
	 * @param accessType
	 * @return
	 * 		MLPAccessType object for Storing in DB
	 */
	public static MLPAccessType getMLPAccessType(String accessType) {
		MLPAccessType mlpAccessType = null;
		
		if(!isEmptyOrNullString(accessType)) {
			mlpAccessType = new MLPAccessType();
			if(accessType.equals("PB")) {
				
				mlpAccessType.setAccessCode("PB");
				mlpAccessType.setAccessName("Public");
			} else if(accessType.equals("OR")) {
				mlpAccessType.setAccessCode("OR");
				mlpAccessType.setAccessName("Organization");
			} else if(accessType.equals("PR")) {
				mlpAccessType.setAccessCode("PR");
				mlpAccessType.setAccessName("Private");
			} else {//Default
				mlpAccessType.setAccessCode("PR");
				mlpAccessType.setAccessName("Private");
			}
		} 
		return mlpAccessType;
	}
	
	public static String getTempFolderPath(String artifactName, String version, String nexusTempPath) {
		logger.debug("--------------- getTempFolderPath() started --------------");
		if (!isEmptyOrNullString(nexusTempPath)) {
			nexusTempPath = nexusTempPath + "/" + artifactName + "/" + version;
			// create the directory for the solution and version specified
			File dir = new File(nexusTempPath);
			logger.debug("------------ Directory for artifactName and Version : -------------" + dir);
			if (!dir.exists()) {
				logger.debug("------------ Directory not exists for artifactName and Version : --------------------");
				dir.mkdir();
				logger.debug("----------- New Directory created for artifactName and Version : ----------------" + dir);
			}
		}
		logger.debug("-------------  getTempFolderPath() ended ---------------");
		return nexusTempPath;
	}
	
	public static void deletetTempFiles(String tempFolder) throws Exception {
		logger.info("--------  deletetTempFiles() Started ------------");

		File directory = new File(tempFolder);

		// make sure directory exists
		if (!directory.exists()) {
			logger.debug("----------- Directory does not exist. ----------");

		} else {

			try {

				delete(directory);
				logger.info("----------- deletetTempFiles() Ended ----------------");
			} catch (Exception e) {
				logger.error("--------- Exception deletetTempFiles() -------------", e);
				throw e;
			}
		}

	}
	
	public static void delete(File file) throws IOException {
		logger.debug("------------  delete() started ------------");
		try {
			if (file.isDirectory()) {

				// directory is empty, then delete it
				if (file.list().length == 0) {

					file.delete();
					logger.debug("--------- Directory is deleted : ----------- " + file.getAbsolutePath());

				} else {

					// list all the directory contents
					String files[] = file.list();

					if (files != null && files.length != 0) {
						for (String temp : files) {
							// construct the file structure
							File fileDelete = new File(file, temp);

							// recursive delete
							delete(fileDelete);
						}
					}

					// check the directory again, if empty then delete it
					if (file.list().length == 0) {
						file.delete();
						logger.debug(" -------Directory is deleted :------------- " + file.getAbsolutePath());
					}
				}

			} else {
				// if file, then delete it
				file.delete();
				logger.debug("---------File is deleted :----------- " + file.getAbsolutePath());
			}
			logger.debug("------------- delete() ended ---------------");
		} catch (Exception ex) {
			logger.error("----------- Exceptoin Occured delete() ---------------", ex);

		}
	}
	
	
	
}

