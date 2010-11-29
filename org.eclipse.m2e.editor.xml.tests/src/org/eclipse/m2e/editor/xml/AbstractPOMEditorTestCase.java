
package org.eclipse.m2e.editor.xml;

import java.io.IOException;

import junit.framework.Assert;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.m2e.core.project.ResolverConfiguration;
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
  protected StructuredTextViewer sourceViewer;

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
      sourceViewer = new StructuredTextViewer(parent, null, null, false, SWT.NONE);
    } else {
      Assert.fail("Unable to run the test as a display must be available.");
    }

    configureSourceViewer();
  }

  protected void tearDown() throws Exception {
      super.tearDown();
      model.releaseFromEdit();
  }
}
