/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.builder;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.codehaus.plexus.util.Scanner;
import org.eclipse.core.resources.IProject;
import org.maven.ide.eclipse.tests.common.AbstractMavenProjectTestCase;


@SuppressWarnings("restriction")
public class EclipseBuildContextTest extends AbstractMavenProjectTestCase {

  public void testScanner() throws Exception {
    IProject project = importProject("projects/MNGECLIPSE-2144/pom.xml");

    EclipseBuildContext context = new EclipseBuildContext(project, new HashMap<String, Object>());
    Scanner scanner = context.newScanner(new File(project.getLocation().toFile(), "src/main/resources"), true);
    // both forward and backward slashes must be understood as separators
    scanner.setIncludes(new String[] {"sub/dir\\file.txt"});
    scanner.scan();

    List<String> included = Arrays.asList(scanner.getIncludedFiles());
    assertTrue(included.toString(), included.contains("sub" + File.separator + "dir" + File.separator + "file.txt"));
  }

}
