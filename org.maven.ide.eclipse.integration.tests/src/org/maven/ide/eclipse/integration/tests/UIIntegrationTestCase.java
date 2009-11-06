/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.integration.tests;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.codehaus.plexus.util.IOUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.ViewForm;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPerspectiveRegistry;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPreferenceConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.internal.IPreferenceConstants;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.internal.util.PrefUtil;
import org.eclipse.ui.part.FileEditorInput;
import org.maven.ide.components.pom.Model;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.editor.pom.MavenPomEditor;
import org.maven.ide.eclipse.embedder.MavenRuntime;
import org.maven.ide.eclipse.embedder.MavenRuntimeManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

import com.windowtester.finder.swt.ShellFinder;
import com.windowtester.internal.runtime.DiagnosticWriter;
import com.windowtester.runtime.IUIContext;
import com.windowtester.runtime.WT;
import com.windowtester.runtime.WaitTimedOutException;
import com.windowtester.runtime.WidgetSearchException;
import com.windowtester.runtime.condition.IUICondition;
import com.windowtester.runtime.condition.IsEnabledCondition;
import com.windowtester.runtime.locator.IWidgetLocator;
import com.windowtester.runtime.swt.UITestCaseSWT;
import com.windowtester.runtime.swt.condition.SWTIdleCondition;
import com.windowtester.runtime.swt.condition.eclipse.JobsCompleteCondition;
import com.windowtester.runtime.swt.condition.shell.ShellDisposedCondition;
import com.windowtester.runtime.swt.condition.shell.ShellShowingCondition;
import com.windowtester.runtime.swt.locator.ButtonLocator;
import com.windowtester.runtime.swt.locator.CTabItemLocator;
import com.windowtester.runtime.swt.locator.FilteredTreeItemLocator;
import com.windowtester.runtime.swt.locator.LabeledTextLocator;
import com.windowtester.runtime.swt.locator.MenuItemLocator;
import com.windowtester.runtime.swt.locator.NamedWidgetLocator;
import com.windowtester.runtime.swt.locator.SWTWidgetLocator;
import com.windowtester.runtime.swt.locator.TableCellLocator;
import com.windowtester.runtime.swt.locator.TreeItemLocator;
import com.windowtester.runtime.swt.locator.eclipse.ViewLocator;
import com.windowtester.runtime.util.ScreenCapture;


/**
 * @author rseddon
 */
@SuppressWarnings("restriction")
public abstract class UIIntegrationTestCase extends UITestCaseSWT {

  public static final String DEFAULT_PROJECT_ZIP = "projects/someproject.zip";
  public static final String DEFAULT_PROJECT_VERSION = "0.0.1-SNAPSHOT";
  public static final String DEFAULT_PROJECT_ARTIFACT = "someproject";
  public static final String DEFAULT_PROJECT_GROUP = "org.sonatype.test";

  /**
   * 
   */
  private static final String SERVERS_VIEW_ID = "org.eclipse.wst.server.ui.ServersView";

  protected static final String PLUGIN_ID = "org.maven.ide.eclipse.integration.tests";

  // Has the maven central index been cached into local workspace?
  private static boolean indexDownloaded = true;

  private boolean xmlPrefsSet = false;
  // URL of local nexus server, tests will attempt download maven/central index from here.
  private static final String DEFAULT_NEXUS_URL = "http://localhost:8081/nexus";

  // Set this system property to override DEFAULT_NEXUS_URL
  private static final String NEXUS_URL_PROPERTY = "nexus.server.url";

  // Location of tomcat 6 installation which can be used by Eclipse WTP tests
  private static final String DEFAULT_TOMCAT_INSTALL_LOCATION = "c:/test/apache-tomcat-6.0.18";

  // Set this system property to override DEFAULT_TOMCAT_INSTALL_LOCATION
  private static final String TOMCAT_INSTALL_LOCATION_PROPERTY = "tomcat.install.location";

  public static final String FIND_REPLACE = "Find/Replace";

  public static final String PACKAGE_EXPLORER_VIEW_ID = "org.eclipse.jdt.ui.PackageExplorer";


  private boolean skipIndexes;
  private boolean useExternalMaven;
  
  public UIIntegrationTestCase() {
  }

  protected IWorkbenchPage getActivePage() {
    IWorkbench workbench = PlatformUI.getWorkbench();
    return workbench.getWorkbenchWindows()[0].getActivePage();
  }

  protected void fullScreen() throws Exception {
    UIThreadTask.executeOnEventQueue(new UIThreadTask() {
    public Object runEx() throws Exception {
      ShellFinder.getWorkbenchRoot().setFullScreen(true);
      return Display.getDefault();
    }});
  }
  protected void closeView(final String id) throws Exception {
    
    IViewPart view = (IViewPart)UIThreadTask.executeOnEventQueue(new UIThreadTask() {

      public Object runEx() throws Exception {
        IViewPart view = getActivePage().findView(id);

        return view;
      }});
    
    if(view != null) {
      getUI().close(new ViewLocator(id));
    }
  }
  
