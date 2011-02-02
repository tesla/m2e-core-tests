
package org.eclipse.m2e.tests.lifecycle;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.m2e.core.internal.lifecycle.LifecycleMappingFactory;
import org.eclipse.m2e.core.internal.lifecycle.LifecycleMappingResult;
import org.eclipse.m2e.core.internal.project.registry.MavenProjectFacade;
import org.eclipse.m2e.tests.common.AbstractLifecycleMappingTest;


@SuppressWarnings("restriction")
public class LifecycleComparisonTest extends AbstractLifecycleMappingTest {

  public void testSameProject() throws CoreException {
    MavenProjectFacade facadeA = newMavenProjectFacade("pom.xml");
    MavenProjectFacade facadeB = newMavenProjectFacade("pom.xml");

    assertFalse(LifecycleMappingFactory.isLifecycleMappingChanged(facadeA, facadeB, monitor));
  }

  public void testPluginVersionChange() throws CoreException {
    MavenProjectFacade facadeA = newMavenProjectFacade("pom.xml");
    MavenProjectFacade facadeB = newMavenProjectFacade("pom-plugin-version-change.xml");

    assertTrue(LifecycleMappingFactory.isLifecycleMappingChanged(facadeA, facadeB, monitor));
  }

  public void testPluginConfigurationChange() throws CoreException {
    MavenProjectFacade facadeA = newMavenProjectFacade("pom.xml");
    MavenProjectFacade facadeB = newMavenProjectFacade("pom-plugin-configuration-change.xml");

    assertTrue(LifecycleMappingFactory.isLifecycleMappingChanged(facadeA, facadeB, monitor));
  }

  protected MavenProjectFacade newMavenProjectFacade(String path) throws CoreException {
    MavenProjectFacade facade = super.newMavenProjectFacade("projects/lifecyclemapping/lifecyclecomparison/" + path);
    LifecycleMappingResult mappingResult = LifecycleMappingFactory.calculateLifecycleMapping(plugin.getMaven()
        .createExecutionRequest(monitor), facade, monitor);
    assertFalse(mappingResult.getProblems().toString(), mappingResult.hasProblems());
    facade.setLifecycleMappingId(mappingResult.getLifecycleMappingId());
    facade.setMojoExecutionMapping(mappingResult.getMojoExecutionMapping());
    return facade;
  }
}
