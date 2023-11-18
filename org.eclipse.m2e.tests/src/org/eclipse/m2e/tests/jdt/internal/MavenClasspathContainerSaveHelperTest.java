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

package org.eclipse.m2e.tests.jdt.internal;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.junit.Test;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.m2e.jdt.IClasspathManager;
import org.eclipse.m2e.jdt.internal.MavenClasspathContainer;
import org.eclipse.m2e.jdt.internal.MavenClasspathContainerSaveHelper;

public class MavenClasspathContainerSaveHelperTest {

  private MavenClasspathContainerSaveHelper helper = new MavenClasspathContainerSaveHelper();

  @Test
  public void testClasspathContainerSace() throws Exception {
    IClasspathEntry[] entries = new IClasspathEntry[2];

    {
      IAccessRule[] accessRules = new IAccessRule[1];
      accessRules[0] = JavaCore.newAccessRule(IPath.fromOSString("aa/**"), IAccessRule.K_ACCESSIBLE);

      IClasspathAttribute[] attributes = new IClasspathAttribute[2];
      attributes[0] = JavaCore.newClasspathAttribute("foo", "11");
      attributes[1] = JavaCore.newClasspathAttribute("moo", "22");

      entries[0] = JavaCore.newProjectEntry(IPath.fromOSString("/foo"), accessRules, true, attributes, false);
    }

    {
      IAccessRule[] accessRules = new IAccessRule[1];
      accessRules[0] = JavaCore.newAccessRule(IPath.fromOSString("bb/**"), IAccessRule.K_DISCOURAGED);

      IClasspathAttribute[] attributes = new IClasspathAttribute[1];
      attributes[0] = JavaCore.newClasspathAttribute("foo", "aa");

      entries[1] = JavaCore.newLibraryEntry(IPath.fromOSString("/foo/moo.jar"), IPath.fromOSString("/foo/moo-sources.jar"), IPath.fromOSString(
          "/foo/moo-javadoc.jar"), accessRules, attributes, false);
    }

    ByteArrayOutputStream os = new ByteArrayOutputStream();
    helper.writeContainer(new MavenClasspathContainer(IPath.fromOSString(IClasspathManager.CONTAINER_ID), entries), os);

    IClasspathContainer container = helper.readContainer(new ByteArrayInputStream(os.toByteArray()));

    assertEquals(IClasspathManager.CONTAINER_ID, container.getPath().toString());

    IClasspathEntry[] classpathEntries = container.getClasspathEntries();
    assertEquals(2, classpathEntries.length);

    {
      IClasspathEntry entry = classpathEntries[0];
      assertEquals(IClasspathEntry.CPE_PROJECT, entry.getEntryKind());
      assertEquals("/foo", entry.getPath().toString());
      assertEquals(false, entry.isExported());
      assertEquals(true, entry.combineAccessRules());

      IAccessRule[] accessRules = entry.getAccessRules();
      assertEquals(1, accessRules.length);
      assertEquals(IAccessRule.K_ACCESSIBLE, accessRules[0].getKind());
      assertEquals("aa/**", accessRules[0].getPattern().toString());

      IClasspathAttribute[] attributes = entry.getExtraAttributes();
      assertEquals(2, attributes.length);
      assertEquals("foo", attributes[0].getName());
      assertEquals("11", attributes[0].getValue());
      assertEquals("moo", attributes[1].getName());
      assertEquals("22", attributes[1].getValue());
    }

    {
      IClasspathEntry entry = classpathEntries[1];
      assertEquals(IClasspathEntry.CPE_LIBRARY, entry.getEntryKind());
      assertEquals("/foo/moo.jar", entry.getPath().toString());
      assertEquals(false, entry.isExported());

      IAccessRule[] accessRules = entry.getAccessRules();
      assertEquals(1, accessRules.length);
      assertEquals(IAccessRule.K_DISCOURAGED, accessRules[0].getKind());
      assertEquals("bb/**", accessRules[0].getPattern().toString());

      IClasspathAttribute[] attributes = entry.getExtraAttributes();
      assertEquals(1, attributes.length);
      assertEquals("foo", attributes[0].getName());
      assertEquals("aa", attributes[0].getValue());
    }
  }

}
