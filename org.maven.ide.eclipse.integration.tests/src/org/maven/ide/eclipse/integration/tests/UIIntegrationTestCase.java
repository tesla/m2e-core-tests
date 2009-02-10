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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.codehaus.plexus.util.IOUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
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
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPerspectiveRegistry;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPreferenceConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.IPreferenceConstants;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.internal.util.PrefUtil;
import org.eclipse.ui.part.FileEditorInput;
import org.maven.ide.components.pom.Model;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.editor.pom.MavenPomEditor;

import com.windowtester.finder.swt.ShellFinder;
import com.windowtester.runtime.IUIContext;
import com.windowtester.runtime.WT;
import com.windowtester.runtime.WaitTimedOutException;
import com.windowtester.runtime.WidgetSearchException;
import com.windowtester.runtime.locator.IWidgetLocator;
import com.windowtester.runtime.swt.UITestCaseSWT;
import com.windowtester.runtime.swt.condition.SWTIdleCondition;
import com.windowtester.runtime.swt.condition.eclipse.JobsCompleteCondition;
import com.windowtester.runtime.swt.condition.shell.ShellDisposedCondition;
import com.windowtester.runtime.swt.condition.shell.ShellShowingCondition;
import com.windowtester.runtime.swt.locator.ButtonLocator;
import com.windowtester.runtime.swt.locator.CTabItemLocator;
import com.windowtester.runtime.swt.locator.FilteredTreeItemLocator;
import com.windowtester.runtime.swt.locator.LabeledLocator;
import com.windowtester.runtime.swt.locator.MenuItemLocator;
import com.windowtester.runtime.swt.locator.NamedWidgetLocator;
import com.windowtester.runtime.swt.locator.SWTWidgetLocator;
import com.windowtester.runtime.swt.locator.TreeItemLocator;
import com.windowtester.runtime.swt.locator.eclipse.ContributedToolItemLocator;
import com.windowtester.runtime.swt.locator.eclipse.ViewLocator;
import com.windowtester.runtime.util.ScreenCapture;


/**
 * @author Rich Seddon
 */
@SuppressWarnings("restriction")
public class UIIntegrationTestCase extends UITestCaseSWT {

  private static final String FIND_REPLACE = "Find/Replace";

  private static final String PLUGIN_ID = "org.maven.ide.eclipse.integration.tests";

  protected IUIContext ui;

  protected IWorkspaceRoot root;

  IWorkspace workspace;

  public UIIntegrationTestCase() {
    super();
  }

  public UIIntegrationTestCase(String testName) {
    super(testName);
  }

  protected IWorkbenchPage getActivePage() {
    IWorkbench workbench = PlatformUI.getWorkbench();
    return workbench.getWorkbenchWindows()[0].getActivePage();
  }

  protected void closeView(String id) throws Exception {
    IViewPart view = getActivePage().findView(id);
    if(view != null) {
      ui.close(new ViewLocator(id));
    }
  }

