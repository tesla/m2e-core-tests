/*******************************************************************************
 * Copyright (c) 2013 Igor Fedorenko
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Igor Fedorenko - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.tests.launch;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.eclipse.core.resources.IProject;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.internal.launch.MavenLauncherConfigurationHandler;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;


public class MavenLauncherConfigurationHandlerTest extends AbstractMavenProjectTestCase {

  public void test421015_workspaceProjectLibraryClasspathEntries() throws Exception {
    IProject project = importProject("projects/421015_workspaceProjectLibraryClasspathEntries/pom.xml");
    IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().create(project, monitor);
    MavenLauncherConfigurationHandler cfg = new MavenLauncherConfigurationHandler();
    cfg.addRealm("realm");
    cfg.addProjectEntry(facade);
    List<String> entries = cfg.getRealmEntries("realm");
    assertEquals(2, entries.size());
    assertEquals(project.getFolder("target/classes").getLocation().toOSString(), entries.get(0));
    assertEquals(project.getFolder("lib-classes").getLocation().toOSString(), entries.get(1));
  }

}
