/*************************************************************************************
 * Copyright (c) 2011-2014 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     JBoss by Red Hat - Initial implementation.
 ************************************************************************************/

package org.eclipse.m2e.tests.profiles;

import java.io.File;

import org.eclipse.core.resources.IProject;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.profiles.core.internal.IProfileManager;
import org.eclipse.m2e.profiles.core.internal.MavenProfilesCoreActivator;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;


public abstract class AbstractMavenProfileTest extends AbstractMavenProjectTestCase {

  protected IProfileManager profileManager;

  private String originalSettings;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    originalSettings = mavenConfiguration.getUserSettingsFile();
    mavenConfiguration.setUserSettingsFile(new File("settings_profiles.xml").getCanonicalPath());
    profileManager = MavenProfilesCoreActivator.getDefault().getProfileManager();
  }

  protected IMavenProjectFacade getFacade(IProject project) {
    return MavenPlugin.getMavenProjectRegistry().create(project.getFile(IMavenConstants.POM_FILE_NAME), true, monitor);
  }

  @Override
  protected void tearDown() throws Exception {
    profileManager = null;
    if(originalSettings != null) {
      mavenConfiguration.setUserSettingsFile(originalSettings);
    }
    super.tearDown();
  }

}
