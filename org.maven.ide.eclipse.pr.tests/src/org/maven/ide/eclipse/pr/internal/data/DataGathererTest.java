/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.pr.internal.data;

import java.io.File;
import java.util.EnumSet;
import java.util.zip.ZipFile;

import org.eclipse.core.runtime.NullProgressMonitor;

import junit.framework.TestCase;


public class DataGathererTest extends TestCase {

  public void testRobustnessAgainstFailingGatherers() throws Exception {
    File tmpFile = File.createTempFile("datagatherertest-", "-" + getName());
    tmpFile.deleteOnExit();

    DataGatherer gatherer = new DataGatherer(null, null, null, null, null);
    gatherer.gather(tmpFile.getAbsolutePath(), EnumSet.allOf(Data.class), new NullProgressMonitor());

    assertTrue(tmpFile.isFile());
    ZipFile zip = new ZipFile(tmpFile);
    try {
      assertNotNull(zip.getEntry("pr/status-0.txt"));
      assertNotNull(zip.getEntry("pr/status-1.txt"));
    } finally {
      zip.close();
    }
  }

}
