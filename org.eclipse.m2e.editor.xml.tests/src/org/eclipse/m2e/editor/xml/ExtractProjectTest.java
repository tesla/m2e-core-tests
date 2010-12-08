/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.editor.xml;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.editor.xml.internal.XmlUtils;

/**
 * Hello fellow tester:
 * everytime this test finds a regression add an 'x' here:
 * everytime you do mindless test update add an 'y' here: 
 * @author mkleint
 *
 */

public class ExtractProjectTest extends AbstractCompletionTest {
  
  private IProject[] projects;
  
  protected IFile loadProjectsAndFiles() throws Exception {
    //Create the projects
    projects = importProjects("projects/extractProject", 
        new String[] {
        "parent/pom.xml",
        "parent/Anested1/pom.xml",
        "parent/Znested2/pom.xml"
        //there is also Xnested3 but we don't open that one..
        }, new ResolverConfiguration());
    waitForJobsToComplete();
    return (IFile) projects[1].findMember("pom.xml");
  }

  //NOTE: this test is no extensive and doesn't cover scenario when the project is located outside of the workspace
  public void testExtractProject() throws Exception {
    IProject prj = XmlUtils.extractProject(sourceViewer);
    assertNotNull(prj);
    assertEquals(projects[1], prj);
    Job job = new Job("XXX") {
      
      protected IStatus run(IProgressMonitor monitor) {
        try {
          projects[1].delete(false, true, monitor);
        } catch(CoreException ex) {
          // TODO Auto-generated catch block
          ex.printStackTrace();
          fail();
        }
        return Status.OK_STATUS;
      }
    };
    job.schedule();
    job.join();
    prj = XmlUtils.extractProject(sourceViewer);
    assertNull(prj);
  }

}
