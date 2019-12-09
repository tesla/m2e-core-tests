/*******************************************************************************
 * Copyright (c) 2010 Sonatype, Inc.
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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.text.source.IOverviewRuler;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.ui.internal.StructuredTextViewer;
import org.eclipse.wst.xml.ui.StructuredTextViewerConfigurationXML;
import org.apache.maven.project.MavenProject;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.eclipse.m2e.tests.common.RequireMavenExecutionContext;


@SuppressWarnings("restriction")
@RequireMavenExecutionContext
public abstract class AbstractPOMEditorTestCase extends AbstractMavenProjectTestCase {
  protected DummyStructuredTextViewer sourceViewer;

  private IFile file;

  private IStructuredDocument document;

  private IStructuredModel model;

  @Override
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

  protected void loadXMLFile() throws Exception {
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
      fail("Unable to run the test as a display must be available.");
    }

    configureSourceViewer();
  }

  class DummyStructuredTextViewer extends StructuredTextViewer implements IAdaptable {

    private MavenProject mavenProject;

    public DummyStructuredTextViewer(Composite parent, IVerticalRuler verticalRuler, IOverviewRuler overviewRuler,
        boolean showAnnotationsOverview, int styles) {
      super(parent, verticalRuler, overviewRuler, showAnnotationsOverview, styles);
    }

    public void setMavenProject(MavenProject mp) {
      this.mavenProject = mp;
    }

    public <T> T getAdapter(Class<T> adapter) {
      if(MavenProject.class.equals(adapter)) {
        return adapter.cast(mavenProject);
      }
      return null;
    }
  }

  @Override
  protected void tearDown() throws Exception {
    try {
      model.releaseFromEdit();
    } finally {
      super.tearDown();
    }
  }
}
