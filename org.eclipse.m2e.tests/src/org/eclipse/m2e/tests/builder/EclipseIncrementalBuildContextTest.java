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

import org.eclipse.core.resources.IProject;

import org.codehaus.plexus.util.Scanner;

import org.eclipse.m2e.core.internal.builder.plexusbuildapi.EclipseIncrementalBuildContext;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.eclipse.m2e.tests.mocks.ResourceDeltaStub;


@SuppressWarnings("restriction")
public class EclipseIncrementalBuildContextTest extends AbstractMavenProjectTestCase {

  public void testScanner() throws Exception {
    IProject project = importProject("projects/MNGECLIPSE-2144/pom.xml");

    ResourceDeltaStub delta = new ResourceDeltaStub(project);
    ResourceDeltaStub child = delta.addChild(new ResourceDeltaStub(project.getFolder("src")));
    child = child.addChild(new ResourceDeltaStub(project.getFolder("src/main")));
    child = child.addChild(new ResourceDeltaStub(project.getFolder("src/main/resources")));
    child = child.addChild(new ResourceDeltaStub(project.getFolder("src/main/resources/sub")));
    child = child.addChild(new ResourceDeltaStub(project.getFolder("src/main/resources/sub/dir")));
    child = child.addChild(new ResourceDeltaStub(project.getFile("src/main/resources/sub/dir/file.txt")));

    EclipseIncrementalBuildContext context = newBuildContext(delta);
    Scanner scanner;

    scanner = context.newScanner(new File(project.getLocation().toFile(), "src/main/resources"), true);
    checkScanner(scanner);

    scanner = context.newScanner(new File(project.getLocation().toFile(), "src/main/resources"), false);
    checkScanner(scanner);
  }

  private EclipseIncrementalBuildContext newBuildContext(ResourceDeltaStub delta) {
    return new EclipseIncrementalBuildContext(delta, new HashMap<String, Object>(), new DummyBuildResultCollector());
  }

  private void checkScanner(Scanner scanner) {
    // both forward and backward slashes must be understood as separators
    scanner.setIncludes(new String[] {"sub/dir\\file.txt"});
    scanner.scan();

    List<String> included = Arrays.asList(scanner.getIncludedFiles());
    assertTrue(included.toString(), included.contains("sub" + File.separator + "dir" + File.separator + "file.txt"));
  }

}