  protected void switchToExternalMaven() throws Exception{
    MavenRuntime newRuntime = MavenRuntimeManager.createExternalRuntime("C:\\apache-maven-2.1.0");
    List<MavenRuntime> currRuntimes = MavenPlugin.getDefault().getMavenRuntimeManager().getMavenRuntimes();
    ArrayList<MavenRuntime> list = new ArrayList<MavenRuntime>(currRuntimes);
    list.add(newRuntime);
    MavenPlugin.getDefault().getMavenRuntimeManager().setRuntimes(list);
    MavenPlugin.getDefault().getMavenRuntimeManager().setDefaultRuntime(newRuntime);
  }
  
  protected void oneTimeSetup()throws Exception{
    super.oneTimeSetup();
    
    // getUI().ensureThat(new WorkbenchLocator().hasFocus()); 
     
     // Turn off eclipse features which make tests unreliable.
     WorkbenchPlugin.getDefault().getPreferenceStore().setValue(IPreferenceConstants.RUN_IN_BACKGROUND, true);
     
     PrefUtil.getAPIPreferenceStore().setValue(IWorkbenchPreferenceConstants.ENABLE_ANIMATIONS, false);

     
     fullScreen();
     MavenPlugin.getDefault(); // force m2e to load so its indexing jobs will be scheduled.
     Thread.sleep(5000);
     closeView("org.eclipse.ui.internal.introview");

     openPerspective("org.eclipse.jdt.ui.JavaPerspective");

     closeView("org.eclipse.ui.views.ContentOutline");
     closeView("org.eclipse.mylyn.tasks.ui.views.tasks");

     if(this.useExternalMaven()){
       this.switchToExternalMaven();
     }
     // Clean out projects left over from previous test runs.
     clearProjects();
  }

  protected void setXmlPrefs() throws Exception{
  if (isEclipseVersion(3, 5) && !xmlPrefsSet) {
    // Disable new xml completion behavior to preserver compatibility with previous versions.
      getUI().click(new MenuItemLocator("Window/Preferences"));
      getUI().wait(new ShellShowingCondition("Preferences"));
      getUI().click(new FilteredTreeItemLocator("XML/XML Files/Editor/Typing"));
      ButtonLocator buttonLocator = new ButtonLocator("&Insert a matching end tag");
      IUICondition selected = buttonLocator.isSelected();
      if(selected.testUI(getUI())){
        getUI().click(buttonLocator);
      }
      getUI().click(new ButtonLocator("OK"));
      getUI().wait(new ShellDisposedCondition("Preferences"));
      xmlPrefsSet=true;
      
    } 
  }
  private void openPerspective(final String id) throws Exception {
    UIThreadTask.executeOnEventQueue(new UIThreadTask() {

      public Object runEx() throws Exception {
        IPerspectiveRegistry perspectiveRegistry = PlatformUI.getWorkbench().getPerspectiveRegistry();
        IPerspectiveDescriptor perspective = perspectiveRegistry
            .findPerspectiveWithId(id);
        getActivePage().setPerspective(perspective);

        return null;
      }});
  }
  
  protected void cancelIndexJobs() throws InterruptedException {
    // Cancel maven central index job 
    MavenPlugin.getDefault();
    Thread.sleep(5000);
    Job[] jobs = Job.getJobManager().find(null);
    for(int i = 0; i < jobs.length; i++ ) {
      if(jobs[i].getClass().getName().endsWith("IndexManager$IndexUpdaterJob")) {
        jobs[i].cancel();
        break;
      }
    }

  }
  /**
   * Attempt up M2E to use a local Nexus server for downloading indexes. This speeds up test execution a lot.
   */
  private void setupLocalMavenIndex() throws Exception {
    if(!indexDownloaded) {
      indexDownloaded = true; // Only attempt to do this once.

      String nexusURL = System.getProperty(NEXUS_URL_PROPERTY);
      if(nexusURL == null) {
        nexusURL = DEFAULT_NEXUS_URL;
      }

      try {

        // Test URL, don't do this if we can't connect.
        URL url = new URL(nexusURL);
        URLConnection conn = url.openConnection();
        try {
          conn.setDoInput(true);
          conn.connect();
        } finally {
          conn.getInputStream().close();
        }

        cancelIndexJobs();
        
        getUI().wait(new JobsCompleteCondition(), 300000);
        
        IViewPart indexView = showView("org.maven.ide.eclipse.views.MavenIndexesView");

        getUI().click(new CTabItemLocator("Maven Indexes"));

        getUI().wait(new SWTIdleCondition());

        // Remove maven central index.
        getUI().contextClick(new TreeItemLocator("http:\\\\/\\\\/repo1.maven.org\\\\/maven2\\\\/", new ViewLocator("org.maven.ide.eclipse.views.MavenIndexesView")),
        "Remove Index");
        getUI().wait(new ShellShowingCondition("Remove Index"));
        getUI().click(new ButtonLocator("OK"));

        getUI().click(new CTabItemLocator("Maven Indexes"));

        // Add in nexus proxy for maven central
        getUI().contextClick(
            new TreeItemLocator("workspace", new ViewLocator("org.maven.ide.eclipse.views.MavenIndexesView")),
            "Add Index");
        //getUI().click(new ContributedToolItemLocator("org.maven.ide.eclipse.addIndexAction"));

        getUI().wait(new ShellShowingCondition("Add Repository Index"));
        getUI().click(new NamedWidgetLocator("repositoryUrlCombo"));
        getUI().enterText(nexusURL + "/content/groups/public/");
        getUI().click(new NamedWidgetLocator("displayName"));
        getUI().enterText("central");
        getUI().wait(new JobsCompleteCondition(), 300000);
        getUI().wait(new SWTIdleCondition());
        getUI().click(new ButtonLocator("OK"));
        getUI().wait(new ShellDisposedCondition("Add Repository Index"));
        getUI().contextClick(
            new TreeItemLocator("central.*", new ViewLocator("org.maven.ide.eclipse.views.MavenIndexesView")),
            "Update Index");
        hideView(indexView);
      } catch(IOException ex) {
        // Couldn't reach local nexus server, just go ahead and use maven central
        System.out.println("Failed to connect to local nexus server: " + nexusURL
            + " test will use maven central instead.");
      }
    }
  }

