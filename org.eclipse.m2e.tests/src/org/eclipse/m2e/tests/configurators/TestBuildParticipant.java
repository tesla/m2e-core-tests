package org.eclipse.m2e.tests.configurators;

import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.project.configurator.AbstractBuildParticipant;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;


public class TestBuildParticipant extends AbstractBuildParticipant {
  public final MojoExecutionKey mojoExecutionKey;

  public TestBuildParticipant(MojoExecutionKey mojoExecutionKey) {
    this.mojoExecutionKey = mojoExecutionKey;
  }

  @Override
  public Set<IProject> build(int kind, IProgressMonitor monitor) throws Exception {
    return null;
  }

  @Override
  public String toString() {
    return "TestBuildParticipant(" + mojoExecutionKey + ")";
  }
}
