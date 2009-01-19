
package org.maven.ide.eclipse.editor.pom;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Writer;

import org.apache.maven.model.Model;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPerspectiveRegistry;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPreferenceConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.internal.IPreferenceConstants;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.internal.util.PrefUtil;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.wst.sse.ui.StructuredTextEditor;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.core.IMavenConstants;
import org.maven.ide.eclipse.project.IProjectConfigurationManager;
import org.maven.ide.eclipse.project.ProjectImportConfiguration;

import com.windowtester.finder.swt.ShellFinder;
import com.windowtester.runtime.IUIContext;
import com.windowtester.runtime.WT;
import com.windowtester.runtime.WaitTimedOutException;
import com.windowtester.runtime.WidgetSearchException;
import com.windowtester.runtime.condition.HasTextCondition;
import com.windowtester.runtime.condition.IConditionMonitor;
import com.windowtester.runtime.condition.IHandler;
import com.windowtester.runtime.locator.IWidgetLocator;
import com.windowtester.runtime.locator.WidgetReference;
import com.windowtester.runtime.swt.UITestCaseSWT;
import com.windowtester.runtime.swt.condition.SWTIdleCondition;
import com.windowtester.runtime.swt.condition.eclipse.JobsCompleteCondition;
import com.windowtester.runtime.swt.condition.shell.ShellDisposedCondition;
import com.windowtester.runtime.swt.condition.shell.ShellShowingCondition;
import com.windowtester.runtime.swt.locator.ButtonLocator;
import com.windowtester.runtime.swt.locator.CTabItemLocator;
import com.windowtester.runtime.swt.locator.NamedWidgetLocator;
import com.windowtester.runtime.swt.locator.SWTWidgetLocator;
import com.windowtester.runtime.util.ScreenCapture;

@SuppressWarnings("unchecked")
public class PomEditorTestBase extends UITestCaseSWT {

  private static final String FIND_REPLACE = "Find/Replace";
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
  protected IUIContext ui;
  protected IWorkspaceRoot root;
  IWorkspace workspace;

  public static void setContents(File aFile, String aContents) throws Exception {
    Writer output = new BufferedWriter(new FileWriter(aFile));
    output.write(aContents);
    output.flush();
    output.close();
  }

  public PomEditorTestBase() {
    super();
  }

  public PomEditorTestBase(String testName) {
    super(testName);
  }

  public PomEditorTestBase(Class launchClass) {
    super(launchClass);
  }

  public PomEditorTestBase(String testName, Class launchClass) {
    super(testName, launchClass);
  }

  public PomEditorTestBase(Class launchClass, String[] launchArgs) {
    super(launchClass, launchArgs);
  }

  protected void oneTimeSetup() throws Exception {
    super.oneTimeSetup();
  
    WorkbenchPlugin.getDefault().getPreferenceStore().setValue(IPreferenceConstants.RUN_IN_BACKGROUND, true);
    PrefUtil.getAPIPreferenceStore().setValue(IWorkbenchPreferenceConstants.ENABLE_ANIMATIONS, false);
    
    ShellFinder.bringRootToFront(getActivePage().getWorkbenchWindow().getShell().getDisplay());
    
    ui = getUI();
  
    if("Welcome".equals(getActivePage().getActivePart().getTitle())) {
      ui.close(new CTabItemLocator("Welcome"));
    }
    
    IPerspectiveRegistry perspectiveRegistry = PlatformUI.getWorkbench().getPerspectiveRegistry();
    IPerspectiveDescriptor perspective = perspectiveRegistry
        .findPerspectiveWithId("org.eclipse.jdt.ui.JavaPerspective");
    getActivePage().setPerspective(perspective);
  
    // close unnecessary tabs (different versions have different defaults in java perspective)
    closeView("org.eclipse.mylyn.tasks.ui.views.tasks", "Task List");
    closeView("org.eclipse.ui.views.ContentOutline", "Outline");
 
    createTestProject();
  }

  protected void setUp() throws Exception {
    super.setUp();
    
    ShellFinder.bringRootToFront(getActivePage().getWorkbenchWindow().getShell().getDisplay());
    
    workspace = ResourcesPlugin.getWorkspace();
    
    root = workspace.getRoot();
    ui = getUI();
    
    if("Welcome".equals(getActivePage().getActivePart().getTitle())) {
      ui.close(new CTabItemLocator("Welcome"));
    }
  }

