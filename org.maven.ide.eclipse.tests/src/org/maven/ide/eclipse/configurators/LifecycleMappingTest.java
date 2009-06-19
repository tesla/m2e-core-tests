
package org.maven.ide.eclipse.configurators;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.internal.project.CustomizableLifecycleMapping;
import org.maven.ide.eclipse.internal.project.GenericLifecycleMapping;
import org.maven.ide.eclipse.internal.project.MojoExecutionProjectConfigurator;
import org.maven.ide.eclipse.project.IMavenProjectFacade;
import org.maven.ide.eclipse.project.IProjectConfigurationManager;
import org.maven.ide.eclipse.project.MavenProjectManager;
import org.maven.ide.eclipse.project.ResolverConfiguration;
import org.maven.ide.eclipse.project.configurator.AbstractProjectConfigurator;
import org.maven.ide.eclipse.project.configurator.ILifecycleMapping;
import org.maven.ide.eclipse.tests.AsbtractMavenProjectTestCase;


public class LifecycleMappingTest extends AsbtractMavenProjectTestCase {
  
  private MavenProjectManager mavenProjectManager;
  private IProjectConfigurationManager projectConfigurationManager;

  protected void setUp() throws Exception {
    super.setUp();
    
    mavenProjectManager = MavenPlugin.getDefault().getMavenProjectManager();
    projectConfigurationManager = MavenPlugin.getDefault().getProjectConfigurationManager();
  }
  
  protected void tearDown() throws Exception {
    projectConfigurationManager = null;
    mavenProjectManager = null;

    super.tearDown();
  }

  public void testGenericMapping() throws Exception {
    ResolverConfiguration configuration = new ResolverConfiguration();
    IProject project1 = importProject("projects/lifecyclemapping/generic/pom.xml", configuration);
    waitForJobsToComplete();

    IMavenProjectFacade facade = mavenProjectManager.create(project1, monitor);

    ILifecycleMapping lifecycleMapping = projectConfigurationManager.getLifecycleMapping(facade, monitor);

    assertTrue( lifecycleMapping instanceof GenericLifecycleMapping );
  }

  public void testCustomizableMapping() throws Exception {
    ResolverConfiguration configuration = new ResolverConfiguration();
    IProject project1 = importProject("projects/lifecyclemapping/customizable/pom.xml", configuration);
    waitForJobsToComplete();

    IMavenProjectFacade facade = mavenProjectManager.create(project1, monitor);

    ILifecycleMapping lifecycleMapping = projectConfigurationManager.getLifecycleMapping(facade, monitor);

    assertTrue( lifecycleMapping instanceof CustomizableLifecycleMapping );
    
    List<AbstractProjectConfigurator> configurators = lifecycleMapping.getProjectConfigurators(facade, monitor);
    assertEquals(4, configurators.size());
    assertTrue(configurators.get(3) instanceof MojoExecutionProjectConfigurator);
  }
}
