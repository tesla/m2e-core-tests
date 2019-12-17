/*******************************************************************************
 * Copyright (c) 2016 Anton Tanasenko
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Anton Tanasenko - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.editor.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.ResolverConfiguration;


/**
 * @author atanasenko
 */
@SuppressWarnings("restriction")
public class FileCompletionTest extends AbstractCompletionTest {

  IProject[] projects;

  protected IFile loadProjectsAndFiles() throws Exception {
    // deliberately import only a subset of projects
    projects = importProjects("projects/489755_file_content_assist", new String[] {"parent1/pom.xml", "parent2/pom.xml", "parent1/module1/pom.xml"},
        new ResolverConfiguration());
    waitForJobsToComplete();
    return (IFile) projects[0].findMember("pom.xml");
  }

  public void testModuleCompletion() throws Exception {
    // Get the location of the place where we want to start the completion
    String docString = sourceViewer.getDocument().get();
    IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().getProject(projects[0]);
    assertNotNull(facade);
    assertNotNull(facade.getMavenProject(monitor));
    sourceViewer.setMavenProject(facade.getMavenProject());
    
    // modules not already added
    {
      int offset = docString.indexOf("#mod1");
      List<ICompletionProposal> proposals = getProposals(offset);
      assertEquals(3, proposals.size());
      assertTrue(proposals.get(0) instanceof PomTemplateProposal);
      assertTrue(proposals.get(1) instanceof PomTemplateProposal);
      assertTrue(proposals.get(2) instanceof PomTemplateProposal);
      assertEquals("module1", ((PomTemplateProposal) proposals.get(0)).getDisplayString());
      assertEquals("module2", ((PomTemplateProposal) proposals.get(1)).getDisplayString());
      assertEquals("module1/", ((PomTemplateProposal) proposals.get(2)).getDisplayString());
    }
    
    // from parent dir
    {
      int offset = docString.indexOf("#mod2");
      List<ICompletionProposal> proposals = getProposals(offset);
      assertEquals(2, proposals.size());
      assertTrue(proposals.get(0) instanceof PomTemplateProposal);
      assertTrue(proposals.get(1) instanceof PomTemplateProposal);
      assertEquals("../parent2", ((PomTemplateProposal) proposals.get(0)).getDisplayString());
      assertEquals("../parent3", ((PomTemplateProposal) proposals.get(1)).getDisplayString());
    }
    
    // profile modules
    {
      int offset = docString.indexOf("#mod3");
      List<ICompletionProposal> proposals = getProposals(offset);
      assertEquals(2, proposals.size());
      assertTrue(proposals.get(0) instanceof PomTemplateProposal);
      assertEquals("module1", ((PomTemplateProposal) proposals.get(0)).getDisplayString());
      assertEquals("module1/", ((PomTemplateProposal) proposals.get(1)).getDisplayString());
    }
  }
  
  public void testFilesAndDirsCompletion() throws Exception {
    // Get the location of the place where we want to start the completion
    String docString = sourceViewer.getDocument().get();
    IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().getProject(projects[0]);
    assertNotNull(facade);
    assertNotNull(facade.getMavenProject(monitor));
    sourceViewer.setMavenProject(facade.getMavenProject());
    
    // dirs only
    {
      int offset = docString.indexOf("#src");
      List<ICompletionProposal> proposals = getProposals(offset);
      assertEquals(2, proposals.size());
      assertTrue(proposals.get(0) instanceof PomTemplateProposal);
      assertTrue(proposals.get(1) instanceof PomTemplateProposal);
      assertEquals("main", ((PomTemplateProposal) proposals.get(0)).getDisplayString());
      assertEquals("test", ((PomTemplateProposal) proposals.get(1)).getDisplayString());
    }
    
    // property interpolation
    {
      int offset = docString.indexOf("#target");
      List<ICompletionProposal> proposals = getProposals(offset);
      assertEquals(1, proposals.size());
      assertTrue(proposals.get(0) instanceof PomTemplateProposal);
      assertEquals("classes", ((PomTemplateProposal) proposals.get(0)).getDisplayString());
    }
    
    // both files and dirs
    {
      int offset = docString.indexOf("#filters");
      List<ICompletionProposal> proposals = getProposals(offset);
      assertEquals(3, proposals.size());
      assertTrue(proposals.get(0) instanceof PomTemplateProposal);
      assertTrue(proposals.get(1) instanceof PomTemplateProposal);
      assertTrue(proposals.get(2) instanceof PomTemplateProposal);
      assertEquals("filtersfolder", ((PomTemplateProposal) proposals.get(0)).getDisplayString());
      assertEquals("filters1.properties", ((PomTemplateProposal) proposals.get(1)).getDisplayString());
      assertEquals("filters2.properties", ((PomTemplateProposal) proposals.get(2)).getDisplayString());
    }
  }

}