  protected void tearDown() throws Exception {
    super.tearDown();
    
    IConditionMonitor monitor = (IConditionMonitor) ui.getAdapter(IConditionMonitor.class);
    monitor.add(new ShellShowingCondition("Save Resource(s)?"), //
      new IHandler() {
        public void handle(IUIContext ui) {
          try {
            ui.click(new ButtonLocator("(Yes|OK)"));
          } catch(WidgetSearchException ex) {
            // ignore
            ex.printStackTrace();
          }
        }
      });
    
    // ui.close(new CTabItemLocator("\\*?" + TEST_POM_POM_XML));
    ui.keyClick(SWT.CTRL, 's');
    Thread.sleep(500L);
    ui.keyClick(SWT.CTRL, 'w');
  }

  private void closeView(String id, String title) throws Exception {
    IViewPart view = getActivePage().findView(id);
    if (view != null) {
      ui.close(new CTabItemLocator(title));
    }
  }

  protected void putIntoClipboard(final String str) throws Exception {
  //		ui.contextClick(new TreeItemLocator(PROJECT_NAME, new ViewLocator("org.eclipse.jdt.ui.PackageExplorer")), "New/File");
  //		ui.wait(new ShellShowingCondition("New File"));
  //		ui.enterText("t.txt");
  //		ui.keyClick(WT.CR);
  //		ui.wait(new ShellDisposedCondition("Progress Information"));
  //		ui.wait(new ShellDisposedCondition("New File"));
  //		ui.enterText(str);
  //		ui.keyClick(SWT.CTRL, 'a');
  //		ui.keyClick(SWT.CTRL, 'c');
  //		ui.keyClick(SWT.CTRL, 'z');
  //		ui.close(new CTabItemLocator("t.txt"));
      
      Display.getDefault().syncExec(new Runnable() {
        public void run() {
          Clipboard clipboard = new Clipboard(Display.getDefault());
          TextTransfer transfer = TextTransfer.getInstance();
          clipboard.setContents(new String[] {str}, new Transfer[] {transfer});
          clipboard.dispose();
        }
      });
  	}

  private void createTestProject() throws CoreException {
    // create new project with POM using new project wizard
    // ui.contextClick(new SWTWidgetLocator(Tree.class, new ViewLocator("org.eclipse.jdt.ui.PackageExplorer")),
    //      "Ne&w/Maven Project");
    // ui.wait(new ShellShowingCondition("New Maven Project"));
    // ui.click(new ButtonLocator("Create a &simple project (skip archetype selection)"));
    // ui.click(new ButtonLocator("&Next >"));
    // ui.enterText("org.foo");
    // ui.keyClick(WT.TAB);
    // ui.enterText("test-pom");
    // ui.click(new ButtonLocator("&Finish"));
    // ui.wait(new ShellDisposedCondition("New Maven Project"));
  
    final IProjectConfigurationManager configurationManager = MavenPlugin.getDefault().getProjectConfigurationManager();
    
    final Model model = new Model();
    model.setModelVersion("4.0.0");
    model.setGroupId("org.foo");
    model.setArtifactId("test-pom");
    model.setVersion("1.0.0");
    
    ResourcesPlugin.getWorkspace().getRoot().refreshLocal(IResource.DEPTH_INFINITE, null);
    
    final IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(PROJECT_NAME);
    if (project.exists()) {
      project.delete(true, null);
    }
    
    WorkspaceJob job = new WorkspaceJob("creating test project") {
      public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
        //createTestProject();
        configurationManager.createSimpleProject(project, null, model, new String[0], new ProjectImportConfiguration(), new NullProgressMonitor());
        return Status.OK_STATUS;
      }
    };
    
