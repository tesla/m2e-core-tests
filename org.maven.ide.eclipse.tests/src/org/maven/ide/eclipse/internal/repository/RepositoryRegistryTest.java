/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.repository;

import java.io.File;

import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.tests.AsbtractMavenProjectTestCase;


public class RepositoryRegistryTest extends AsbtractMavenProjectTestCase {

  private RepositoryRegistry repositoryRegistry;

  protected void setUp() throws Exception {
    super.setUp();

    File securityFile = new File("src/org/maven/ide/eclipse/internal/repository/settings-security.xml");
    System.setProperty("settings.security", securityFile.getAbsolutePath());

    repositoryRegistry = (RepositoryRegistry) MavenPlugin.getDefault().getRepositoryRegistry();
  }

  protected void tearDown() throws Exception {
    repositoryRegistry = null;

    super.tearDown();
  }

  private Server newServer(String id, String username, String password) {
    Server server = new Server();

    server.setId(id);
    server.setUsername(username);
    server.setPassword(password);

    return server;
  }

  public void testSettingsDecryption() {
    Settings settings = new Settings();
    settings.addServer(newServer("test", "user", "{9k2z8UPSVlYHp1+h2s05Qe4Zzpx46wGmzlGqJPgL3lQ=}"));

    AuthenticationInfo auth = repositoryRegistry.getAuthenticationInfo(settings, "test");

    assertEquals("user", auth.getUserName());
    assertEquals("pass", auth.getPassword());
  }

}
