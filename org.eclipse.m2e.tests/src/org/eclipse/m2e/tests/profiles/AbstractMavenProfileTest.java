/*************************************************************************************
 * Copyright (c) 2011-2014 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     JBoss by Red Hat - Initial implementation.
 ************************************************************************************/

package org.eclipse.m2e.tests.profiles;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;

import com.google.inject.Inject;

import org.eclipse.core.resources.IProject;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.profiles.core.internal.IProfileManager;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.eclipse.m2e.tests.common.OSGiServiceInjector;


public abstract class AbstractMavenProfileTest extends AbstractMavenProjectTestCase {

  @Rule
  public OSGiServiceInjector serviceInjector = OSGiServiceInjector.INSTANCE;

  @Inject
  protected IProfileManager profileManager;

  private String originalSettings;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    originalSettings = mavenConfiguration.getUserSettingsFile();
    mavenConfiguration.setUserSettingsFile(new File("settings_profiles.xml").getCanonicalPath());
  }

  protected IMavenProjectFacade getFacade(IProject project) {
    return MavenPlugin.getMavenProjectRegistry().create(project.getFile(IMavenConstants.POM_FILE_NAME), true, monitor);
  }

  @Override
  @After
  public void tearDown() throws Exception {
    if(originalSettings != null) {
      mavenConfiguration.setUserSettingsFile(originalSettings);
    }
    super.tearDown();
  }

}
