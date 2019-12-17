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

package org.eclipse.m2e.tests.builder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.junit.Test;

import org.eclipse.core.resources.IProject;

import org.codehaus.plexus.util.Scanner;

import org.eclipse.m2e.core.internal.builder.plexusbuildapi.EclipseBuildContext;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;


@SuppressWarnings("restriction")
public class EclipseBuildContextTest extends AbstractMavenProjectTestCase {
  @Test
  public void testScanner() throws Exception {
    IProject project = importProject("projects/MNGECLIPSE-2144/pom.xml");

    EclipseBuildContext context = newBuildContext(project);
    Scanner scanner;

    scanner = context.newScanner(new File(project.getLocation().toFile(), "src/main/resources"), true);
    checkScanner(scanner);

    scanner = context.newScanner(new File(project.getLocation().toFile(), "src/main/resources"), false);
    checkScanner(scanner);
  }

  private EclipseBuildContext newBuildContext(IProject project) {
    return new EclipseBuildContext(project, new HashMap<String, Object>(), new DummyBuildResultCollector());
  }

  private void checkScanner(Scanner scanner) {
    // both forward and backward slashes must be understood as separators
    scanner.setIncludes(new String[] {"sub/dir\\file.txt"});
    scanner.scan();

    List<String> included = Arrays.asList(scanner.getIncludedFiles());
    assertTrue(included.toString(), included.contains("sub" + File.separator + "dir" + File.separator + "file.txt"));
  }

  @Test
  public void test361038_buildContext_scan_nonExistingFolder() throws Exception {
    IProject project = importProject("projects/361038_buildContext_scan_nonExistingFolder/pom.xml");
    assertNoErrors(project);

    EclipseBuildContext context = newBuildContext(project);

    Scanner scanner = context.newScanner(new File(project.getLocation().toFile(), "doesnotexist"));

    scanner.setIncludes(new String[] {"**/*"});
    scanner.scan();

    assertEquals(0, scanner.getIncludedFiles().length);
  }

}