  /**
   * Remove all projects from the workspace
   */
  protected void clearProjects() {
    WorkspaceJob job = new WorkspaceJob("deleting test projects") {
      public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
        ResourcesPlugin.getWorkspace().getRoot().refreshLocal(IResource.DEPTH_INFINITE, null);
        IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
        for(IProject project : projects) {
          //project.delete(true, true, monitor);
          project.delete(true, monitor);
        }
        return Status.OK_STATUS;
      }
    };
    job.setRule(ResourcesPlugin.getWorkspace().getRuleFactory().buildRule());
    job.schedule();
    try{
      waitForAllBuildsToComplete();
    } catch(Exception e){}
  }

  protected void oneTimeTearDown() throws Exception {
    clearProjects();
    super.oneTimeTearDown();
  }

  protected void tearDown() throws Exception {
    super.tearDown();
    clearProjects();
  }

  protected void setUp() throws Exception {
    super.setUp();
    clearProjects();
    ShellFinder.bringRootToFront(getActivePage().getWorkbenchWindow().getShell().getDisplay());
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

  protected IEditorPart openFile(IProject project, String relPath) throws Exception {

    final IFile f = project.getFile(relPath);

    IEditorPart editor = (IEditorPart) UIThreadTask.executeOnEventQueue(new UIThreadTask() {

      public Object runEx() throws Exception {
        return IDE.openEditor(getActivePage(), f, true);
      }
    });
    getUI().wait(new JobsCompleteCondition(), 60000);
    return editor;
  }

  protected IEditorPart openTextFile(IProject project, String relPath) throws Exception {

    final IFile f = project.getFile(relPath);

    IEditorPart editor = (IEditorPart) UIThreadTask.executeOnEventQueue(new UIThreadTask() {

      public Object runEx() throws Exception {
        return IDE.openEditor(getActivePage(), f, EditorsUI.DEFAULT_TEXT_EDITOR_ID, true);
      }
    });
    getUI().wait(new JobsCompleteCondition(), 60000);
    return editor;
  }

  protected MavenPomEditor openPomFile(String name) throws Exception {

    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    IFile file = root.getFile(new Path(name));

    final IEditorInput editorInput = new FileEditorInput(file);
    MavenPomEditor editor = (MavenPomEditor) UIThreadTask.executeOnEventQueue(new UIThreadTask() {

      public Object runEx() throws Exception {
        IEditorPart part = getActivePage().openEditor(editorInput, "org.maven.ide.eclipse.editor.MavenPomEditor", true);
        if(part instanceof MavenPomEditor) {
          return part;
        }
        return null;
      }
    });
    getUI().wait(new SWTIdleCondition());
    return editor;
  }

  protected void findText(String src) throws WaitTimedOutException, WidgetSearchException {
    findTextWithWrap(src, false);
  }

  protected void findTextWithWrap(String src, boolean wrap) throws WaitTimedOutException, WidgetSearchException{
    getUI().keyClick(SWT.CTRL, 'f');
    getUI().wait(new ShellShowingCondition(FIND_REPLACE));
    getUI().enterText(src);
    if(wrap){
      getUI().click(new ButtonLocator("Wrap search"));
    }
    getUI().keyClick(WT.CR); // "find"
    getUI().close(new SWTWidgetLocator(Shell.class, FIND_REPLACE));
    getUI().wait(new ShellDisposedCondition(FIND_REPLACE));
  }
 
  protected boolean searchForText(String src, boolean wrap, boolean replace) throws WaitTimedOutException, WidgetSearchException{
    getUI().keyClick(SWT.CTRL, 'f');
    getUI().wait(new ShellShowingCondition(FIND_REPLACE));
    getUI().enterText(src);
    getUI().keyClick(WT.TAB);
    getUI().enterText("xxxxx");
    if(wrap){
      getUI().click(new ButtonLocator("Wrap search"));
    }
    getUI().click(new ButtonLocator("Find"));
    
    getUI().wait(new SWTIdleCondition());
    if(replace){
    
      getUI().assertThat(new IsEnabledCondition(new ButtonLocator("&Replace")));
    }
    return true;
    
  }
  protected void replaceTextWithClick(String src, String target) throws WaitTimedOutException, WidgetSearchException {
    getUI().click(new MenuItemLocator("Edit/Find\\\\/Replace..."));
    getUI().wait(new ShellShowingCondition("Find/Replace"));
    getUI().enterText(src);
    getUI().keyClick(WT.TAB);
    ScreenCapture.createScreenCapture();

    getUI().enterText(target);

    getUI().click(new ButtonLocator("Replace &All"));

    getUI().close(new SWTWidgetLocator(Shell.class, FIND_REPLACE));
    getUI().wait(new ShellDisposedCondition(FIND_REPLACE));
  }
  
  protected void replaceText(String src, String target) throws WaitTimedOutException, WidgetSearchException {
    replaceTextWithWrap(src, target, false);
  }
  
  protected void replaceTextWithWrap(String src, String target, boolean wrapSearch) throws WaitTimedOutException, WidgetSearchException {
    getUI().keyClick(SWT.CTRL, 'f');
    getUI().wait(new ShellShowingCondition(FIND_REPLACE));

    getUI().enterText(src);
    getUI().keyClick(WT.TAB);

    getUI().enterText(target);
    if(wrapSearch){
      getUI().click(new ButtonLocator("Wra&p search"));
    }
    getUI().click(new ButtonLocator("Replace &All"));

    getUI().close(new SWTWidgetLocator(Shell.class, FIND_REPLACE));
    getUI().wait(new ShellDisposedCondition(FIND_REPLACE));
  }

  protected File copyPluginResourceToTempFile(String plugin, String file) throws MalformedURLException, IOException {
    URL url = FileLocator.find(Platform.getBundle(plugin), new Path("/" + file), null);
    File f = File.createTempFile("temp", new Path(file).getFileExtension());
    InputStream is = new BufferedInputStream(url.openStream());
    FileOutputStream os = new FileOutputStream(f);
    try {
      IOUtil.copy(is, os);
    } finally {
      is.close();
      os.close();
    }

    return f;
  }

  public void importZippedMavenProjects(String pluginPath) throws Exception {
    IUIContext ui = getUI();
    File f = copyPluginResourceToTempFile(PLUGIN_ID, pluginPath);
    try {
      ui.keyClick(SWT.ALT, 'F'); // File -> Import
      ui.keyClick('I');
      ui.wait(new ShellShowingCondition("Import"));
      ui.click(new FilteredTreeItemLocator("General/Existing Projects into Workspace"));
      ui.click(new ButtonLocator("&Next >"));
      ui.click(new ButtonLocator("Select roo&t directory:"));
      ui.click(new ButtonLocator("Select &archive file:"));
      ui.enterText(f.getCanonicalPath());
      ui.keyClick(SWT.TAB);
      ui.click(new ButtonLocator("&Finish"));
      ui.wait(new ShellDisposedCondition("Import"));
      waitForAllBuildsToComplete();
    } finally {
      f.delete();
    }
  }
  
  public void importZippedProject(String pluginPath) throws Exception {
    IUIContext ui = getUI();
    File f = copyPluginResourceToTempFile(PLUGIN_ID, pluginPath);
    try {
      ui.keyClick(SWT.ALT, 'F'); // File -> Import
      ui.keyClick('I');
      ui.wait(new ShellShowingCondition("Import"));
      ui.click(new FilteredTreeItemLocator("General/Existing Projects into Workspace"));
      ui.click(new ButtonLocator("&Next >"));
      ui.click(new ButtonLocator("Select roo&t directory:"));
      ui.click(new ButtonLocator("Select &archive file:"));
      ui.enterText(f.getCanonicalPath());
      ui.keyClick(SWT.TAB);
      ui.click(new ButtonLocator("&Finish"));
      ui.wait(new ShellDisposedCondition("Import"));
      waitForAllBuildsToComplete();
    } finally {
      f.delete();
    }
  }
  
  private void unzipFile(String pluginPath, File dest) throws IOException {
    URL url = FileLocator.find(Platform.getBundle(PLUGIN_ID), new Path("/" + pluginPath), null);
    InputStream is = new BufferedInputStream(url.openStream());
    ZipInputStream zis = new ZipInputStream(is);
    try {
      ZipEntry entry = zis.getNextEntry();
      while(entry != null) {
        File f = new File(dest, entry.getName());
        if(entry.isDirectory()) {
          f.mkdirs();
        } else {
          if(!f.getParentFile().exists()) {
            f.getParentFile().mkdirs();
          }
          OutputStream os = new BufferedOutputStream(new FileOutputStream(f));
          try {
            IOUtil.copy(zis, os);
          } finally {
            os.close();
          }
        }
        zis.closeEntry();
        entry = zis.getNextEntry();
      }
    } finally {
      zis.close();
    }
  }
  
  public File unzipProject(String pluginPath) throws Exception {
    File tempDir = createTempDir("sonatype");
    unzipFile(pluginPath, tempDir);
    return tempDir;
  }
  
  public IProject setupDefaultProject() throws Exception{
    importMavenProjects(DEFAULT_PROJECT_ZIP);
    IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
    for(IProject project : projects) {
      if(DEFAULT_PROJECT_ARTIFACT.equals(project.getName())) {
        return project;
      }
    }
    return null;
  }

  protected File createTempDir(String prefix) throws IOException {
    File temp = null;
    temp = File.createTempFile(prefix, "");
    if(!temp.delete()) {
      throw new IOException("Unable to delete temp file:" + temp.getName());
    }
    if(!temp.mkdir()) {
      throw new IOException("Unable to create temp dir:" + temp.getName());
    }
    return temp;
  }

  protected void deleteDirectory(File dir) {
    File[] fileArray = dir.listFiles();
    if(fileArray != null) {
      for(int i = 0; i < fileArray.length; i++ ) {
        if(fileArray[i].isDirectory())
          deleteDirectory(fileArray[i]);
        else
          fileArray[i].delete();
      }
    }
    dir.delete();
  }

  protected File importMavenProjects(String projectPath) throws Exception {
    File tempDir = unzipProject(projectPath);
    waitForAllBuildsToComplete();
    try{
      
      getUI().click(new ButtonLocator("Cancel"));
      //if there is a dialog up here, take a screenshot but get rid of it - so we can keep going
      ScreenCapture.createScreenCapture();
    } catch(Exception e){
      //make sure that there are no dialogs up here
    }
    try {
      getUI().click(new MenuItemLocator("File/Import..."));
      
      getUI().wait(new ShellShowingCondition("Import"));
      getUI().click(new FilteredTreeItemLocator("Maven/Existing Maven Projects"));
      getUI().click(new ButtonLocator("&Next >"));
      getUI().wait(new SWTIdleCondition());
      getUI().enterText(tempDir.getCanonicalPath());
      getUI().keyClick(SWT.CR);
      Thread.sleep(2000);
      getUI().click(new ButtonLocator("&Finish"));
      getUI().wait(new ShellDisposedCondition("Import Maven Projects"));
      Thread.sleep(5000);
      
      waitForAllBuildsToComplete();
      

    } catch(Exception ex) {
      deleteDirectory(tempDir);
      throw ex;
    }

    return tempDir;
  }

  
  /**
   * Import a project and assert it has no markers of SEVERITY_ERROR
   */
  protected File doImport(String projectPath) throws Exception {
    return doImport(projectPath, true);
  }

  protected File doImport(String projectPath, boolean assertNoErrors) throws Exception{
    File tempDir = importMavenProjects(projectPath);
    if(assertNoErrors){
      assertProjectsHaveNoErrors();
    }
    return tempDir;
  }
  protected IViewPart showView(final String id) throws Exception {
    IViewPart part = (IViewPart) UIThreadTask.executeOnEventQueue(new UIThreadTask() {
      public Object runEx() throws Exception {
        IViewPart part = getActivePage().showView(id);

        return part;
      }
    });

    Thread.sleep(5000);
    
    getUI().wait(new SWTIdleCondition());
    assertFalse(part == null);
    return part;
  }

  protected void hideView(final IViewPart view) throws Exception {
    UIThreadTask.executeOnEventQueue(new UIThreadTask() {
      public Object runEx() throws Exception {
        getActivePage().hideView(view);
        return null;
      }
    });

    getUI().wait(new SWTIdleCondition());
  }

  protected void replaceText(IWidgetLocator locator, String text) throws WidgetSearchException {
    // Ctrl+A doesn't work in Eclipse 3.3 form view, but it is more reliable than double click so it is preferable to use it in later versions.
    // This is a workaround
    if(isEclipseVersion(3,3)) {
      getUI().click(2, locator);
    } else {
      getUI().click(locator);
      getUI().setFocus(locator);
      getUI().keyClick(SWT.MOD1, 'a');
    }
    getUI().enterText(text);
  }

  protected void assertProjectsHaveNoErrors() throws Exception {
    StringBuffer messages = new StringBuffer();
    IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
    int count = 0;
    for(IProject project : projects) {
      if("Servers".equals(project.getName())) {
        continue;
      }
      if(count >= 10) {
        break;
      }
      IMarker[] markers = project.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
      for(int i = 0; i < markers.length; i++ ) {
        if(markers[i].getAttribute(IMarker.SEVERITY, 0) == IMarker.SEVERITY_ERROR) {
          count++ ;
          messages.append('\t');
          if(messages.length() > 0) {
            messages.append(System.getProperty("line.separator"));
          }
          messages.append(project.getName() + ":" + markers[i].getAttribute(IMarker.LOCATION, "unknown location") + " "
              + markers[i].getAttribute(IMarker.MESSAGE, "unknown message"));
        }
      }
    }
    if(count > 0) {
      fail("One or more compile errors found:" + System.getProperty("line.separator") + messages);

    }
  }

  public static void copyFile(File from, File to) throws IOException {
    FileInputStream is = null;
    FileOutputStream os = null;

    File dest = to.getParentFile();
    if(dest != null) {
      dest.mkdirs();
    }

    try {
      is = new FileInputStream(from);
      os = new FileOutputStream(to);

      FileChannel outChannel = os.getChannel();
      outChannel.transferFrom(is.getChannel(), 0, is.getChannel().size());
    } finally {
      if(is != null) {
        is.close();
      }
      if(os != null) {
        os.close();
      }
    }
  }

  public String getPlatformPath() {
    URL installURL = Platform.getInstallLocation().getURL();
    IPath ppath = new Path(installURL.getFile()).removeTrailingSeparator();
    return getCorrectPath(ppath.toOSString());
  }

  /**
   * Patch up paths returned by Eclipse Path.toOSString()
   */
  private String getCorrectPath(String path) {
    StringBuffer buf = new StringBuffer();
    for(int i = 0; i < path.length(); i++ ) {
      char c = path.charAt(i);
      if(Platform.getOS().equals("win32")) { //$NON-NLS-1$
        if(i == 0 && c == '/')
          continue;
      }
      // Some VMs may return %20 instead of a space
      if(c == '%' && i + 2 < path.length()) {
        char c1 = path.charAt(i + 1);
        char c2 = path.charAt(i + 2);
        if(c1 == '2' && c2 == '0') {
          i += 2;
          buf.append(" "); //$NON-NLS-1$
          continue;
        }
      }
      buf.append(c);
    }
    return buf.toString();
  }

  protected void checkoutProjectsFromSVN(String url) throws Exception {
    getUI().click(new MenuItemLocator("File/Import..."));
    getUI().wait(new ShellShowingCondition("Import"));
    getUI().click(new FilteredTreeItemLocator("General"));
    getUI().click(new FilteredTreeItemLocator("Maven/Check out Maven Projects from SCM"));
    getUI().click(new ButtonLocator("&Next >"));
    //for some reason, in eclipse 3.5.1 and WT, the direct combo selection is
    //not triggering the UI events, so the finish button never gets enabled
  //getUI().click(new ComboItemLocator("svn", new NamedWidgetLocator("mavenCheckoutLocation.typeCombo")));
    getUI().setFocus(new NamedWidgetLocator("mavenCheckoutLocation.typeCombo"));
    for(int i=0;i<9;i++){
      getUI().keyClick(WT.ARROW_DOWN);
    }
    getUI().click(new NamedWidgetLocator("mavenCheckoutLocation.urlCombo"));
    getUI().enterText(url);
    getUI().wait(new SWTIdleCondition());
    getUI().click(new ButtonLocator("&Finish"));
    getUI().wait(new ShellDisposedCondition("Checkout as Maven project from SCM"));

    getUI().wait(new JobsCompleteCondition(), 300000);

    Thread.sleep(5000);
  }

  protected void installTomcat6() throws Exception {

    String tomcatInstallLocation = System.getProperty(TOMCAT_INSTALL_LOCATION_PROPERTY);
    if(tomcatInstallLocation == null) {
      tomcatInstallLocation = DEFAULT_TOMCAT_INSTALL_LOCATION;
    }

    assertTrue("Can't locate tomcat installation: " + tomcatInstallLocation, new File(tomcatInstallLocation).exists());
    // Install the Tomcat server 

    Thread.sleep(5000);

    String newServer = isEclipseVersion(3,3) ? "New/Server" : "Ne&w/Server";
    showView(SERVERS_VIEW_ID);
    getUI().contextClick(new SWTWidgetLocator(Tree.class, new ViewLocator(SERVERS_VIEW_ID)),
        newServer);
    getUI().wait(new ShellShowingCondition("New Server"));
    Thread.sleep(2000);
    getUI().click(new FilteredTreeItemLocator("Apache/Tomcat v6.0 Server"));
    getUI().click(new ButtonLocator("&Next >"));

    ButtonLocator b = new ButtonLocator("&Finish");
    if(!b.isEnabled(getUI())) {
      //First time...
      getUI().click(new LabeledTextLocator("Tomcat installation &directory:"));
      getUI().enterText(tomcatInstallLocation);
    }
    getUI().click(new ButtonLocator("&Finish"));
    getUI().wait(new ShellDisposedCondition("New Server"));
    Thread.sleep(5000);
    getUI().wait(new JobsCompleteCondition(), 120000);
  }

  protected Model getModel(final MavenPomEditor editor) throws Exception {
    Model model = (Model) UIThreadTask.executeOnEventQueue(new UIThreadTask() {

      public Object runEx() throws Exception {
        return editor.readProjectDocument();
      }
    });
    return model;
  }

  protected IProject createQuickstartProject(String projectName) throws Exception{
    return createArchetypeProject("maven-archetype-quickstart", projectName);
  }
  /**
   * Create an archetype project and assert that it has proper natures & builders, and no error markers
   */
  protected IProject createArchetypeProject(String archetypeName, String projectName) throws Exception {
    try {
      IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
      assertFalse(project.exists());

      IUIContext ui = getUI();
      ui.click(new SWTWidgetLocator(ViewForm.class, new SWTWidgetLocator(CTabFolder.class, 0, new SWTWidgetLocator(
          Composite.class))));
      ui.click(new MenuItemLocator("File/New/Project..."));
      ui.wait(new ShellShowingCondition("New Project"));
      ui.click(new FilteredTreeItemLocator("Plug-in Project"));
      ui.click(new FilteredTreeItemLocator("Maven/Maven Project"));
      //click the first next button
      ui.click(new ButtonLocator("&Next >"));
      //then the first page with only 'default' values
      ui.click(new ButtonLocator("&Next >"));
      
      ui.wait(new SWTIdleCondition());
      //now select the quickstart row
      ui.click(new TableCellLocator(archetypeName, 2));
      //and then click next
      ui.click(new ButtonLocator("&Next >"));

	  //then fill in the last page details
      IWidgetLocator groupCombo = ui.find(new NamedWidgetLocator("groupId"));
      ui.setFocus(groupCombo);
      
      ui.enterText(DEFAULT_PROJECT_GROUP);
      ui.setFocus(ui.find(new NamedWidgetLocator("artifactId")));
      ui.enterText(projectName);
      ui.click(new ButtonLocator("&Finish"));
      ui.wait(new ShellDisposedCondition("New Maven Project"));
      waitForAllBuildsToComplete();

      project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
      assertTrue(project.exists());
      assertProjectsHaveNoErrors();
      assertTrue("archtype project \"" + archetypeName + "\" created without Maven nature", project
          .hasNature("org.maven.ide.eclipse.maven2Nature"));

      ui.click(new TreeItemLocator(projectName + ".*", new ViewLocator(PACKAGE_EXPLORER_VIEW_ID)));
     
      return project;
    } catch(Exception ex) {
      ex.printStackTrace();
      ScreenCapture.createScreenCapture();
      throw new Exception("Failed to create project for archetype:" + archetypeName, ex);
    }
  }

  private static final String TOMCAT_SERVER_NAME = "Tomcat.*";

  protected void removeAllProjectInTomcat() throws Exception {
    getUI().click(new CTabItemLocator("Servers"));
    getUI().contextClick(
        new TreeItemLocator(TOMCAT_SERVER_NAME, new ViewLocator(SERVERS_VIEW_ID)),
        "Add and Remove Projects...");
    getUI().wait(new ShellShowingCondition("Add and Remove Projects"));
    getUI().click(new ButtonLocator("<< Re&move All"));
    getUI().click(new ButtonLocator("&Finish"));
    getUI().wait(new ShellDisposedCondition("Add and Remove Projects"));

    Thread.sleep(3000);
  }

  protected void deployProjectsIntoTomcat() throws WidgetSearchException, InterruptedException {
    // Deploy the test project into tomcat
    getUI().click(new CTabItemLocator("Servers"));
    if (isEclipseVersion(3, 5)) {
      getUI().contextClick(
          new TreeItemLocator(TOMCAT_SERVER_NAME, new ViewLocator(SERVERS_VIEW_ID)),
          "&Add and Remove...");
    } else {
      getUI().contextClick(
          new TreeItemLocator(TOMCAT_SERVER_NAME, new ViewLocator(SERVERS_VIEW_ID)),
          "Add and Remove Projects...");
    }
    String title = isEclipseVersion(3, 5) ? "Add and Remove..." : "Add and Remove Projects";
    getUI().wait(new ShellShowingCondition(title));
    getUI().click(new ButtonLocator("Add A&ll >>"));
    getUI().click(new ButtonLocator("&Finish"));
    getUI().wait(new ShellDisposedCondition("Add and Remove Projects"));

    Thread.sleep(3000);

    // Start the server
    if(isEclipseVersion(3,4)) {
      getUI().click(new TreeItemLocator(TOMCAT_SERVER_NAME, new ViewLocator(SERVERS_VIEW_ID)));
      getUI().keyClick(SWT.MOD1 | SWT.ALT, 'r');
    } else {
      getUI().contextClick(
          new TreeItemLocator(TOMCAT_SERVER_NAME, new ViewLocator(SERVERS_VIEW_ID)), "Start");
    }
    getUI().wait(new JobsCompleteCondition(JobsCompleteCondition.IGNORE_SYSTEM_JOBS), 120000);
    Thread.sleep(20000);
    //getUI().click(new CTabItemLocator("Servers"));
    //Thread.sleep(3000);
  }

  protected void shutdownTomcat() throws Exception {
    // Stop the server
    getUI().click(new TreeItemLocator("Servers", new ViewLocator(PACKAGE_EXPLORER_VIEW_ID)));
    getUI().keyClick(SWT.F5);
    getUI().click(new CTabItemLocator("Servers"));
    if(isEclipseVersion(3,4)) {
      getUI().click(new TreeItemLocator(TOMCAT_SERVER_NAME, new ViewLocator(SERVERS_VIEW_ID)));
      getUI().keyClick(SWT.MOD1 | SWT.ALT, 's');
    } else {
      getUI().contextClick(
          new TreeItemLocator(TOMCAT_SERVER_NAME, new ViewLocator(SERVERS_VIEW_ID)), "Stop");
    }
    getUI().wait(new JobsCompleteCondition(), 120000);
    getUI().click(new TreeItemLocator(TOMCAT_SERVER_NAME, new ViewLocator(SERVERS_VIEW_ID)));
    getUI().contextClick(
        new TreeItemLocator(TOMCAT_SERVER_NAME, new ViewLocator(SERVERS_VIEW_ID)), "Delete");
    getUI().wait(new ShellShowingCondition("Delete Server"));
    getUI().click(new ButtonLocator("OK"));
    getUI().wait(new ShellDisposedCondition("Delete Server"));
  }

  protected String retrieveWebPage(String urlString) throws Exception {
    URL url = new URL(urlString);
    URLConnection conn = url.openConnection();
    conn.setDoInput(true);
    conn.connect();
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    IOUtil.copy(conn.getInputStream(), out);
    conn.getInputStream().close();

    return new String(out.toByteArray(), "UTF-8");
  }

  
  public static boolean isEclipseVersion(int major, int minor) {
    Bundle bundle = ResourcesPlugin.getPlugin().getBundle();
    String version = (String) bundle.getHeaders().get(org.osgi.framework.Constants.BUNDLE_VERSION);
    Version v = org.osgi.framework.Version.parseVersion(version);
    return v.getMajor() == major && v.getMinor() == minor;
  }

  protected void waitForAllBuildsToComplete() throws InterruptedException {
    
    Thread.sleep(3000);
    
    // Some m2e builds trigger subqequent builds, and each build starts with a delay.
    for (int i = 0; i < 10 && !new JobsCompleteCondition().test(); i++) {
      JobsCompleteCondition condition = null;
      try{
        condition = new JobsCompleteCondition();
        getUI().wait(condition, 240000);
      } catch(WaitTimedOutException wtoe){
        //trying to diagnose what kind of jobs cause this to fail
        wtoe.printStackTrace();
        if(condition != null){
          DiagnosticWriter writer = new DiagnosticWriter();
          condition.diagnose(writer);
        }
      }
      Thread.sleep(5000);
    }
  }
  
  protected void addDependency(IProject project, String groupId, String artifactID, String version) throws Exception {
    openFile(project, "pom.xml");

    
    getUI().wait(new JobsCompleteCondition(), 120000);
    getUI().click(new CTabItemLocator("pom.xml"));
    findText("</dependencies");
    getUI().keyClick(SWT.ARROW_LEFT);
    String sep = System.getProperty("line.separator");
    getUI()
        .enterText(
            "<dependency>" + sep + "<groupId>" + groupId + "</<artifactId>" + artifactID +"</<version>" + version + "</<type>jar</<scope>compile</" + sep + "</" + sep);
    getUI().keyClick(SWT.MOD1, 's');

    waitForAllBuildsToComplete();
  }

  /**
   * @param skipIndexes the skipIndexes to set
   */
  public void setSkipIndexes(boolean skipIndexes) {
    this.skipIndexes = skipIndexes;
  }

  /**
   * @return the skipIndexes
   */
  public boolean skipIndexes() {
    return skipIndexes;
  }
  
  public void setUseExternalMaven(boolean useExternal){
    this.useExternalMaven = useExternal;
  }
  
  public boolean useExternalMaven(){
    return this.useExternalMaven;
  }
  
}
