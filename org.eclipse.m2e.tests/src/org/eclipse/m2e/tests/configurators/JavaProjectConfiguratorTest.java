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

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;


public class JavaProjectConfiguratorTest extends AbstractMavenProjectTestCase {

  public void testMNGECLIPSE2313_markAllRawClasspathEntries() throws Exception {
    IProject project = importProject("projects/MNGECLIPSE-2313_markAllRawClasspathEntries/pom.xml");
    IJavaProject javaProject = JavaCore.create(project);

    for(IClasspathEntry cpe : javaProject.getRawClasspath()) {
      assertHasAttribute(MNGECLIPSE2313MarkAllRawClasspathEntriesConfigurator.ATTR, cpe);
    }
  }

  private void assertHasAttribute(IClasspathAttribute expected, IClasspathEntry cpe) {
    IClasspathAttribute[] attrs = cpe.getExtraAttributes();
    assertNotNull(attrs);

    for(IClasspathAttribute attr : attrs) {
      if(expected.equals(attr)) {
        return;
      }
    }

    fail("Expected classpath attribute " + expected.toString() + " for " + cpe.toString());
  }

}