  protected void oneTimeSetup() throws Exception {
    super.oneTimeSetup();

    WorkbenchPlugin.getDefault().getPreferenceStore().setValue(IPreferenceConstants.RUN_IN_BACKGROUND, true);
    PrefUtil.getAPIPreferenceStore().setValue(IWorkbenchPreferenceConstants.ENABLE_ANIMATIONS, false);

    ShellFinder.bringRootToFront(getActivePage().getWorkbenchWindow().getShell().getDisplay());

    MavenPlugin.getDefault(); // force m2e to load so its indexing jobs will be scheduled.
    Thread.sleep(2000);
    ui = getUI();
    
    
    if("Welcome".equals(getActivePage().getActivePart().getTitle())) {
      ui.close(new CTabItemLocator("Welcome"));
    }

    IPerspectiveRegistry perspectiveRegistry = PlatformUI.getWorkbench().getPerspectiveRegistry();
    IPerspectiveDescriptor perspective = perspectiveRegistry
        .findPerspectiveWithId("org.eclipse.jdt.ui.JavaPerspective");
    getActivePage().setPerspective(perspective);

    // close unnecessary tabs (different versions have different defaults in java perspective)
    closeView("org.eclipse.ui.views.ContentOutline");
    closeView("org.eclipse.mylyn.tasks.ui.views.tasks");

    // Cancel maven central index job 
    MavenPlugin.getDefault();
    Thread.sleep(5000);
    Job [] jobs = Job.getJobManager().find(null);
    for(int i = 0; i < jobs.length; i++ ) {
      if (jobs[i].getClass().getName().endsWith("IndexUpdaterJob"))  {
        jobs[i].cancel();
        break;
      }
    }
    
    IViewPart indexView = showView("org.maven.ide.eclipse.views.MavenIndexesView");

    ui.click(new CTabItemLocator("Maven Indexes"));
    
    // Remove maven central.
    ui.contextClick(new TreeItemLocator(
        "central .*",
        new ViewLocator("org.maven.ide.eclipse.views.MavenIndexesView")), "Remove Index");
    ui.wait(new ShellShowingCondition("Remove Index"));
    ui.click(new ButtonLocator("OK"));
    
    // Add in nexus proxy for maven central
    ui.click(new ContributedToolItemLocator("org.maven.ide.eclipse.addIndexAction"));

    ui.wait(new ShellShowingCondition("Add Repository Index"));
    ui.click(new NamedWidgetLocator("repositoryUrlCombo"));
    ui.enterText("http://localhost:8081/nexus/content/groups/public/");
    ui.click(new NamedWidgetLocator("retrieveButton"));
    ui.wait(new JobsCompleteCondition());
    ui.wait(new SWTIdleCondition());
    ui.click(new ButtonLocator("OK"));
    ui.wait(new ShellDisposedCondition("Add Repository Index"));
    ui.contextClick(new TreeItemLocator(
        "central-remote.*",
        new ViewLocator("org.maven.ide.eclipse.views.MavenIndexesView")), "Update Index");
    hideView(indexView);

    clearProjects();
    
    ui.wait(new JobsCompleteCondition(), 300000);
  }

  protected void clearProjects() {

    WorkspaceJob job = new WorkspaceJob("deleting test projects") {
      public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
        ResourcesPlugin.getWorkspace().getRoot().refreshLocal(IResource.DEPTH_INFINITE, null);
        IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
        for(IProject project : projects) {
          project.delete(true, null);
        }
        return Status.OK_STATUS;
      }
    };

