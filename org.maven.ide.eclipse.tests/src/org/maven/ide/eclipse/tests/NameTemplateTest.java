package org.maven.ide.eclipse.tests;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.maven.ide.eclipse.internal.project.MavenProjectFacade;
import org.maven.ide.eclipse.internal.project.MavenProjectFacade.GAVPatternIterator;

public class NameTemplateTest extends TestCase {
  public void testNameTemplates() throws Exception {
    // test that there are exactly 15 unique patterns when all values are different
    GAVPatternIterator iterator = new GAVPatternIterator("1", "2", "3");
    List<String> list = new ArrayList<String>();
    while (iterator.hasNext()) {
      String p = iterator.next().pattern(); 
      if (!list.contains(p))
        list.add(p);
    }
    assertEquals(15, list.size());
    
    // test different patterns
    assertEquals("[groupId].[artifactId]", MavenProjectFacade.getNamePattern("org.apache.maven.maven", "org.apache.maven", "maven", "1"));
    assertEquals("[groupId].[artifactId][version]", MavenProjectFacade.getNamePattern("org.apache.maven.maven1", "org.apache.maven", "maven", "1"));
    assertEquals("m[artifactId]", MavenProjectFacade.getNamePattern("mparent", "mine", "parent", "1"));
    assertEquals("[artifactId]", MavenProjectFacade.getNamePattern("nothing", "mine", "parent", "1"));
    
    //make sure when values are the same, priority is A > G > V
    assertEquals("[artifactId]-[version]", MavenProjectFacade.getNamePattern("maven-SNAPSHOT-1.0.0", "maven", "maven", "SNAPSHOT-1.0.0"));
  }
}
