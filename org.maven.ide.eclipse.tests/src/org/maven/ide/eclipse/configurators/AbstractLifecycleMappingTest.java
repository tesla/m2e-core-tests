package org.maven.ide.eclipse.configurators;

import org.eclipse.core.resources.IProject;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.project.IMavenProjectFacade;
import org.maven.ide.eclipse.project.IProjectConfigurationManager;
import org.maven.ide.eclipse.project.MavenProjectManager;
import org.maven.ide.eclipse.project.ResolverConfiguration;
import org.maven.ide.eclipse.tests.AsbtractMavenProjectTestCase;

public abstract class AbstractLifecycleMappingTest extends AsbtractMavenProjectTestCase {
  protected MavenProjectManager mavenProjectManager;
  protected IProjectConfigurationManager projectConfigurationManager;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    
    mavenProjectManager = MavenPlugin.getDefault().getMavenProjectManager();
    projectConfigurationManager = MavenPlugin.getDefault().getProjectConfigurationManager();
  }
  
  @Override
  protected void tearDown() throws Exception {
    projectConfigurationManager = null;
    mavenProjectManager = null;

    super.tearDown();
  }
  
  protected IMavenProjectFacade importMavenProject(String pomName) throws Exception {
    ResolverConfiguration configuration = new ResolverConfiguration();
    IProject project = importProject(pomName, configuration);
    waitForJobsToComplete();

    return mavenProjectManager.create(project, monitor);
  }
}