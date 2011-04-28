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

package org.eclipse.m2e.tests.configurators;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.eclipse.m2e.tests.common.WorkspaceHelpers;

/**
 * Tests of settings file encoding for Maven project resource folders based on encoding
 * configuration of maven-resources-plugin.
 */
public class ResourcesEncodingConfiguratorTest extends AbstractMavenProjectTestCase {

  public void test001_shouldConfigureEncodingForBasicProject() throws Exception {
    IProject project = importProject("projects/resourcesEncoding/p001/pom.xml");
    IJavaProject javaProject = JavaCore.create(project);
    WorkspaceHelpers.assertNoErrors(project);

    String mainResourceEncoding = javaProject.getProject().getFolder(new Path("src/main/resources")).getDefaultCharset();
    assertEquals("Encoding configured for plugin not set on folder", "ISO-8859-1", mainResourceEncoding);
    String testResourceEncoding = javaProject.getProject().getFolder(new Path("src/test/resources")).getDefaultCharset();
    assertEquals("Encoding configured for plugin not set on folder", "ISO-8859-1", testResourceEncoding);

    copyContent(project, new File("projects/resourcesEncoding/p001/pom2.xml"), "pom.xml");
    
    MavenPlugin.getProjectConfigurationManager().updateProjectConfiguration(project, monitor);

    String mainResourceEncodingChanged = javaProject.getProject().getFolder(new Path("src/main/resources")).getDefaultCharset();
    assertEquals("Encoding configured for plugin not set on folder", "UTF-16", mainResourceEncodingChanged);
    String testResourceEncodingChanged = javaProject.getProject().getFolder(new Path("src/test/resources")).getDefaultCharset();
    assertEquals("Encoding configured for plugin not set on folder", "UTF-16", testResourceEncodingChanged);
  }
}
