/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.tests;

import org.maven.ide.eclipse.wizards.MavenProjectWizardArchetypeParametersPage;

import junit.framework.TestCase;

/**
 * @author @author Eugene Kuleshov
 */
public class JavaUtilTest extends TestCase {

  public void testGetDefaultJavaPackage() {
    assertEquals("foo", MavenProjectWizardArchetypeParametersPage.getDefaultJavaPackage("", ""));
    assertEquals("aaa.bbb", MavenProjectWizardArchetypeParametersPage.getDefaultJavaPackage("aaa", "bbb"));
    assertEquals("aaa.bbb.ccc.ddd", MavenProjectWizardArchetypeParametersPage.getDefaultJavaPackage("aaa.bbb.ccc", "ddd"));
    assertEquals("aaa.bbb1", MavenProjectWizardArchetypeParametersPage.getDefaultJavaPackage("aaa", "bbb1"));
    assertEquals("aaa.bbb", MavenProjectWizardArchetypeParametersPage.getDefaultJavaPackage("aaa", "1bbb"));
    assertEquals("aaa.a_b", MavenProjectWizardArchetypeParametersPage.getDefaultJavaPackage("aaa", "a-b"));
  }

}
