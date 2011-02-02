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
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;
import org.sonatype.plexus.build.incremental.BuildContext;
import org.sonatype.plexus.build.incremental.ThreadBuildContext;


public class AddMarkersProjectConfiguratorFoo extends AbstractProjectConfigurator {
  public static final String FILE_NAME = "foo.txt";

  public static final String FILE_NAME1 = "foo1.txt";

  public static final String ERROR_MESSAGE = "AddMarkersProjectConfiguratorFoo error ";

  public static final int ERROR_LINE_NUMBER = 2;

  public static final String WARNING_MESSAGE = "AddMarkersProjectConfiguratorFoo warning ";

  public static final int WARNING_LINE_NUMBER = 3;

  @Override
  public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException {
  }

  @Override
  public AbstractBuildParticipant getBuildParticipant(IMavenProjectFacade projectFacade, MojoExecution execution,
                                                      PluginExecutionMetadata executionMetadata) {
    return new AbstractBuildParticipant() {
      public Set<IProject> build(int kind, IProgressMonitor monitor) throws Exception {
        Exception error = doBuild(FILE_NAME);
        Exception error1 = doBuild(FILE_NAME1);
        if(error != null) {
          throw error;
        }
        if(error1 != null) {
          throw error1;
        }
        return null;
      }

      private Exception doBuild(String filename) {
        BuildContext buildContext = ThreadBuildContext.getContext();
        IPath path = getMavenProjectFacade().getProject().getLocation().append(filename);
        File file = path.toFile();
        if(buildContext.isIncremental() && !buildContext.hasDelta(file)) {
          return null;
        }
        if(!file.exists()) {
          return null;
        }

        buildContext.removeMessages(file);

        Exception warning = new Exception(WARNING_MESSAGE + filename + " " + System.currentTimeMillis()
            + System.nanoTime());
        buildContext.addMessage(file, WARNING_LINE_NUMBER, 1 /*column*/, null /*message*/,
            BuildContext.SEVERITY_WARNING, warning);

        Exception error = new Exception(ERROR_MESSAGE + filename + " " + System.currentTimeMillis() + System.nanoTime());
        buildContext.addMessage(file, ERROR_LINE_NUMBER, 1 /*column*/, null /*message*/, BuildContext.SEVERITY_ERROR,
            error);

        return error;
      }
    };
  }
}
