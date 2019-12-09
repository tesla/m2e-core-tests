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

package org.eclipse.m2e.tests.project;

import java.io.File;

import junit.framework.TestCase;

import org.eclipse.m2e.core.project.MavenProjectInfo;


public class MavenProjectInfoTest extends TestCase {
  public void testTwoChildProjectInfosWithSameLabel() throws Exception {
    File baseDir = new File("projects/mavenprojectinfo/twochildrensamelabel");
    MavenProjectInfo parent = new MavenProjectInfo("parent", new File(baseDir, "pom.xml"), null /*model*/, null /*parent*/);
    MavenProjectInfo child1 = new MavenProjectInfo("child", new File(baseDir, "child1/pom.xml"), null /*model*/,
        parent);
    parent.add(child1);
    MavenProjectInfo child2 = new MavenProjectInfo("child", new File(baseDir, "child2/pom.xml"), null /*model*/,
        parent);
    parent.add(child2);
    assertEquals(2, parent.getProjects().size());
  }
}
