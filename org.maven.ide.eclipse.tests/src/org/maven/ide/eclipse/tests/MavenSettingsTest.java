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

import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Repository;
import org.apache.maven.settings.Settings;
import org.maven.ide.eclipse.MavenPlugin;


/**
 * @author Eugene Kuleshov
 */
public class MavenSettingsTest extends TestCase {

  public void testReadSettingsEnvVar() throws Exception {
    File settingsWithEnv = File.createTempFile("settings", "withEnv.xml");
    settingsWithEnv.deleteOnExit();
    
    FileWriter fw = new FileWriter(settingsWithEnv);
    if(System.getProperty("os.name", "").toLowerCase().indexOf("windows")>-1) {
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
    
    settingsWithEnv.delete();
    
//    List mirrors = settings.getMirrors();
//    Mirror mirror = (Mirror) mirrors.get(0);
//    assertEquals("????", mirror.getUrl());
  }

  // XXX property substitution is not expanded by Maven
  public void XXX_testReadSettingsWithProperty() throws Exception {
    File settingsWithProperty = new File("src/org/maven/ide/eclipse/tests/settingsWithProperty.xml");
    
    MavenEmbedder embedder = getEmbedder(settingsWithProperty);
    
    Settings settings = embedder.getSettings();
    
    @SuppressWarnings("unchecked")
    List<Profile> profiles = settings.getProfiles();
    Profile profile = profiles.get(0);
    @SuppressWarnings("unchecked")
    List<Repository> repositories = profile.getRepositories();
    Repository repository = repositories.get(0);
    assertEquals("http://archiva.someserver.de/repository/releases", repository.getUrl());
  }
  
  public void testDefaultLocalRepositoryFromUserSettings() throws Exception {
    MavenEmbedderManager embedderManager = MavenPlugin.getDefault().getMavenEmbedderManager();
    ContainerCustomizer customizer = EmbedderFactory.createExecutionCustomizer();
    Configuration configuration = embedderManager.createDefaultConfiguration(customizer);
    // if(globalSettings!=null) {
    //   configuration.setGlobalSettingsFile(new File(globalSettings));
    // }
    
    File settings = new File("settingsWithDefaultLocalRepo.xml").getCanonicalFile();
    configuration.setUserSettingsFile(settings);
    
    MavenEmbedder embedder = EmbedderFactory.createMavenEmbedder(configuration, null);
    
    String localRepository = embedder.getLocalRepository().getBasedir();
    assertEquals(System.getProperty("user.home").replace('\\', '/') + "/.m2/repository", //
        localRepository.replace('\\', '/'));
  }
  
  public void testCustomLocalRepositoryFromUserSettings() throws Exception {
    File localrepoDir = new File("localrepo");

    File userSettings = File.createTempFile("settings", "withEnv.xml");
    userSettings.deleteOnExit();
    
    FileWriter fw = new FileWriter(userSettings);
    fw.write("<settings>\n" +
        "  <localRepository>" + localrepoDir.getAbsolutePath() + "</localRepository>\n" +
        "  <mirrors>\n" + 
        "    <mirror>\n" + 
        "      <id>localhost mirror</id>\n" + 
        "      <name>My Local Repository</name>\n" + 
        "      <url>${localRepository}</url>\n" + 
        "      <mirrorOf>localhost</mirrorOf>\n" + 
        "    </mirror>\n" +
        "  </mirrors>\n" + 
    "</settings>");
    fw.flush();
    fw.close();    
    
    MavenEmbedderManager embedderManager = MavenPlugin.getDefault().getMavenEmbedderManager();
    ContainerCustomizer customizer = EmbedderFactory.createExecutionCustomizer();
    Configuration configuration = embedderManager.createDefaultConfiguration(customizer);
    // if(globalSettings!=null) {
    //   configuration.setGlobalSettingsFile(new File(globalSettings));
    // }
    configuration.setUserSettingsFile(userSettings);
    
    MavenEmbedder embedder = EmbedderFactory.createMavenEmbedder(configuration, null);
    
    assertEquals(userSettings, embedder.getConfiguration().getUserSettingsFile());
    
    assertEquals(localrepoDir.getAbsolutePath(), embedder.getLocalRepository().getBasedir());
  }
  
  private MavenEmbedder getEmbedder(File settingsFile) throws MavenEmbedderException {
    ContainerCustomizer customizer = EmbedderFactory.createExecutionCustomizer();
    
    MavenEmbedderConsoleLogger logger = new MavenEmbedderConsoleLogger();
    logger.setThreshold(MavenEmbedderLogger.LEVEL_DEBUG);
    
    String userSettings = settingsFile.getAbsolutePath();
    
    return EmbedderFactory.createMavenEmbedder(customizer, logger, null, userSettings);
  }
  
}
