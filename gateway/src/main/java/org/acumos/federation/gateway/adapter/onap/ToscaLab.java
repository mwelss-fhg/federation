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
package org.acumos.federation.gateway.adapter.onap;

import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.ByteArrayOutputStream;

import java.net.URI;
import java.net.URL;
import java.util.Properties;

import org.python.util.PythonInterpreter;

/**
 * Java interface to ToscaLab python code
 */
public class ToscaLab {

	public static void main(String[] theArgs) throws Exception {
		ToscaLab lab = new ToscaLab();
		for (String spec: theArgs)
			System.out.println(
				lab.create_model(new FileInputStream(spec)));
	}


	//private PythonInterpreter create_model_interpreter = null;
	//private PyCode create_model = null;

	public ToscaLab() {
		//the problem here is that if we use jython anywhere else this static initialization will be reflected
		Properties props = new Properties();
		props.put("python.console.encoding", "UTF-8");
		props.put("python.import.site","false");
		props.put("python.path", "/");
		
		PythonInterpreter.initialize(System.getProperties(), props, new String[] {"-i","stdin", "-o", "WEB", "-m", "/data/meta_model/meta_tosca_schema.yaml"});
	}

	public String create_model(InputStream theSpec) throws Exception {

		//if (this.create_model_interpreter == null) {
			PythonInterpreter python = new PythonInterpreter();
			python.setIn(theSpec);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			python.setOut(out);

			InputStream script = ClassLoader.getSystemResourceAsStream("model_create.py");
			if (script == null)
				throw new Exception("Failed to load 'model_create.py' script");
			else
				python.execfile(script);

			return out.toString();
		//}
	} 
}


