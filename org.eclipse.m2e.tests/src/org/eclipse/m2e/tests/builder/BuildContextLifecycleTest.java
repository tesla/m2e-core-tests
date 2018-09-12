
package org.eclipse.m2e.tests.builder;

import org.junit.Test;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;

import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;


public class BuildContextLifecycleTest extends AbstractMavenProjectTestCase {

  @Test
  public void testBuildContextRefreshProject() throws Exception {
    IProject project = importProject("projects/testProjectBuildContext/pom.xml");
    int[] initialTestResourcesCount = new int[1];
    project.accept(res -> {
      if(res.getType() == IResource.FILE && res.getName().startsWith("refreshTest")) {
        initialTestResourcesCount[0]++ ;
      }
      return res.getType() == IResource.PROJECT;
    }, IResource.DEPTH_ONE);

    project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);

    project.accept(res -> {
      if(res.getType() == IResource.FILE && res.getName().startsWith("refreshTest")) {
        initialTestResourcesCount[0]-- ;
      }
      return res.getType() == IResource.PROJECT;
    }, IResource.DEPTH_ONE);

    assertEquals("Created resource was not made visible", -1, initialTestResourcesCount[0]);
  }
}
