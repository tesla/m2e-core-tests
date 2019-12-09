/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package com.cmaxinc.test;

import java.net.URL;
import java.net.URLClassLoader;

public class TestApp {

	public static void main(String[] args){
		URLClassLoader cl = (URLClassLoader) new TestApp().getClass().getClassLoader();
		URL[] urls = cl.getURLs();
		for (URL url : urls) {
			if(url.toExternalForm().indexOf("testlib")!=-1)
				System.out.println(url.toExternalForm());
		}
		System.out.println("");
		
		new VersionPrinter();
	}
}
