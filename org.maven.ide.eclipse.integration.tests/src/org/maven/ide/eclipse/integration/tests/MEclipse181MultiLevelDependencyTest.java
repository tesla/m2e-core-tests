/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.integration.tests;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.junit.Assert;
import org.junit.Test;


/**
 * @author Administrator
 */
public class MEclipse181MultiLevelDependencyTest extends M2EUIIntegrationTestCase {

  private IProject createDependentProject(IProject parent, String childName) throws Exception {
    IProject childProject = createArchetypeProject("maven-archetype-quickstart", childName);

    waitForAllBuildsToComplete();
    
    addDependency(parent.getName(), "org.sonatype.test", childName, "0.0.1-SNAPSHOT");
    
    waitForAllBuildsToComplete();
    IJavaProject jp = (IJavaProject) parent.getNature(JavaCore.NATURE_ID);
    Assert.assertTrue("classpath dependency for "+ childName + " not found", childName.equals(jp.getRequiredProjectNames()[0]));
    return childProject;
  }
  

  @Test
  public void testMultiLevelDependencies() throws Exception {
    IProject project = createArchetypeProject("maven-archetype-quickstart", "multiProject0");
    //dropped it to three to speed things up
    for (int i = 1; i < 4; i++) {
      project = createDependentProject(project, "multiProject" + i);
    }
  }
}
