/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.tests.internal.repository;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.eclipse.core.runtime.CoreException;

import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.wagon.authentication.AuthenticationInfo;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.repository.RepositoryRegistry;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;


public class RepositoryRegistryTest extends AbstractMavenProjectTestCase {

  private RepositoryRegistry repositoryRegistry;

  @Before
  public void setUp() throws Exception {
    super.setUp();

    File securityFile = new File("src/org/eclipse/m2e/tests/internal/repository/settings-security.xml");
    System.setProperty("settings.security", securityFile.getAbsolutePath());

    repositoryRegistry = (RepositoryRegistry) MavenPlugin.getRepositoryRegistry();
  }

  @After
  public void tearDown() throws Exception {
    try {
      repositoryRegistry = null;
    } finally {
      super.tearDown();
    }
  }

  private Server newServer(String id, String username, String password) {
    Server server = new Server();

    server.setId(id);
    server.setUsername(username);
    server.setPassword(password);

    return server;
  }

  @Test
  public void testSettingsDecryption() throws CoreException {
    Settings settings = new Settings();
    settings.addServer(newServer("test", "user", "{9k2z8UPSVlYHp1+h2s05Qe4Zzpx46wGmzlGqJPgL3lQ=}"));

    AuthenticationInfo auth = repositoryRegistry.getAuthenticationInfo(settings, "test");

    assertEquals("user", auth.getUserName());
    assertEquals("pass", auth.getPassword());
  }

}
