/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package com.cmaxinc.test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class VersionPrinter {
	public VersionPrinter(){
		try {
			Properties props = new Properties();
			String pomPropertiesLocation = "META-INF/maven/com.cmaxinc.test/testlib/pom.properties";
			
			InputStream stream = this.getClass().getClassLoader().getResourceAsStream(pomPropertiesLocation);
			if(stream==null) throw new RuntimeException("Couldn't find stream using " + pomPropertiesLocation);
			props.load(stream);
			System.out.println("Test Library version " + props.get("version"));
		} catch (IOException e) {
			throw new RuntimeException("Can't find properties", e);
		}
	}
	
	public static void main(String[] args){
		new VersionPrinter();
	}
}
