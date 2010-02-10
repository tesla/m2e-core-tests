
package org.maven.ide.eclipse.editor.pom;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Writer;
import java.util.List;

import org.apache.maven.model.Model;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotEditor;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.ui.StructuredTextEditor;
import org.junit.Before;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.core.IMavenConstants;
import org.maven.ide.eclipse.integration.tests.common.UIIntegrationTestCase;
import org.maven.ide.eclipse.project.IProjectConfigurationManager;
import org.maven.ide.eclipse.project.ProjectImportConfiguration;


@SuppressWarnings("restriction")
public class PomEditorTestBase extends UIIntegrationTestCase {

  protected static final String TEST_POM_POM_XML = "test-pom/pom.xml";

  protected static final String TAB_POM_XML = null;

  protected static final String TAB_OVERVIEW = IMavenConstants.PLUGIN_ID + ".pom.overview";

  static final String TAB_DEPENDENCIES = IMavenConstants.PLUGIN_ID + ".pom.dependencies";

  static final String TAB_REPOSITORIES = IMavenConstants.PLUGIN_ID + ".pom.repositories";

  static final String TAB_BUILD = IMavenConstants.PLUGIN_ID + ".pom.build";

  static final String TAB_PLUGINS = IMavenConstants.PLUGIN_ID + ".pom.plugins";

  static final String TAB_REPORTING = IMavenConstants.PLUGIN_ID + ".pom.reporting";

  static final String TAB_PROFILES = IMavenConstants.PLUGIN_ID + ".pom.profiles";

  static final String TAB_TEAM = IMavenConstants.PLUGIN_ID + ".pom.team";

  static final String TAB_DEPENDENCY_TREE = IMavenConstants.PLUGIN_ID + ".pom.dependencyTree";

  static final String TAB_DEPENDENCY_GRAPH = IMavenConstants.PLUGIN_ID + ".pom.dependencyGraph";

  protected static final String PROJECT_NAME = "test-pom";

  public static void setContents(File aFile, String aContents) throws Exception {
    Writer output = new BufferedWriter(new FileWriter(aFile));
    output.write(aContents);
    output.flush();
    output.close();
  }

//  protected void setUp() {
//    ShellFinder.bringRootToFront(getActivePage().getWorkbenchWindow().getShell().getDisplay());
//  }
  protected void tearDown() throws Exception {

//    IConditionMonitor monitor = (IConditionMonitor) getUI().getAdapter(IConditionMonitor.class);
//    monitor.add(new ShellShowingCondition("Save Resource(s)?"), //
//      new IHandler() {
//        public void handle(IUIContext ui) {
//          try {
//            ui.click(new ButtonLocator("(Yes|OK)"));
//          } catch(WidgetSearchException ex) {
//            // ignore
//            ex.printStackTrace();
//          }
//        }
//      });
//    
//    // ui.close(new CTabItemLocator("\\*?" + TEST_POM_POM_XML));
//    getUI().keyClick(SWT.CTRL, 's');
//    Thread.sleep(500L);
//    getUI().keyClick(SWT.CTRL, 'w');
  }

  @Before
  public void createTestProject() {

    clearProjects();

    List<? extends SWTBotEditor> editors = bot.editors();
    for(SWTBotEditor e : editors) {
      e.saveAndClose();
    }

    final IProjectConfigurationManager configurationManager = MavenPlugin.getDefault().getProjectConfigurationManager();

    final Model model = new Model();
    model.setModelVersion("4.0.0");
    model.setGroupId("org.foo");
    model.setArtifactId("test-pom");
    model.setVersion("1.0.0");

    //ResourcesPlugin.getWorkspace().getRoot().refreshLocal(IResource.DEPTH_INFINITE, null);

    final IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(PROJECT_NAME);

    WorkspaceJob job = new WorkspaceJob("creating test project") {
      public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
        configurationManager.createSimpleProject(project, null, model, new String[0], new ProjectImportConfiguration(),
            new NullProgressMonitor());
        return Status.OK_STATUS;
      }
    };

