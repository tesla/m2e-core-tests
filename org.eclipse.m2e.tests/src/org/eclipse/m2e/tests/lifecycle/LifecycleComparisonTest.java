
package org.eclipse.m2e.tests.lifecycle;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.m2e.core.internal.lifecyclemapping.LifecycleMappingFactory;
import org.eclipse.m2e.core.internal.lifecyclemapping.LifecycleMappingResult;
import org.eclipse.m2e.core.internal.project.LifecycleMappingConfiguration;
import org.eclipse.m2e.core.internal.project.registry.MavenProjectFacade;
import org.eclipse.m2e.tests.common.AbstractLifecycleMappingTest;


@SuppressWarnings("restriction")
public class LifecycleComparisonTest extends AbstractLifecycleMappingTest {

  public void testSameProject() throws Exception {
    IProject project = createExisting("LifecycleComparisonTest", "projects/lifecyclemapping/lifecyclecomparison");
    LifecycleMappingConfiguration facadeA = LifecycleMappingConfiguration.newLifecycleMappingConfiguration(
        newMavenProjectFacade(project, "pom.xml"), monitor);
    MavenProjectFacade facadeB = newMavenProjectFacade(project, "pom.xml");

    assertFalse(LifecycleMappingFactory.isLifecycleMappingChanged(facadeB, facadeA, monitor));
  }

  public void testPluginVersionChange() throws Exception {
    IProject project = createExisting("LifecycleComparisonTest", "projects/lifecyclemapping/lifecyclecomparison");
    LifecycleMappingConfiguration facadeA = LifecycleMappingConfiguration.newLifecycleMappingConfiguration(
        newMavenProjectFacade(project, "pom.xml"), monitor);
    MavenProjectFacade facadeB = newMavenProjectFacade(project, "pom-plugin-version-change.xml");

    assertTrue(LifecycleMappingFactory.isLifecycleMappingChanged(facadeB, facadeA, monitor));
  }

  public void testPluginConfigurationChange() throws Exception {
    IProject project = createExisting("LifecycleComparisonTest", "projects/lifecyclemapping/lifecyclecomparison");
    LifecycleMappingConfiguration facadeA = LifecycleMappingConfiguration.newLifecycleMappingConfiguration(
        newMavenProjectFacade(project, "pom.xml"), monitor);
    MavenProjectFacade facadeB = newMavenProjectFacade(project, "pom-plugin-configuration-change.xml");

    assertTrue(LifecycleMappingFactory.isLifecycleMappingChanged(facadeB, facadeA, monitor));
  }

  protected MavenProjectFacade newMavenProjectFacade(IProject project, String path) throws CoreException {
    MavenProjectFacade facade = super.newMavenProjectFacade(project.getFile(path));
    LifecycleMappingResult mappingResult = calculateLifecycleMapping(facade);
    assertFalse(mappingResult.getProblems().toString(), mappingResult.hasProblems());
    facade.setLifecycleMappingId(mappingResult.getLifecycleMappingId());
    facade.setMojoExecutionMapping(mappingResult.getMojoExecutionMapping());
    return facade;
  }
}
