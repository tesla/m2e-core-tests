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

package org.eclipse.m2e.editor.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.junit.Test;


/**
 * @author atanasenko
 */
public class PluginConfigCompletionTest extends AbstractCompletionTest {

  IProject project;

  protected IFile loadProjectsAndFiles() throws Exception {
    // Create the projects
    setAutoBuilding(true);
    project = importProjects("projects/442560_plugin_content_assist", new String[] {"project/pom.xml"},
        new ResolverConfiguration())[0];
    waitForJobsToComplete();
    return (IFile) project.findMember("pom.xml");
  }
  
  private void initViewer() throws CoreException {
      IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().getProject(project);
      assertNotNull(facade);
      assertNotNull(facade.getMavenProject(monitor));
      sourceViewer.setMavenProject(facade.getMavenProject());
  }
  @Test
  public void testAllMojosCompletion() throws Exception {
      initViewer();
      
      String docString = sourceViewer.getDocument().get();
      int offset = docString.indexOf("<archive>");

      List<ICompletionProposal> proposals = getProposals(offset);
      
      // 32 parameter proposals, 2 = cdata and processing instr, 1 = wst xml editor thinks that you can also put <project> under <configuration>
      assertEquals("Proposal count", 32, proposals.size());
  }
  @Test
  public void testSingleMojoCompletion() throws Exception {
      initViewer();
      
      String docString = sourceViewer.getDocument().get();
      int offset = docString.indexOf("<detail/>");

      List<ICompletionProposal> proposals = getProposals(offset);
      assertEquals("Proposal count", 4, proposals.size());
  }
  @Test
  public void testNestedParameterCompletion() throws Exception {
      initViewer();
      
      String docString = sourceViewer.getDocument().get();
      int offset = docString.indexOf("<manifestSections>");

      List<ICompletionProposal> proposals = getProposals(offset);
      assertEquals("Proposal count", 10, proposals.size());
  }
  @Test
  public void testListItemNameCompletion() throws Exception {
      initViewer();
      
      String docString = sourceViewer.getDocument().get();
      int offset = docString.indexOf("<manifestSection>");

      List<ICompletionProposal> proposals = getProposals(offset);
      assertEquals("Proposal count", 1, proposals.size());
      
      assertEquals("List item proposal", "manifestSection", proposals.get(0).getDisplayString());
  }
  @Test
  public void testListItemParameterCompletion() throws Exception {
      initViewer();
      
      String docString = sourceViewer.getDocument().get();
      int offset = docString.indexOf("</manifestSection>");

      List<ICompletionProposal> proposals = getProposals(offset);
      assertEquals("Proposal count", 2, proposals.size());
      
      assertEquals("List item proposal", "manifestEntries", proposals.get(0).getDisplayString());
      assertEquals("List item proposal", "name", proposals.get(1).getDisplayString());
  }
  @Test
  public void testAnyListItemNameParameterCompletion() throws Exception {
      initViewer();
      
      String docString = sourceViewer.getDocument().get();
      int offset = docString.indexOf("</anyItemName>");

      List<ICompletionProposal> proposals = getProposals(offset);
      assertEquals("Proposal count", 2, proposals.size());
      
      assertEquals("List item proposal", "manifestEntries", proposals.get(0).getDisplayString());
      assertEquals("List item proposal", "name", proposals.get(1).getDisplayString());
  }
  @Test
  public void testUnknownContentCompletion() throws Exception {
      initViewer();
      
      String docString = sourceViewer.getDocument().get();
      int offset = docString.indexOf("<anything ");

      List<ICompletionProposal> proposals = getProposals(offset);
      assertEquals("Proposal count", 0, proposals.size());
  }
  @Test
  public void testImplementationContentCompletion() throws Exception {
      initViewer();
      
      String docString = sourceViewer.getDocument().get();
      int offset = docString.indexOf("</anything>");

      List<ICompletionProposal> proposals = getProposals(offset);
      assertEquals("Proposal count", 14, proposals.size());
  }
  @Test
  public void testImplementationListItemNameCompletion() throws Exception {
      initViewer();
      
      String docString = sourceViewer.getDocument().get();
      int offset = docString.indexOf("</availableVersions>");

      List<ICompletionProposal> proposals = getProposals(offset);
      assertEquals("Proposal count", 1, proposals.size());
      
      assertEquals("List item proposal", "availableVersion", proposals.get(0).getDisplayString());
  }
  @Test
  public void testSubclassListItems() throws Exception {
      initViewer();
      
      String docString = sourceViewer.getDocument().get();
      int offset = docString.indexOf("</modules>");

      List<ICompletionProposal> proposals = getProposals(offset);
      assertEquals("Proposal count", 12, proposals.size());
      
      assertEquals("Subclass item", "appClientModule", proposals.get(0).getDisplayString());
      assertEquals("Subclass item", "ejb3Module", proposals.get(1).getDisplayString());
      assertEquals("Subclass item", "ejbClientModule", proposals.get(2).getDisplayString());
      assertEquals("Subclass item", "ejbModule", proposals.get(3).getDisplayString());
      assertEquals("Subclass item", "harModule", proposals.get(4).getDisplayString());
      assertEquals("Subclass item", "jarModule", proposals.get(5).getDisplayString());
      assertEquals("Subclass item", "javaModule", proposals.get(6).getDisplayString());
      assertEquals("Subclass item", "parModule", proposals.get(7).getDisplayString());
      assertEquals("Subclass item", "rarModule", proposals.get(8).getDisplayString());
      assertEquals("Subclass item", "sarModule", proposals.get(9).getDisplayString());
      assertEquals("Subclass item", "webModule", proposals.get(10).getDisplayString());
      assertEquals("Subclass item", "wsrModule", proposals.get(11).getDisplayString());
  }
  @Test
  public void testM2ELifecycleMappingConfiguration() throws Exception {
      initViewer();
      
      String docString = sourceViewer.getDocument().get();
      
      int offset = docString.indexOf("</lifecycleMappingMetadata>");
      List<ICompletionProposal> proposals = getProposals(offset);
      assertEquals("Proposal count", 1, proposals.size());
      assertEquals("List item proposal", "pluginExecutions", proposals.get(0).getDisplayString());
      
      offset = docString.indexOf("</pluginExecutionFilter>");
      proposals = getProposals(offset);
      assertEquals("Proposal count", 4, proposals.size());

      offset = docString.indexOf("</execute>");
      proposals = getProposals(offset);
      assertEquals("Proposal count", 2, proposals.size());
      assertEquals("Execute proposal", "runOnConfiguration", proposals.get(0).getDisplayString());
      assertEquals("Execute proposal", "runOnIncremental", proposals.get(1).getDisplayString());

  }
  @Test
  public void testMetadataExtension() throws Exception {
      initViewer();
      
      String docString = sourceViewer.getDocument().get();
      int offset = docString.indexOf("<test1/>");

      List<ICompletionProposal> proposals = getProposals(offset);
      
      assertEquals("Proposal count", 2, proposals.size());
      assertEquals("Extension test1", "test1", proposals.get(0).getDisplayString());
      assertEquals("Extension test2", "test2", proposals.get(1).getDisplayString());
  }


}
