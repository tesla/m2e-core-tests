package org.maven.ide.eclipse.project;

import java.io.File;

import junit.framework.TestCase;

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