    job.setRule(configurationManager.getRule());
    job.schedule();
    ui.wait(new JobsCompleteCondition(), 300000);
    
  }

  protected void createFile(String name, String content) throws Exception {
    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    IFile file = root.getFile(new Path(name));
    file.create(new ByteArrayInputStream(content.getBytes("UTF-8")), true, null);
    ui.wait(new ShellDisposedCondition("Progress Information"));
  }

  protected void selectEditorTab(final String id) throws Exception {
    final MavenPomEditor editor = (MavenPomEditor) getActivePage().getActiveEditor();
    Display.getDefault().syncExec(new Runnable() {
      public void run() {
        editor.setActivePage(id);
      }
    });
    ui.wait(new SWTIdleCondition());
  }

  protected void openPomFile(String name) throws Exception {
    // ui.click(2, new TreeItemLocator(name, new ViewLocator("org.eclipse.jdt.ui.PackageExplorer")));
    
    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    IFile file = root.getFile(new Path(name));
  
    final IEditorInput editorInput = new FileEditorInput(file);
    Display.getDefault().syncExec(new Runnable() {
      public void run() {
        try {
          getActivePage().openEditor(editorInput, "org.maven.ide.eclipse.editor.MavenPomEditor", true);
        } catch(PartInitException ex) {
          throw new RuntimeException(ex);
        }
      }
    });
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

  public PomEditorTestBase(String testName, Class launchClass, String[] launchArgs) {
    super(testName, launchClass, launchArgs);
  }

  protected void delete(String startMarker, final String endMarker) throws WaitTimedOutException {
      final MavenPomEditor editor = (MavenPomEditor) getActivePage().getActiveEditor();
      final StructuredTextEditor[] sse = new StructuredTextEditor[1];
      Display.getDefault().syncExec(new Runnable() {
        public void run() {
          sse[0] = (StructuredTextEditor) editor.getActiveEditor();
        }
      });
      
      @SuppressWarnings("restriction")
      IDocument structuredDocument = sse[0].getModel().getStructuredDocument();
      String text = structuredDocument.get();
      int pos1 = text.indexOf(startMarker); 
      int pos2 = text.indexOf(endMarker);
      text = text.substring(0, pos1) + text.substring(pos2 + endMarker.length());
      structuredDocument.set(text);
  }

  protected void assertTextValue(String id, String value) {
    ui.assertThat(new HasTextCondition(new NamedWidgetLocator(id), value));
  }

  protected void setTextValue(String id, String value) throws WidgetSearchException {
    ui.setFocus(new NamedWidgetLocator(id));
    ScreenCapture.createScreenCapture();
    ui.keyClick(SWT.CTRL, 'a');
    ScreenCapture.createScreenCapture();
    ui.enterText(value);
    ScreenCapture.createScreenCapture();
  }

  protected void replaceText(String src, String target) throws WaitTimedOutException, WidgetSearchException {
    ui.keyClick(SWT.CTRL, 'f');
    ui.wait(new ShellShowingCondition(FIND_REPLACE));
  
    ui.enterText(src);
    ui.keyClick(WT.TAB);
    ScreenCapture.createScreenCapture();
    
    ui.enterText(target);
    
    // ui.keyClick(SWT.ALT, 'a'); // "replace all"
    ui.click(new ButtonLocator("Replace &All"));
    
    ui.close(new SWTWidgetLocator(Shell.class, FIND_REPLACE));
    ui.wait(new ShellDisposedCondition(FIND_REPLACE));
    // ScreenCapture.createScreenCapture();
  }

  protected void findText(String src) throws WaitTimedOutException, WidgetSearchException {
    ui.keyClick(SWT.CTRL, 'f');
    ui.wait(new ShellShowingCondition(FIND_REPLACE));
    ui.enterText(src);
    ui.keyClick(WT.CR); // "find"
    ui.close(new SWTWidgetLocator(Shell.class, FIND_REPLACE));
    ui.wait(new ShellDisposedCondition(FIND_REPLACE));
  }

  ISelectionProvider getSelectionProvider() {
    return getEditorSite().getSelectionProvider();
  }

  private IEditorSite getEditorSite() {
    IEditorPart editor = getActivePage().getActiveEditor();
    IEditorSite editorSite = editor.getEditorSite();
    return editorSite;
  }

  IWorkbenchPage getActivePage() {
    IWorkbench workbench = PlatformUI.getWorkbench();
    IWorkbenchWindow window = workbench.getWorkbenchWindows()[0];
    return window.getActivePage();
  }

  protected String getEditorText() {
    final String[] texts = new String[1];
    Display.getDefault().syncExec(new Runnable() {
      public void run() {
        try {
          IWidgetLocator[] loc = ui.findAll(new SWTWidgetLocator(StyledText.class));
          for (int i=0; i<loc.length; i++) {
            WidgetReference ref = (WidgetReference) ui.find(loc[i]);
            texts[0] = ((StyledText) ref.getWidget()).getText();
            if (texts[0].contains("<project"))
              break;
          }
        } catch(WidgetSearchException ex) {
          ex.printStackTrace();
          ex.printStackTrace();
        }
      }
    });
    return texts[0];
  }

  protected void expandSectionIfRequired(String sectionName, String sectionLabel) throws WidgetSearchException {
    SWTWidgetLocator organizationLocator = new NamedWidgetLocator(sectionName);
    WidgetReference organizationReference = (WidgetReference) ui.find(organizationLocator);
    Section organizationSection = (Section) organizationReference.getWidget();
    if(!organizationSection.isExpanded()) {
      ui.click(new SWTWidgetLocator(Label.class, sectionLabel));
    }
  }

  protected void collapseSectionIfRequired(String sectionName, String sectionLabel) throws WidgetSearchException {
    SWTWidgetLocator organizationLocator = new NamedWidgetLocator(sectionName);
    WidgetReference organizationReference = (WidgetReference) ui.find(organizationLocator);
    Section organizationSection = (Section) organizationReference.getWidget();
    if(organizationSection.isExpanded()) {
      ui.click(new SWTWidgetLocator(Label.class, sectionLabel));
    }
  }

}