    job.setRule(ResourcesPlugin.getWorkspace().getRuleFactory().buildRule());
    job.schedule();
    ui.wait(new JobsCompleteCondition(), 600000);

  }

  protected void oneTimeTearDown() throws Exception {
    clearProjects();
    super.oneTimeTearDown();
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

  protected Model getModel(final MavenPomEditor editor) throws Exception {
    Model model = (Model) executeOnEventQueue(new Task() {

      public Object runEx() throws Exception {
        return editor.readProjectDocument();
      }
    });
    return model;
  }

  protected MavenPomEditor openPomFile(String name) throws Exception {

    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    IFile file = root.getFile(new Path(name));

    final IEditorInput editorInput = new FileEditorInput(file);
    MavenPomEditor editor = (MavenPomEditor) executeOnEventQueue(new Task() {

      public Object runEx() throws Exception {
        IEditorPart part = getActivePage().openEditor(editorInput, "org.maven.ide.eclipse.editor.MavenPomEditor", true);
        if(part instanceof MavenPomEditor) {
          return part;
        }
        return null;
      }
    });
    ui.wait(new SWTIdleCondition());
    return editor;
  }

  protected void checkTextNotFound(String text) throws WaitTimedOutException, WidgetSearchException {
    ui.keyClick(SWT.MOD1, 'f');
    ui.wait(new ShellShowingCondition(FIND_REPLACE));
    ui.enterText(text);
    ui.click(new ButtonLocator("Fi&nd"));
    ui.click(new LabeledLocator(Button.class, "String Not Found"));
    ui.wait(new ShellDisposedCondition(FIND_REPLACE));
  }

  protected void findText(String src) throws WaitTimedOutException, WidgetSearchException {
    ui.keyClick(SWT.CTRL, 'f');
    ui.wait(new ShellShowingCondition(FIND_REPLACE));
    ui.enterText(src);
    ui.keyClick(WT.CR); // "find"
    ui.close(new SWTWidgetLocator(Shell.class, FIND_REPLACE));
    ui.wait(new ShellDisposedCondition(FIND_REPLACE));
  }

  protected void replaceText(String src, String target) throws WaitTimedOutException, WidgetSearchException {
    ui.keyClick(SWT.CTRL, 'f');
    ui.wait(new ShellShowingCondition(FIND_REPLACE));

    ui.enterText(src);
    ui.keyClick(WT.TAB);
    ScreenCapture.createScreenCapture();

    ui.enterText(target);

    ui.click(new ButtonLocator("Replace &All"));

    ui.close(new SWTWidgetLocator(Shell.class, FIND_REPLACE));
    ui.wait(new ShellDisposedCondition(FIND_REPLACE));
  }

  public static abstract class Task implements Runnable {

    private Object result = null;

    private Exception exception = null;

    final public void run() {
      try {
        result = runEx();
      } catch(Exception ex) {
        exception = ex;
      }

    }

    public Exception getException() {
      return exception;
    }

    public Object getResult() {
      return result;
    }

    public abstract Object runEx() throws Exception;

  }

  public static Object executeOnEventQueue(Task task) throws Exception {
    if(Display.getDefault().getThread() == Thread.currentThread()) {
      task.run();
    } else {
      Display.getDefault().syncExec(task);
    }
    if(task.getException() != null) {
      throw task.getException();
    }
    return task.getResult();
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

  public void importZippedProject(String pluginPath) throws Exception {
    IUIContext ui = getUI();
    File f = copyPluginResourceToTempFile(PLUGIN_ID, pluginPath);
    try {
      ui.click(new MenuItemLocator("File/Import..."));
      ui.wait(new ShellShowingCondition("Import"));
      ui.click(new FilteredTreeItemLocator("General/Existing Projects into Workspace"));
      ui.click(new ButtonLocator("&Next >"));
      ui.click(new ButtonLocator("Select roo&t directory:"));
      ui.click(new ButtonLocator("Select &archive file:"));
      ui.enterText(f.getCanonicalPath());
      ui.keyClick(SWT.TAB);
      ui.click(new ButtonLocator("&Finish"));
      ui.wait(new ShellDisposedCondition("Import"));
      Thread.sleep(5000);
      ui.wait(new JobsCompleteCondition(), 240000);
    } finally {
      f.delete();
    }
  }

  public File unzipProject(String pluginPath) throws Exception {
    URL url = FileLocator.find(Platform.getBundle(PLUGIN_ID), new Path("/" + pluginPath), null);
    InputStream is = new BufferedInputStream(url.openStream());
    ZipInputStream zis = new ZipInputStream(is);
    File tempDir = createTempDir("sonatype");
    try {
      ZipEntry entry = zis.getNextEntry();
      while(entry != null) {
        File f = new File(tempDir, entry.getName());
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
    return tempDir;
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

  void deleteDirectory(File dir) {
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

  protected File doImport(String projectPath) throws Exception {
    File tempDir = unzipProject(projectPath);

    try {
      ui.click(new MenuItemLocator("File/Import..."));
      ui.wait(new ShellShowingCondition("Import"));
      ui.click(new FilteredTreeItemLocator("General/Maven Projects"));
      ui.click(new ButtonLocator("&Next >"));
      ui.wait(new SWTIdleCondition());
      ui.enterText(tempDir.getCanonicalPath());
      ui.keyClick(SWT.CR);
      Thread.sleep(2000);
      ui.click(new ButtonLocator("&Finish"));
      ui.wait(new ShellDisposedCondition("Import Maven Projects"));
      Thread.sleep(5000);
      ui.wait(new JobsCompleteCondition(), 300000);
      assertProjectsHaveNoErrors();

    } catch(Exception ex) {
      deleteDirectory(tempDir);
      throw ex;
    }

    return tempDir;
  }

  protected IViewPart showView(final String id) throws Exception {
    IViewPart part = (IViewPart) executeOnEventQueue(new Task() {
      public Object runEx() throws Exception {
        return getActivePage().showView(id);
      }
    });
    
    getUI().wait(new SWTIdleCondition());
    return part;
  }

  protected void hideView(final IViewPart view) throws Exception {
    executeOnEventQueue(new Task() {
      public Object runEx() throws Exception {
        getActivePage().hideView(view);
        return null;
      }
    });
    
    getUI().wait(new SWTIdleCondition());
  }
  
  protected void replaceText(IWidgetLocator locator, String text) throws WidgetSearchException {
    ui.click(locator);
    ui.keyClick(SWT.MOD1, 'a');
    ui.enterText(text);
  }

  public void assertProjectsHaveNoErrors() throws Exception {
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

}
