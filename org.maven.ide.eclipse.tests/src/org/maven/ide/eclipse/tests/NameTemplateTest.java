/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.tests;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.maven.ide.eclipse.internal.project.MavenProjectFacade.GAVPatternIterator;

import static org.maven.ide.eclipse.internal.project.MavenProjectFacade.getNamePattern;

/**
 * @author Anton Kraev
 */
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
    assertEquals("[groupId].[artifactId]", getNamePattern("org.apache.maven.maven", "org.apache.maven", "maven", "1"));
    assertEquals("[groupId].[artifactId][version]", getNamePattern("org.apache.maven.maven1", "org.apache.maven", "maven", "1"));
    assertEquals("[groupId].[artifactId]-foo", getNamePattern("org.apache.maven.maven-foo", "org.apache.maven", "maven", "1"));
    
    assertEquals("[artifactId]-[version]", getNamePattern("maven-1.0", "org.apache.maven", "maven", "1.0"));
    assertEquals("[artifactId]-TRUNK", getNamePattern("maven-TRUNK", "org.apache.maven", "maven", "1.0"));
    
    assertEquals("m[artifactId]", getNamePattern("mparent", "mine", "parent", "1"));
    assertEquals("[artifactId]", getNamePattern("nothing", "mine", "parent", "1"));

    assertEquals("[artifactId]", getNamePattern("org.maven.ide.eclipse", "org.maven.ide.eclipse", "org.maven.ide.eclipse", "0.9.6"));
    assertEquals("[artifactId]", getNamePattern("org.maven.ide.eclipse.jdt", "org.maven.ide.eclipse", "org.maven.ide.eclipse.jdt", "0.9.6"));
    assertEquals("[groupId].[artifactId]", getNamePattern("org.maven.ide.eclipse.org.maven.ide.eclipse.jdt", "org.maven.ide.eclipse", "org.maven.ide.eclipse.jdt", "0.9.6"));
    assertEquals("[artifactId]-[version]", getNamePattern("org.maven.ide.eclipse.jdt-0.9.6", "org.maven.ide.eclipse", "org.maven.ide.eclipse.jdt", "0.9.6"));
    assertEquals("[artifactId]-0.9.7", getNamePattern("org.maven.ide.eclipse.jdt-0.9.7", "org.maven.ide.eclipse", "org.maven.ide.eclipse.jdt", "0.9.6"));
    
    // make sure when values are the same, priority is A > G > V
    assertEquals("[artifactId]-[version]", getNamePattern("maven-SNAPSHOT-1.0.0", "maven", "maven", "SNAPSHOT-1.0.0"));
    assertEquals("[artifactId]-[version]", getNamePattern("maven-core-SNAPSHOT-1.0.0", "maven", "maven-core", "SNAPSHOT-1.0.0"));
  }
  
}

