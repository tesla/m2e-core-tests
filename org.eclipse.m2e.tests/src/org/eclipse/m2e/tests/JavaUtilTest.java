/*******************************************************************************
 * Copyright (c) 2008-2014 Sonatype, Inc.
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

package org.eclipse.m2e.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import org.eclipse.m2e.core.ui.internal.wizards.MavenProjectWizardArchetypeParametersPage;


public class JavaUtilTest {
  @Test
  public void testGetDefaultJavaPackage() {
    assertEquals("", MavenProjectWizardArchetypeParametersPage.getDefaultJavaPackage("", ""));
    assertEquals("aaa.bbb", MavenProjectWizardArchetypeParametersPage.getDefaultJavaPackage("aaa", "bbb"));
    assertEquals("aaa.bbb.ccc.ddd",
        MavenProjectWizardArchetypeParametersPage.getDefaultJavaPackage("aaa.bbb.ccc", "ddd"));
    assertEquals("aaa.bbb1", MavenProjectWizardArchetypeParametersPage.getDefaultJavaPackage("aaa", "bbb1"));
    assertEquals("aaa.bbb", MavenProjectWizardArchetypeParametersPage.getDefaultJavaPackage("aaa", "1bbb"));
    assertEquals("aaa.a_b", MavenProjectWizardArchetypeParametersPage.getDefaultJavaPackage("aaa", "a-b"));
  }

}