    job.setRule(ResourcesPlugin.getWorkspace().getRuleFactory().buildRule());
    job.schedule();

    waitForAllBuildsToComplete();
  }

  protected void createFile(String name, String content) throws Exception {
    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    IFile file = root.getFile(new Path(name));
    file.create(new ByteArrayInputStream(content.getBytes("UTF-8")), true, null);
    waitForAllBuildsToComplete();
  }

  protected void selectEditorTab(final String id) throws Exception {
    final MavenPomEditor editor = (MavenPomEditor) getActivePage().getActiveEditor();
    Display.getDefault().syncExec(new Runnable() {
      public void run() {
        editor.setActivePage(id);
      }
    });
//    getUI().wait(new SWTIdleCondition());
  }

  protected String getContents(File aFile) throws Exception {
    StringBuilder contents = new StringBuilder();

    BufferedReader input = new BufferedReader(new FileReader(aFile));
    String line = null; //not declared within while loop
    while((line = input.readLine()) != null) {
      contents.append(line);
      contents.append(System.getProperty("line.separator"));
    }
    return contents.toString();
  }

  protected void delete(final String startMarker, final String endMarker) {
    final MavenPomEditor editor = (MavenPomEditor) getActivePage().getActiveEditor();
    final StructuredTextEditor[] sse = new StructuredTextEditor[1];
    Display.getDefault().syncExec(new Runnable() {
      public void run() {
        sse[0] = (StructuredTextEditor) editor.getActiveEditor();

        IStructuredDocument structuredDocument = (IStructuredDocument) sse[0].getDocumentProvider().getDocument(
            sse[0].getEditorInput());
        String text = structuredDocument.get();
        int pos1 = text.indexOf(startMarker);
        int pos2 = text.indexOf(endMarker);
        if(pos1 > -1 && pos2 > -1) {
          structuredDocument.replaceText(this, pos1, (pos2 + endMarker.length()) - pos1, null);
        }
      }
    });

//      IDocument structuredDocument = sse[0].getDocumentProvider().getDocument(sse[0].getEditorInput());
//      String text = structuredDocument.get();
//      int pos1 = text.indexOf(startMarker); 
//      int pos2 = text.indexOf(endMarker);
//      text = text.substring(0, pos1) + text.substring(pos2 + endMarker.length());
//      structuredDocument.set(text);
//      getUI().wait(new SWTIdleCondition());
  }

  protected void assertTextValue(String id, String value) {
    assertEquals("Expecting input text value", value, bot.textWithName(id).getText());
  }

  protected void setTextValue(String id, String value) {
    SWTBotText text = bot.textWithName(id);
    text.setFocus();
    text.setText(value);
  }

  ISelectionProvider getSelectionProvider() {
    return getEditorSite().getSelectionProvider();
  }

  private IEditorSite getEditorSite() {
    IEditorPart editor = getActivePage().getActiveEditor();
    IEditorSite editorSite = editor.getEditorSite();
    return editorSite;
  }

  protected String getEditorText() {
    final String[] texts = new String[1];
    Display.getDefault().syncExec(new Runnable() {
      public void run() {
        final MavenPomEditor editor = (MavenPomEditor) getActivePage().getActiveEditor();
        texts[0] = editor.getSourcePage().getTextViewer().getTextWidget().getText();
//        try {
//          IWidgetLocator[] loc = getUI().findAll(new SWTWidgetLocator(StyledText.class));
//          for(int i = 0; i < loc.length; i++ ) {
//            WidgetReference ref = (WidgetReference) getUI().find(loc[i]);
//            texts[0] = ((StyledText) ref.getWidget()).getText();
//            if(texts[0].contains("<project"))
//              break;
//          }
//        } catch(WidgetSearchException ex) {
//          ex.printStackTrace();
//          ex.printStackTrace();
//        }
      }
    });
    return texts[0];
  }

  protected void expandSectionIfRequired(String sectionName, String sectionLabel) {
    bot.section(sectionLabel).expand();
  }

  protected void collapseSectionIfRequired(String sectionName, String sectionLabel) {
    bot.section(sectionLabel).collapse();
  }

}
