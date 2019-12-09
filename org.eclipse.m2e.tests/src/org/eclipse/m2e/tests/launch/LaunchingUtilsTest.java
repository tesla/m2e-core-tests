/*******************************************************************************
 * Copyright (c) 2015 Konrad Windszus.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Konrad Windszus - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.tests.launch;

import java.io.File;
import java.io.IOException;

import org.junit.Assert;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;

import org.eclipse.m2e.internal.launch.LaunchingUtils;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;


public class LaunchingUtilsTest extends AbstractMavenProjectTestCase {

  public void testGenerateProjectLocationVariableExpression() throws IOException, CoreException {
    IProject project = createExisting("simpleproject", "projects/simple-pom");
    String projectLocationWithVariable = LaunchingUtils.generateProjectLocationVariableExpression(project);

    String projectLocation = LaunchingUtils.substituteVar(projectLocationWithVariable);
    Assert.assertEquals(project.getLocation().toOSString(), projectLocation);
    // now move the projects location: the new location must be outside of the workspace folder, otherwise 
    // it is implicitly assumed this is an existing project in the workspace already!
    moveExistingProjectLocation(project,
        new File(workspace.getRoot().getLocation().toFile(), "../someotherprojectfolder"));
    String newProjectLocation = LaunchingUtils.substituteVar(projectLocationWithVariable);
    Assert.assertNotEquals(newProjectLocation, projectLocation);
    // the resolved project location should still be valid
    Assert.assertEquals(project.getLocation().toOSString(), newProjectLocation);
  }

  /**
   * Similar to {@link #createExisting}, but moves the existing project to a different location while keeping the same
   * project name.
   * 
   * @param project the current project.
   * @param projectTargetFolder the target folder to which the project's content should be moved (must be outside of the
   *          workspace folder)
   */
  protected void moveExistingProjectLocation(IProject project, File projectTargetFolder)
      throws CoreException, IOException {
    copyDir(project.getLocation().toFile(), projectTargetFolder);
    workspace.run(new IWorkspaceRunnable() {
      public void run(IProgressMonitor monitor) throws CoreException {
        // delete old project
        project.delete(true, monitor);
        // create new project with same name but in a different location
        IProjectDescription projectDescription = workspace.newProjectDescription(project.getName());
        IPath location = Path.fromOSString(projectTargetFolder.getAbsolutePath());
        projectDescription.setLocation(location);
        project.create(projectDescription, monitor);
        project.open(IResource.NONE, monitor);
      }
    }, null);
  }
}
