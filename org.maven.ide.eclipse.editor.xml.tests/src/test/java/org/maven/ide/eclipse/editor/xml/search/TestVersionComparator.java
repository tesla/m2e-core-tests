/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.editor.xml.search;

import static org.junit.Assert.assertTrue;

import org.junit.Test;


/**
 * @author Lukas Krecan
 */
public class TestVersionComparator {
  private VersionComparator comparator = new VersionComparator();

  @Test
  public void testCompareVersion() {
    assertGreater("2.0", "1.2");
    assertGreater("10.0", "2.2");
    assertGreater("2.2", "2.1-rc1");
    assertGreater("2.2-rc2", "2.2-rc1");
    assertGreater("2.2.5", "2.2");
    assertGreater("2.2.5", "2.2-rc1");
    assertGreater("2.2.5", "2.2-alpha1");
    assertGreater("2.2", "2.2-alpha1");
    assertGreater("1.1-rc1", "1.0");
  }

  private void assertGreater(String version1, String version2) {
    assertTrue(comparator.compare(version1, version2) < 0);
    assertTrue(comparator.compare(version2, version1) > 0);
  }
}
