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

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.eclipse.m2e.tests.common.WorkspaceHelpers;


public class CompilerSettingsWithEncodingConfiguratorTest extends AbstractMavenProjectTestCase {

  public void test001_shouldSetEncodingForBasicProject() throws Exception {
    IProject project = importProject("projects/compilerSettingsWithEncoding/p001/pom.xml");
    IJavaProject javaProject = JavaCore.create(project);
    WorkspaceHelpers.assertNoErrors(project);

    String encoding = javaProject.getProject().getFolder(new Path("src/main/java")).getDefaultCharset();
    assertEquals("Encoding should match", "ISO-8859-1", encoding);

    copyContent(project, new File("projects/compilerSettingsWithEncoding/p001/pom-UTF-16.xml"), "pom.xml");

    MavenPlugin.getProjectConfigurationManager().updateProjectConfiguration(project, monitor);

    String encodingChanged = javaProject.getProject().getFolder(new Path("src/main/java")).getDefaultCharset();
    assertEquals("Encoding (changed) should match", "UTF-16", encodingChanged);
  }

  public void test002_shouldResetToContainerDefinedEncoding() throws Exception {
    IProject project = importProject("projects/compilerSettingsWithEncoding/p002/pom.xml");
    IJavaProject javaProject = JavaCore.create(project);
    WorkspaceHelpers.assertNoErrors(project);

    String containerMainJavaEncoding = javaProject.getProject().getFolder(new Path("src/main/java"))
        .getDefaultCharset();
    String containerTestJavaEncoding = javaProject.getProject().getFolder(new Path("src/test/java"))
        .getDefaultCharset();

    copyContent(project, new File("projects/compilerSettingsWithEncoding/p002/pom2.xml"), "pom.xml");
    MavenPlugin.getProjectConfigurationManager().updateProjectConfiguration(project, monitor);

    String mainJavaEncodingChanged = javaProject.getProject().getFolder(new Path("src/main/java")).getDefaultCharset();
    assertEquals("Encoding configured for plugin not set on folder", "ISO-8859-1", mainJavaEncodingChanged);
    String testJavaEncodingChanged = javaProject.getProject().getFolder(new Path("src/test/java")).getDefaultCharset();
    assertEquals("Encoding configured for plugin not set on folder", "UTF-16", testJavaEncodingChanged);

    copyContent(project, new File("projects/compilerSettingsWithEncoding/p002/pom.xml"), "pom.xml");
    MavenPlugin.getProjectConfigurationManager().updateProjectConfiguration(project, monitor);

    String mainJavaEncodingReverted = javaProject.getProject().getFolder(new Path("src/main/java")).getDefaultCharset();
    assertEquals("Folder encoding not reverted to container defined", containerMainJavaEncoding,
        mainJavaEncodingReverted);
    String testJavaEncodingReverted = javaProject.getProject().getFolder(new Path("src/test/java")).getDefaultCharset();
    assertEquals("Folder encoding not reverted to container defined", containerTestJavaEncoding,
        testJavaEncodingReverted);
  }

  protected IFolder getFolder(IProject project, String absolutePath) {
    return project.getFolder(getProjectRelativePath(project, absolutePath));
  }

  protected IPath getProjectRelativePath(IProject project, String absolutePath) {
    File basedir = project.getLocation().toFile();
    String relative;
    if(absolutePath.equals(basedir.getAbsolutePath())) {
      relative = "."; //$NON-NLS-1$
    } else if(absolutePath.startsWith(basedir.getAbsolutePath())) {
      relative = absolutePath.substring(basedir.getAbsolutePath().length() + 1);
    } else {
      relative = absolutePath;
    }
    return new Path(relative.replace('\\', '/')); //$NON-NLS-1$ //$NON-NLS-2$
  }

}
