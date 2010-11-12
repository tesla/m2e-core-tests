/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.tests.builder;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.codehaus.plexus.util.Scanner;
import org.eclipse.core.resources.IProject;
import org.eclipse.m2e.core.builder.EclipseBuildContext;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;


@SuppressWarnings("restriction")
public class EclipseBuildContextTest extends AbstractMavenProjectTestCase {

  public void testScanner() throws Exception {
    IProject project = importProject("projects/MNGECLIPSE-2144/pom.xml");

    EclipseBuildContext context = new EclipseBuildContext(project, new HashMap<String, Object>());
    Scanner scanner;

    scanner = context.newScanner(new File(project.getLocation().toFile(), "src/main/resources"), true);
    checkScanner(scanner);

    scanner = context.newScanner(new File(project.getLocation().toFile(), "src/main/resources"), false);
    checkScanner(scanner);
  }

  private void checkScanner(Scanner scanner) {
    // both forward and backward slashes must be understood as separators
    scanner.setIncludes(new String[] {"sub/dir\\file.txt"});
    scanner.scan();

    List<String> included = Arrays.asList(scanner.getIncludedFiles());
    assertTrue(included.toString(), included.contains("sub" + File.separator + "dir" + File.separator + "file.txt"));
  }

}
