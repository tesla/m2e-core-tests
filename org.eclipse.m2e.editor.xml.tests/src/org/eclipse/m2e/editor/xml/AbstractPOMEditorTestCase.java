/*******************************************************************************
 * Copyright (c) 2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.editor.xml;

import java.io.IOException;

import junit.framework.Assert;

import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.text.source.IOverviewRuler;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.exceptions.ResourceAlreadyExists;
import org.eclipse.wst.sse.core.internal.provisional.exceptions.ResourceInUse;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.ui.internal.StructuredTextViewer;
import org.eclipse.wst.xml.ui.StructuredTextViewerConfigurationXML;


public abstract class AbstractPOMEditorTestCase extends AbstractMavenProjectTestCase {
  protected DummyStructuredTextViewer sourceViewer;

  private IFile file;

  private IStructuredDocument document;

  private IStructuredModel model;

  protected void setUp() throws Exception {
    super.setUp();
    file = loadProjectsAndFiles();
    loadXMLFile();
    initializeSourceViewer();
  }

  protected abstract IFile loadProjectsAndFiles() throws Exception;

  protected void configureSourceViewer() {
    sourceViewer.configure(new StructuredTextViewerConfigurationXML());

    sourceViewer.setDocument(document);
  }

  protected void loadXMLFile() throws ResourceAlreadyExists, ResourceInUse, IOException, CoreException {
    IModelManager modelManager = StructuredModelManager.getModelManager();
    model = modelManager.getModelForEdit(file);
    document = model.getStructuredDocument();
  }

  protected void initializeSourceViewer() {
    // some test environments might not have a "real" display
    if(Display.getCurrent() != null) {

      Shell shell = null;
      Composite parent = null;

      if(PlatformUI.isWorkbenchRunning()) {
        shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
      } else {
        shell = new Shell(Display.getCurrent());
      }
      parent = new Composite(shell, SWT.NONE);

      // dummy viewer
      sourceViewer = new DummyStructuredTextViewer(parent, null, null, false, SWT.NONE);
      sourceViewer.setMavenProject(new MavenProject());
    } else {
      Assert.fail("Unable to run the test as a display must be available.");
    }

    configureSourceViewer();
  }
  
  class DummyStructuredTextViewer extends StructuredTextViewer implements IAdaptable {

    private MavenProject mavenProject;

    public DummyStructuredTextViewer(Composite parent, IVerticalRuler verticalRuler, IOverviewRuler overviewRuler,
        boolean showAnnotationsOverview, int styles) {
      super(parent, verticalRuler, overviewRuler, showAnnotationsOverview, styles);
      // TODO Auto-generated constructor stub
    }
    
    public void setMavenProject(MavenProject mp) {
      this.mavenProject = mp;
    }

    public Object getAdapter(Class adapter) {
      // TODO Auto-generated method stub
      if (MavenProject.class.equals(adapter)) {
        return mavenProject;
      }
      return null;
    }
    
  }

  protected void tearDown() throws Exception {
      super.tearDown();
      model.releaseFromEdit();
  }
  
  
}
