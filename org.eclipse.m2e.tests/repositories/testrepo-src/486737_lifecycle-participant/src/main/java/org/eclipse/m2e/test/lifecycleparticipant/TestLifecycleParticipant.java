package org.eclipse.m2e.test.lifecycleparticipant;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.building.DefaultModelBuilder;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelCache;
import org.apache.maven.project.MavenProject;

import java.io.File;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

@Component(role = AbstractMavenLifecycleParticipant.class, hint = "TestLifecycleParticipant")
public class TestLifecycleParticipant extends AbstractMavenLifecycleParticipant
{
  private static final String DUMMY_SOURCE_FOLDER = "dummySrc";

  @Requirement
  private ModelBuilder modelBuilder;

  @Override
  public void afterProjectsRead(MavenSession session) throws MavenExecutionException {
    MavenProject currentProject = session.getCurrentProject();
    File sourceFolder = new File(currentProject.getBasedir(), DUMMY_SOURCE_FOLDER);
    currentProject.getBuild().setSourceDirectory(sourceFolder.getAbsolutePath());
    currentProject.getCompileSourceRoots().set(0, sourceFolder.getAbsolutePath());
  }

}
