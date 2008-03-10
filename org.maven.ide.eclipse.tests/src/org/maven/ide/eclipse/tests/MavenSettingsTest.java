/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.tests;

import java.io.File;
import java.io.FileWriter;
import java.util.List;

import junit.framework.TestCase;

import org.apache.maven.embedder.ContainerCustomizer;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.embedder.MavenEmbedderException;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Repository;
import org.apache.maven.settings.Settings;
import org.maven.ide.eclipse.embedder.EmbedderFactory;
import org.maven.ide.eclipse.internal.embedder.ConsoleMavenEmbeddedLogger;


/**
 * @author Eugene Kuleshov
 */
public class MavenSettingsTest extends TestCase {

  protected void setUp() throws Exception {
    super.setUp();
  }
  
  public void testReadSettingsEnvVar() throws Exception {
    File settingsWithEnv = File.createTempFile("settings", "withEnv.xml");
    settingsWithEnv.deleteOnExit();
    
    FileWriter fw = new FileWriter(settingsWithEnv);
    if(isWindows()) {
      fw.write("<settings>\n" +
          "  <localRepository>${env.USERNAME}/m2.repository</localRepository>\n" +
          "  <mirrors>\n" + 
          "    <mirror>\n" + 
          "      <id>localhost mirror</id>\n" + 
          "      <name>My Local Repository</name>\n" + 
          "      <url>${localRepository}</url>\n" + 
          "      <mirrorOf>localhost</mirrorOf>\n" + 
          "    </mirror>\n" +
          "  </mirrors>\n" + 
      "</settings>");
    } else {
      fw.write("<settings>\n" +
      		"  <localRepository>${env.USER}/m2.repository</localRepository>\n" +
      		"  <mirrors>\n" + 
      		"    <mirror>\n" + 
      		"      <id>localhost mirror</id>\n" + 
      		"      <name>My Local Repository</name>\n" + 
      		"      <url>${localRepository}</url>\n" + 
      		"      <mirrorOf>localhost</mirrorOf>\n" + 
      		"    </mirror>\n" +
      		"  </mirrors>\n" + 
      		"</settings>");
    }
    fw.flush();
    fw.close();    
    
    MavenEmbedder embedder = getEmbedder(settingsWithEnv);

    Settings settings = embedder.getSettings();
    
    String expectedLocalRepository = System.getProperty("user.name") + "/m2.repository";
    
    String localRepository = settings.getLocalRepository();
    assertEquals(expectedLocalRepository, localRepository.replace('\\', '/'));
    
    String expectedLocalRepositoryDir = new File(expectedLocalRepository).getAbsolutePath();
    
    String localRepositoryDir = embedder.getLocalRepository().getBasedir();
    assertEquals(expectedLocalRepositoryDir, localRepositoryDir);
    
//    List mirrors = settings.getMirrors();
//    Mirror mirror = (Mirror) mirrors.get(0);
//    assertEquals("????", mirror.getUrl());
  }

  // XXX property substitution is not expanded by Maven
  public void XXX_testReadSettingsWithProperty() throws Exception {
    File settingsWithProperty = new File("src/org/maven/ide/eclipse/tests/settingsWithProperty.xml");
    
    MavenEmbedder embedder = getEmbedder(settingsWithProperty);
    
    Settings settings = embedder.getSettings();
    
    List profiles = settings.getProfiles();
    Profile profile = (Profile) profiles.get(0);
    List repositories = profile.getRepositories();
    Repository repository = (Repository) repositories.get(0);
    assertEquals("http://archiva.someserver.de/repository/releases", repository.getUrl());
  }
  
  private MavenEmbedder getEmbedder(File settingsFile) throws MavenEmbedderException {
    ContainerCustomizer customizer = EmbedderFactory.createExecutionCustomizer();
    
    ConsoleMavenEmbeddedLogger logger = new ConsoleMavenEmbeddedLogger(true);
    
    String userSettings = settingsFile.getAbsolutePath();
    
    return EmbedderFactory.createMavenEmbedder(customizer, null, null, userSettings);
  }
  
  private boolean isWindows() {
    String osName = System.getProperty("os.name", "").toLowerCase();
    return osName.indexOf("windows")>-1;
  }
  
}
