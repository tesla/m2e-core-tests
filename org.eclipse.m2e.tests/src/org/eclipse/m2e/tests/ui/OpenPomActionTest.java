/*******************************************************************************
 * Copyright (c) 2014 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Takari, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.tests.ui;

import org.eclipse.core.resources.IProject;
import org.eclipse.ui.IEditorPart;

import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.ui.internal.actions.OpenPomAction;
import org.eclipse.m2e.editor.pom.MavenPomEditor;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;


public class OpenPomActionTest extends AbstractMavenProjectTestCase {

  public void test454759_openEditor() throws Exception {
    IProject project = importProject("projects/454759_openEditor/pom.xml");

    waitForJobsToComplete();

    IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().getProject(project);
    MavenProject mavenProject = facade.getMavenProject(monitor);

    OpenPomAction action = new OpenPomAction() {
      protected void openDialog(String msg) {
        // don't open any dialogs
      }
    };

    IEditorPart editor = action.openPomEditor("test", "b", "1.0", null, monitor);
    assertNull(editor);

    editor = action.openPomEditor("test", "b", "1.0", mavenProject, monitor);
    assertNotNull(editor);
    assertTrue(editor instanceof MavenPomEditor);
  }

}
