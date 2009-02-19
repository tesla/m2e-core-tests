/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.integration.tests;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.JavaCore;


/**
 * @author rseddon
 */
public class MEclipse161ArchetypeProjectCreationTest extends UIIntegrationTestCase {

  private IProject createArchetypeProjct(String archetypeName) throws Exception {
    return createArchetypeProjct(archetypeName, "project");
  }
 
  public void testQuickStartCreate() throws Exception {
    IProject project = createArchetypeProjct("maven-archetype-quickstart");
    assertTrue(project.hasNature(JavaCore.NATURE_ID));
    IFile f = project.getFile("src/main/java/org/sonatype/test/project/App.java");
    assertTrue(f.exists());
    f = project.getFile("pom.xml");
    assertTrue(f.exists());
    f = project.getFile("src/test/java/org/sonatype/test/project/AppTest.java");
    assertTrue(f.exists());

  }

  public void testCreateMojo() throws Exception {
    createArchetypeProjct("maven-archetype-mojo");
  }

  public void testCreatePortlet() throws Exception {
    createArchetypeProjct("maven-archetype-portlet");
  }

  public void testCreateProfiles() throws Exception {
    createArchetypeProjct("maven-archetype-profiles");
  }

  public void testCreateSite() throws Exception {
    createArchetypeProjct("maven-archetype-site");
  }

  public void testCreateSiteSimple() throws Exception {
    createArchetypeProjct("maven-archetype-site-simple");
  }

  public void testCreateSiteWebapp() throws Exception {
    createArchetypeProjct("maven-archetype-webapp");
  }
  
  public void testCreateStrutsStarter() throws Exception {
    createArchetypeProjct("struts2-archetype-starter");
  }
  
  public void testCreateSpringWS() throws Exception {
    createArchetypeProjct("spring-ws-archetype");
  }
  
  public void createJ2EESimple() throws Exception {
    createArchetypeProjct("maven-archetype-j2ee-simple");
  }
}
