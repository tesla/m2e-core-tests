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

import java.net.URL;
import java.net.URLClassLoader;

import junit.framework.TestCase;

public class TestLibraryClass extends TestCase {

	public void testLibary(){
		URLClassLoader cl = (URLClassLoader) this.getClass().getClassLoader();
		URL[] urls = cl.getURLs();
		for (URL url : urls) {
			if(url.toExternalForm().indexOf("hibernate")!=-1)
				System.out.println(url.toExternalForm());
		}
		System.out.println("");

		new VersionPrinter();
	}
}
