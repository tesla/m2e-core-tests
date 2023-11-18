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

package org.eclipse.m2e.tests.configurators;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Test;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.eclipse.m2e.tests.common.WorkspaceHelpers;


/**
 * Tests of settings file encoding for Maven project resource folders based on encoding configuration of
 * maven-resources-plugin.
 */
public class ResourcesEncodingConfiguratorTest extends AbstractMavenProjectTestCase {
  @Test
  public void test001_shouldConfigureEncodingForBasicProject() throws Exception {
    IProject project = importProject("projects/resourcesEncoding/p001/pom.xml");
    IJavaProject javaProject = JavaCore.create(project);
    WorkspaceHelpers.assertNoErrors(project);

    String mainResourceEncoding = javaProject.getProject().getFolder(IPath.fromOSString("src/main/resources"))
        .getDefaultCharset();
    assertEquals("Encoding configured for plugin not set on folder", "ISO-8859-1", mainResourceEncoding);
    String testResourceEncoding = javaProject.getProject().getFolder(IPath.fromOSString("src/test/resources"))
        .getDefaultCharset();
    assertEquals("Encoding configured for plugin not set on folder", "ISO-8859-1", testResourceEncoding);

    copyContent(project, new File("projects/resourcesEncoding/p001/pom2.xml"), "pom.xml");

    MavenPlugin.getProjectConfigurationManager().updateProjectConfiguration(project, monitor);

    String mainResourceEncodingChanged = javaProject.getProject().getFolder(IPath.fromOSString("src/main/resources"))
        .getDefaultCharset();
    assertEquals("Encoding configured for plugin not set on folder", "UTF-16", mainResourceEncodingChanged);
    String testResourceEncodingChanged = javaProject.getProject().getFolder(IPath.fromOSString("src/test/resources"))
        .getDefaultCharset();
    assertEquals("Encoding configured for plugin not set on folder", "UTF-16", testResourceEncodingChanged);
  }

  @Test
  public void test002_shouldConfigureEncodingForProjectWithSeveralResourceFolders() throws Exception {
    IProject project = importProject("projects/resourcesEncoding/p002/pom.xml");
    IJavaProject javaProject = JavaCore.create(project);
    WorkspaceHelpers.assertNoErrors(project);

    String mainResource1Encoding = javaProject.getProject().getFolder(IPath.fromOSString("src/main/resources"))
        .getDefaultCharset();
    assertEquals("Encoding configured for plugin not set on folder", "ISO-8859-1", mainResource1Encoding);
    String mainResource2Encoding = javaProject.getProject().getFolder(IPath.fromOSString("extra-resources")).getDefaultCharset();
    assertEquals("Encoding configured for plugin not set on folder", "ISO-8859-1", mainResource2Encoding);
    String testResource1Encoding = javaProject.getProject().getFolder(IPath.fromOSString("src/test/resources"))
        .getDefaultCharset();
    assertEquals("Encoding configured for plugin not set on folder", "ISO-8859-1", testResource1Encoding);
    String testResource2Encoding = javaProject.getProject().getFolder(IPath.fromOSString("extra-test-resources"))
        .getDefaultCharset();
    assertEquals("Encoding configured for plugin not set on folder", "ISO-8859-1", testResource2Encoding);

    copyContent(project, new File("projects/resourcesEncoding/p002/pom2.xml"), "pom.xml");

    MavenPlugin.getProjectConfigurationManager().updateProjectConfiguration(project, monitor);

    String mainResource1EncodingChanged = javaProject.getProject().getFolder(IPath.fromOSString("src/main/resources"))
        .getDefaultCharset();
    assertEquals("Encoding configured for plugin not set on folder", "UTF-16", mainResource1EncodingChanged);
    String mainResource2EncodingChanged = javaProject.getProject().getFolder(IPath.fromOSString("extra-resources"))
        .getDefaultCharset();
    assertEquals("Encoding configured for plugin not set on folder", "UTF-16", mainResource2EncodingChanged);
    String testResource1EncodingChanged = javaProject.getProject().getFolder(IPath.fromOSString("src/test/resources"))
        .getDefaultCharset();
    assertEquals("Encoding configured for plugin not set on folder", "UTF-16", testResource1EncodingChanged);
    String testResource2EncodingChanged = javaProject.getProject().getFolder(IPath.fromOSString("extra-test-resources"))
        .getDefaultCharset();
    assertEquals("Encoding configured for plugin not set on folder", "UTF-16", testResource2EncodingChanged);
  }

  @Test
  public void test003_shouldResetToContainerDefinedEncoding() throws Exception {
    IProject project = importProject("projects/resourcesEncoding/p003/pom.xml");
    IJavaProject javaProject = JavaCore.create(project);
    WorkspaceHelpers.assertNoErrors(project);

    String containerMainResourceEncoding = javaProject.getProject().getFolder(IPath.fromOSString("src/main/resources"))
        .getDefaultCharset();
    String containerTestResourceEncoding = javaProject.getProject().getFolder(IPath.fromOSString("src/test/resources"))
        .getDefaultCharset();

    copyContent(project, new File("projects/resourcesEncoding/p003/pom2.xml"), "pom.xml");

    MavenPlugin.getProjectConfigurationManager().updateProjectConfiguration(project, monitor);

    String mainResourceEncodingChanged = javaProject.getProject().getFolder(IPath.fromOSString("src/main/resources"))
        .getDefaultCharset();
    assertEquals("Encoding configured for plugin not set on folder", "ISO-8859-1", mainResourceEncodingChanged);
    String testResourceEncodingChanged = javaProject.getProject().getFolder(IPath.fromOSString("src/test/resources"))
        .getDefaultCharset();
    assertEquals("Encoding configured for plugin not set on folder", "UTF-16", testResourceEncodingChanged);

    copyContent(project, new File("projects/resourcesEncoding/p003/pom.xml"), "pom.xml");

    MavenPlugin.getProjectConfigurationManager().updateProjectConfiguration(project, monitor);

    String mainResourceEncodingReverted = javaProject.getProject().getFolder(IPath.fromOSString("src/main/resources"))
        .getDefaultCharset();
    assertEquals("Folder encoding not reverted to container defined", containerMainResourceEncoding,
        mainResourceEncodingReverted);
    String testResourceEncodingReverted = javaProject.getProject().getFolder(IPath.fromOSString("src/test/resources"))
        .getDefaultCharset();
    assertEquals("Folder encoding not reverted to container defined", containerTestResourceEncoding,
        testResourceEncodingReverted);
  }
}
