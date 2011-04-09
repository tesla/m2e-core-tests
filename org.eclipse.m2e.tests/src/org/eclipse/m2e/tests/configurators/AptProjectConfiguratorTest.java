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

import junit.framework.Assert;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.apt.core.internal.ExtJarFactoryContainer;
import org.eclipse.jdt.apt.core.internal.util.FactoryContainer;
import org.eclipse.jdt.apt.core.internal.util.FactoryPath;
import org.eclipse.jdt.apt.core.util.AptConfig;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;


/**
 * Integration tests for {@link org.eclipse.m2e.jdt.apt.AptProjectConfigurator}.
 */
@SuppressWarnings("restriction")
public class AptProjectConfiguratorTest extends AbstractMavenProjectTestCase {

  /**
   * Imports the <code>projects/aptConfigurator/datanucleus</code> Maven project and ensures that APT is configured
   * correctly for it.
   */
  public void testSimpleConfiguration() throws Exception {
    IProject project = importProject("projects/aptConfigurator/datanucleus/pom.xml");
    IJavaProject javaProject = JavaCore.create(project);

    // Make sure that the generated sources folder is set in the APT config
    String aptGenSrcDir = AptConfig.getGenSrcDir(javaProject);
    Assert.assertTrue("Expected generated annotations source folder.", aptGenSrcDir.startsWith("target"));
    Assert.assertTrue("Expected generated annotations source folder.", aptGenSrcDir.contains("generated-sources"));
    Assert.assertTrue("Expected generated annotations source folder.", aptGenSrcDir.endsWith("annotations"));

    // Make sure the actual Eclipse class path contains the generated sources
    boolean generatedAnnotationsFound = false;
    for(IClasspathEntry cpe : javaProject.getRawClasspath()) {
      IPath cpePath = cpe.getPath();
      String cpePortablePath = cpePath.toPortableString();
      if(cpePortablePath.contains("target/generated-sources/annotations"))
        generatedAnnotationsFound = true;
    }
    Assert.assertTrue("Expected generated annotations source folder.", generatedAnnotationsFound);

    // Ensure that the APT factory path is set correctly
    boolean jdoQueryJarFound = false;
    FactoryPath factoryPath = (FactoryPath) AptConfig.getFactoryPath(javaProject);
    for(FactoryContainer container : factoryPath.getEnabledContainers().keySet()) {
      if(container instanceof ExtJarFactoryContainer) {
        ExtJarFactoryContainer extJarContainer = (ExtJarFactoryContainer) container;
        if(extJarContainer.getJarFile().getName().contains("datanucleus-jdo-query"))
          jdoQueryJarFound = true;
      }
    }
    Assert.assertTrue("Expected datanucleus-jdo-query in factory path.", jdoQueryJarFound);
  }
}
