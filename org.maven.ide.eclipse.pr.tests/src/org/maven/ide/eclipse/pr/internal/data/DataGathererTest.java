/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.pr.internal.data;

import java.io.File;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;

import junit.framework.TestCase;


public class DataGathererTest extends TestCase {

  public void testRobustnessAgainstFailingGatherers() throws Exception {
    DataGatherer gatherer = new DataGatherer(null, null, null, null, null);
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
      DataGatherer gatherer = new DataGatherer(null, null, null, null, Collections.singleton(project));
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

}
