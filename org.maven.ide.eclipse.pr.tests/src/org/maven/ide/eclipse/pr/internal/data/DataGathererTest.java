/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.pr.internal.data;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.maven.ide.eclipse.tests.common.AbstractMavenProjectTestCase;


public class DataGathererTest extends AbstractMavenProjectTestCase {

  public void testRobustnessAgainstFailingGatherers() throws Exception {
    DataGatherer gatherer = new DataGatherer(null, null, null, null, null, null);
    List<File> files = gatherer.gather(null, EnumSet.allOf(Data.class), new NullProgressMonitor());
    for(File tmpFile : files) {
      tmpFile.deleteOnExit();
    }
    assertEquals(1, files.size());
    File tmpFile = files.get(0);

    assertTrue(tmpFile.isFile());
    ZipFile zip = new ZipFile(tmpFile);
    try {
      assertNotNull(zip.getEntry("pr/status-0.txt"));
      assertNotNull(zip.getEntry("pr/status-1.txt"));
    } finally {
      zip.close();
    }
  }

  public void testProjectSourcesGatheredInSeparateBundle() throws Exception {
    IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject("DataGathererTest");
    project.create(new NullProgressMonitor());
    try {
      project.open(new NullProgressMonitor());
      DataGatherer gatherer = new DataGatherer(null, null, null, null, Collections.singleton(project), null);
      List<File> files = gatherer.gather(null, EnumSet.allOf(Data.class), new NullProgressMonitor());
      for(File tmpFile : files) {
        tmpFile.deleteOnExit();
      }
      assertEquals(2, files.size());
      File tmpFile = files.get(1);

      assertTrue(tmpFile.isFile());
      ZipFile zip = new ZipFile(tmpFile);
      try {
        assertNotNull(zip.getEntry("projects/DataGathererTest/.project"));
      } finally {
        zip.close();
      }
    } finally {
      project.delete(true, true, new NullProgressMonitor());
    }
  }

  public void testSensitiveSettingsDataGetsObfuscated() throws Exception {
    String oldGlobalSettings = mavenConfiguration.getGlobalSettingsFile();
    String oldUserSettings = mavenConfiguration.getUserSettingsFile();
    try {
      mavenConfiguration.setUserSettingsFile(new File("settings/user-settings.xml").getAbsolutePath());
      mavenConfiguration.setGlobalSettingsFile(new File("settings/global-settings.xml").getAbsolutePath());
      DataGatherer gatherer = new DataGatherer(mavenConfiguration, null, null, null, null, null);
      List<File> files = gatherer.gather(null, EnumSet.of(Data.MAVEN_USER_SETTINGS, Data.MAVEN_GLOBAL_SETTINGS),
          new NullProgressMonitor());
      for(File tmpFile : files) {
        tmpFile.deleteOnExit();
      }

      assertEquals(1, files.size());
      File tmpFile = files.get(0);

      assertTrue(tmpFile.isFile());
      ZipFile zip = new ZipFile(tmpFile);
      try {
        ZipEntry ze;

        ze = zip.getEntry("config/global-settings.xml");
        assertNotNull(ze);
        String global = toString(zip.getInputStream(ze));
        assertFalse(global, global.contains("server-username"));
        assertFalse(global, global.contains("server-password"));
        assertFalse(global, global.contains("server-passphrase"));
        assertFalse(global, global.contains("proxy-username"));
        assertFalse(global, global.contains("proxy-password"));

        ze = zip.getEntry("config/user-settings.xml");
        assertNotNull(ze);
        String user = toString(zip.getInputStream(ze));
        assertFalse(user, user.contains("server-username"));
        assertFalse(user, user.contains("server-password"));
        assertFalse(user, user.contains("server-passphrase"));
        assertFalse(user, user.contains("proxy-username"));
        assertFalse(user, user.contains("proxy-password"));
      } finally {
        zip.close();
      }
    } finally {
      mavenConfiguration.setUserSettingsFile(oldUserSettings);
      mavenConfiguration.setGlobalSettingsFile(oldGlobalSettings);
    }

  }

  private String toString(InputStream is) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
    for(byte[] buffer = new byte[1024 * 4];;) {
      int n = is.read(buffer);
      if(n < 0) {
        break;
      }
      baos.write(buffer, 0, n);
    }
    return baos.toString("UTF-8");
  }

  public void testBundleEncryption() throws Exception {
    URL publicKey = getClass().getResource("/apr/public-key.txt");
    assertNotNull(publicKey);
    URL privateKey = getClass().getResource("/apr/private-key.txt");
    assertNotNull(privateKey);

    DataGatherer gatherer = new DataGatherer(null, null, null, ResourcesPlugin.getWorkspace(), null, publicKey);
    List<File> files = gatherer.gather(null, EnumSet.of(Data.ECLIPSE_LOG), new NullProgressMonitor());
    files = gatherer.decryptBundles(files, null, privateKey);
    for(File tmpFile : files) {
      tmpFile.deleteOnExit();
    }
    assertEquals(1, files.size());
    File tmpFile = files.get(0);

    assertTrue(tmpFile.isFile());
    ZipFile zip = new ZipFile(tmpFile);
    try {
      assertNotNull(zip.getEntry("config/error.log"));
    } finally {
      zip.close();
    }
  }

}
