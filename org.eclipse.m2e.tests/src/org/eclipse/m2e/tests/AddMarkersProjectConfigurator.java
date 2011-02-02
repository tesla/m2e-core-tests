
package org.eclipse.m2e.tests;

import java.io.File;
import java.util.Set;

import org.apache.maven.plugin.MojoExecution;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.internal.lifecycle.model.PluginExecutionMetadata;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.AbstractBuildParticipant;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;
import org.sonatype.plexus.build.incremental.BuildContext;
import org.sonatype.plexus.build.incremental.ThreadBuildContext;


public class AddMarkersProjectConfigurator extends AbstractProjectConfigurator {
  public static final String FILE_NAME = "foo.txt";

  public static final String ERROR_MESSAGE = "AddMarkersProjectConfigurator error ";

  public static final int ERROR_LINE_NUMBER = 2;

  public static final String WARNING_MESSAGE = "AddMarkersProjectConfigurator warning ";

  public static final int WARNING_LINE_NUMBER = 3;

  @Override
  public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException {
  }

  @Override
  public AbstractBuildParticipant getBuildParticipant(IMavenProjectFacade projectFacade, final MojoExecution mojoExecution,
                                                      PluginExecutionMetadata executionMetadata) {
    return new AbstractBuildParticipant() {
      public Set<IProject> build(int kind, IProgressMonitor monitor) throws Exception {
        String mojoExecutionKey = new MojoExecutionKey(mojoExecution).getKeyString();
        BuildContext buildContext = ThreadBuildContext.getContext();
        if(buildContext.isIncremental() && !mojoExecutionKey.contains("maven-install-plugin")) {
          return null;
        }

        IPath path = getMavenProjectFacade().getProject().getLocation().append(FILE_NAME);
        File file = path.toFile();

        buildContext.removeMessages(file);

        Exception warning = new Exception(WARNING_MESSAGE + " " + mojoExecutionKey + " " + System.currentTimeMillis()
            + System.nanoTime());
        buildContext.addMessage(file, WARNING_LINE_NUMBER, 1 /*column*/, null /*message*/,
            BuildContext.SEVERITY_WARNING, warning);

        Exception error = new Exception(ERROR_MESSAGE + " " + mojoExecutionKey + " " + System.currentTimeMillis()
            + System.nanoTime());
        buildContext.addMessage(file, ERROR_LINE_NUMBER, 1 /*column*/, null /*message*/, BuildContext.SEVERITY_ERROR,
            error);

        throw error;
      }
    };
  }
}
