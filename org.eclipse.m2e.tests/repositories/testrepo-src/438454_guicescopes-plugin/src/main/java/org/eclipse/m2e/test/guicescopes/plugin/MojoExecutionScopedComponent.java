
package org.eclipse.m2e.test.guicescopes.plugin;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.scope.MojoExecutionScoped;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;


@Named
@MojoExecutionScoped
public class MojoExecutionScopedComponent {

  @Inject
  public MojoExecutionScopedComponent(MavenSession session, MavenProject project, MojoExecution execution) {
  }

}
