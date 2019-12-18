/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.tests.conversion;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.conversion.IProjectConversionEnabler;
import org.eclipse.m2e.core.project.conversion.IProjectConversionManager;


/**
 * ProjectConversionEnablerTest
 * 
 * @author Roberto Sanchez
 */
public class ProjectConversionEnablerTest extends AbstractProjectConversionTestCase {
  @Test
  public void testProjectCanBeConverted() throws Exception {
    // Looks for a project conversion enabler for project called foo. There are three project conversion enablers
    // configured for this project, each with different weight. We have to retrieve the enabler with the highest weight.
    // The enablers for this project will all say that it is OK to convert the project.

    final String projectName = "foo";

    deleteProject(projectName);
    //Import existing regular Eclipse project
    IProject project = createExisting(projectName, "projects/conversion/" + projectName);
    assertTrue(projectName + " was not created!", project.exists());
    assertNoErrors(project);

    IProjectConversionManager pcm = MavenPlugin.getProjectConversionManager();
    IProjectConversionEnabler en = pcm.getConversionEnablerForProject(project);
    String className = en.getClass().getSimpleName();
    assertEquals("The class found is incorrect:" + className, "FooEnabler70", className);
    assertNotNull(en.canBeConverted(project));
    assertTrue(en.canBeConverted(project).isOK());
    String[] packagings = en.getPackagingTypes(project);
    assertArrayEquals(FooEnablerBase.PACKAGING, packagings);
  }

  @Test
  public void testProjectCannotBeConverted() throws Exception {
    // Looks for a project conversion enabler for project called maven-layout. There are three project conversion enablers
    // configured for this project, each with different weight. We have to retrieve the enabler with the highest weight.
    // The enablers for this project will all say that it is NOT OK to convert the project.

    final String projectName = "maven-layout";

    deleteProject(projectName);
    //Import existing regular Eclipse project
    IProject project = createExisting(projectName, "projects/conversion/" + projectName);
    assertTrue(projectName + " was not created!", project.exists());
    assertNoErrors(project);

    IProjectConversionManager pcm = MavenPlugin.getProjectConversionManager();
    IProjectConversionEnabler en = pcm.getConversionEnablerForProject(project);
    String className = en.getClass().getSimpleName();
    assertEquals("The class found is incorrect:" + className, "MavenLayoutEnabler60", className);
    assertNotNull(en.canBeConverted(project));
    assertEquals(IStatus.ERROR, en.canBeConverted(project).getSeverity());
    assertEquals(MavenLayoutEnablerBase.ERR_MSG, en.canBeConverted(project).getMessage());
    String[] packagings = en.getPackagingTypes(project);
    assertArrayEquals(FooEnablerBase.PACKAGING, packagings);
  }

  @Test
  public void testEnablerNotFound() throws Exception {
    // Looks for a project conversion enabler for project called custom-layout. Since no enabler has been
    // configured for this project, the no enabler should be found.

    final String projectName = "custom-layout";

    deleteProject(projectName);
    //Import existing regular Eclipse project
    IProject project = createExisting(projectName, "projects/conversion/" + projectName);
    assertTrue(projectName + " was not created!", project.exists());
    assertNoErrors(project);

    IProjectConversionManager pcm = MavenPlugin.getProjectConversionManager();
    IProjectConversionEnabler en = pcm.getConversionEnablerForProject(project);
    assertNull("An enabler should have not be found", en);
  }

  // The following are the enablers used by the tests in this class.

  public static abstract class FooEnablerBase implements IProjectConversionEnabler {

    public static final String[] PACKAGING = {"jar", "war", "rar"};

    @Override
    public boolean accept(IProject project) {
      return (project != null && project.getName().equals("foo"));
    }

    @Override
    public IStatus canBeConverted(IProject project) {
      return Status.OK_STATUS;
    }

    @Override
    public String[] getPackagingTypes(IProject project) {
      return PACKAGING;
    }
  }

  public static class FooEnabler30 extends FooEnablerBase {
  }

  public static class FooEnablerDefault extends FooEnablerBase {
  }

  public static class FooEnabler70 extends FooEnablerBase {
  }

  public static abstract class MavenLayoutEnablerBase extends FooEnablerBase {

    public static final String ERR_MSG = "This project cannot be converted to Maven.";

    @Override
    public boolean accept(IProject project) {
      return (project != null && project.getName().equals("maven-layout"));
    }

    @Override
    public IStatus canBeConverted(IProject project) {
      return new Status(IStatus.ERROR, "org.eclipse.m2e.tests", ERR_MSG);

    }
  }

  public static class MavenLayoutEnabler40 extends MavenLayoutEnablerBase {
  }

  public static class MavenLayoutEnablerDefault extends MavenLayoutEnablerBase {
  }

  public static class MavenLayoutEnabler60 extends MavenLayoutEnablerBase {
  }

}
