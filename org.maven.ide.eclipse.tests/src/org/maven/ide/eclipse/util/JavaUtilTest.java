/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.util;

import junit.framework.TestCase;

/**
 * @author @author Eugene Kuleshov
 */
public class JavaUtilTest extends TestCase {

  public void testGetDefaultJavaPackage() {
    assertEquals(JavaUtil.DEFAULT_PACKAGE, JavaUtil.getDefaultJavaPackage("", ""));
    assertEquals("aaa.bbb", JavaUtil.getDefaultJavaPackage("aaa", "bbb"));
    assertEquals("aaa.bbb.ccc.ddd", JavaUtil.getDefaultJavaPackage("aaa.bbb.ccc", "ddd"));
    assertEquals("aaa.bbb1", JavaUtil.getDefaultJavaPackage("aaa", "bbb1"));
    assertEquals("aaa.bbb", JavaUtil.getDefaultJavaPackage("aaa", "1bbb"));
    assertEquals("aaa.a_b", JavaUtil.getDefaultJavaPackage("aaa", "a-b"));
  }

}
