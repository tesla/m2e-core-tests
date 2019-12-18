
package org.eclipse.m2e.tests;

import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;

import org.apache.maven.plugin.MojoExecution;

import org.eclipse.m2e.core.lifecyclemapping.model.IPluginExecutionMetadata;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.AbstractBuildParticipant;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;


public class ThrowBuildExceptionProjectConfigurator extends AbstractProjectConfigurator {
  public static final String ERROR_MESSAGE = "ThrowBuildExceptionProjectConfigurator exception ";

  @Override
  public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) {
  }

  @Override
  public AbstractBuildParticipant getBuildParticipant(IMavenProjectFacade projectFacade, MojoExecution execution,
      IPluginExecutionMetadata executionMetadata) {
    return new AbstractBuildParticipant() {
      @Override
      public Set<IProject> build(int kind, IProgressMonitor monitor) throws Exception {
        throw new Exception(ERROR_MESSAGE + System.currentTimeMillis() + System.nanoTime());
      }
    };
  }
}
