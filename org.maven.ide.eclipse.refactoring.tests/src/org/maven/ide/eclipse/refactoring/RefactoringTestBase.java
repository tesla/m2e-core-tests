
package org.maven.ide.eclipse.refactoring;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;

import org.apache.maven.model.Model;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
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
import org.maven.ide.eclipse.editor.pom.MavenPomEditor;
import org.maven.ide.eclipse.embedder.MavenModelManager;
import org.maven.ide.eclipse.project.MavenProjectInfo;
import org.maven.ide.eclipse.project.ProjectImportConfiguration;
import org.maven.ide.eclipse.project.ResolverConfiguration;

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
import com.windowtester.runtime.swt.condition.shell.ShellDisposedCondition;
import com.windowtester.runtime.swt.condition.shell.ShellShowingCondition;
import com.windowtester.runtime.swt.locator.ButtonLocator;
import com.windowtester.runtime.swt.locator.CTabItemLocator;
import com.windowtester.runtime.swt.locator.NamedWidgetLocator;
import com.windowtester.runtime.swt.locator.SWTWidgetLocator;
import com.windowtester.runtime.util.ScreenCapture;


public class RefactoringTestBase extends UITestCaseSWT {

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

  public RefactoringTestBase() {
    super();
  }

  public RefactoringTestBase(String testName) {
    super(testName);
  }

  public RefactoringTestBase(Class launchClass) {
    super(launchClass);
  }

  public RefactoringTestBase(String testName, Class launchClass) {
    super(testName, launchClass);
  }

  public RefactoringTestBase(Class launchClass, String[] launchArgs) {
    super(launchClass, launchArgs);
  }

  protected void oneTimeSetup() throws Exception {
    super.oneTimeSetup();
  
    WorkbenchPlugin.getDefault().getPreferenceStore().setValue(IPreferenceConstants.RUN_IN_BACKGROUND, true);
    PrefUtil.getAPIPreferenceStore().setValue(IWorkbenchPreferenceConstants.ENABLE_ANIMATIONS, false);
    workspace = ResourcesPlugin.getWorkspace();
  
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
  
    createTestProjects();
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
      Display.getDefault().syncExec(new Runnable() {
        public void run() {
          Clipboard clipboard = new Clipboard(Display.getDefault());
          TextTransfer transfer = TextTransfer.getInstance();
          clipboard.setContents(new String[] {str}, new Transfer[] {transfer});
          clipboard.dispose();
        }
      });
  	}

  private void createTestProjects() throws CoreException {
    ResolverConfiguration configuration = new ResolverConfiguration();
    try {
      importProject("projects/mine/pom.xml", configuration);
      importProject("projects/child/pom.xml", configuration);
    } catch(IOException ex) {
      throw new CoreException(new Status(IStatus.ERROR, "Cannot import project", 0, "", ex));
    }
  }

  protected IProject importProject(String pomName, ResolverConfiguration configuration) throws IOException, CoreException {
    File pomFile = new File(pomName);
    return importProjects(pomFile.getParentFile().getCanonicalPath(), new String[] {pomFile.getName()}, configuration)[0];
  }

  protected IProject[] importProjects(String basedir, String[] pomNames, ResolverConfiguration configuration) throws IOException, CoreException {
    final MavenPlugin plugin = MavenPlugin.getDefault();
    MavenModelManager mavenModelManager = plugin.getMavenModelManager();
    IWorkspaceRoot root = workspace.getRoot();
    
    File src = new File(basedir);
    File dst = new File(root.getLocation().toFile(), src.getName());
    copyDir(src, dst);

    final ArrayList<MavenProjectInfo> projectInfos = new ArrayList<MavenProjectInfo>();
    for(String pomName : pomNames) {
      File pomFile = new File(dst, pomName);
      Model model = mavenModelManager.readMavenModel(pomFile);
      projectInfos.add(new MavenProjectInfo(pomName, pomFile, model, null));
    }

    final ProjectImportConfiguration importConfiguration = new ProjectImportConfiguration(configuration);

    workspace.run(new IWorkspaceRunnable() {
      public void run(IProgressMonitor monitor) throws CoreException {
        plugin.getProjectConfigurationManager().importProjects(projectInfos, importConfiguration, monitor);
      }
    }, plugin.getProjectConfigurationManager().getRule(), IWorkspace.AVOID_UPDATE, monitor);

    IProject[] projects = new IProject[projectInfos.size()];
    for (int i = 0; i < projectInfos.size(); i++) {
      MavenProjectInfo projectInfo = projectInfos.get(i);
      IProject project = importConfiguration.getProject(root, projectInfo.getModel());
      projects[i] = project;
      assertNotNull("Failed to import project " + projectInfos, project);
    }

    return projects;
  }

  protected static final IProgressMonitor monitor = new NullProgressMonitor();
  
  protected static void copyDir(File src, File dst) throws IOException {
    copyDir(src, dst, new FileFilter() {
      public boolean accept(File pathname) {
        return !".svn".equals(pathname.getName());
      }
    });
  }
  
  public static void copyDir(File src, File dst, FileFilter filter) throws IOException {
    copyDir(src, dst, filter, true);
  }

  private static void copyDir(File src, File dst, FileFilter filter, boolean deleteDst) throws IOException {
    if (deleteDst) {
      FileUtils.deleteDirectory(dst);
    }
    dst.mkdirs();
    File[] files = src.listFiles(filter);
    if (files != null) {
      for (int i = 0; i < files.length; i++) {
        File file = files[i];
        if (file.canRead()) {
          File dstChild = new File(dst, file.getName());
          if (file.isDirectory()) {
            copyDir(file, dstChild, filter, false);
          } else {
            copyFile(file, dstChild);
          }
        }
      }
    }
  }

  private static void copyFile(File src, File dst) throws IOException {
    BufferedInputStream in = new BufferedInputStream(new FileInputStream(src));
    BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(dst));

    byte[] buf = new byte[10240];
    int len;
    while ( (len = in.read(buf)) != -1 ) {
      out.write(buf, 0, len);
    }

    out.close();
    in.close();
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
    Thread.sleep(1000L);
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

  public RefactoringTestBase(String testName, Class launchClass, String[] launchArgs) {
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
