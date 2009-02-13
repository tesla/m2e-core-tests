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
import java.net.URLConnection;
import java.nio.channels.FileChannel;
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
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
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
import org.eclipse.ui.ide.IDE;
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
import com.windowtester.runtime.swt.locator.ComboItemLocator;
import com.windowtester.runtime.swt.locator.FilteredTreeItemLocator;
import com.windowtester.runtime.swt.locator.LabeledTextLocator;
import com.windowtester.runtime.swt.locator.MenuItemLocator;
import com.windowtester.runtime.swt.locator.NamedWidgetLocator;
import com.windowtester.runtime.swt.locator.SWTWidgetLocator;
import com.windowtester.runtime.swt.locator.TreeItemLocator;
import com.windowtester.runtime.swt.locator.eclipse.ContributedToolItemLocator;
import com.windowtester.runtime.swt.locator.eclipse.ViewLocator;
import com.windowtester.runtime.util.ScreenCapture;


/**
 * @author rseddon
 */
@SuppressWarnings("restriction")
public class UIIntegrationTestCase extends UITestCaseSWT {

  private static final String PLUGIN_ID = "org.maven.ide.eclipse.integration.tests";

  // Has the maven central index been cached into local workspace?
  private static boolean indexDownloaded = false;
  
  // URL of local nexus server, tests will attempt download maven/central index from here.
  private static final String DEFAULT_NEXUS_URL = "http://localhost:8081/nexus";

  // Set this system property to override DEFAULT_NEXUS_URL
  private static final String NEXUS_URL_PROPERTY = "nexus.server.url";

  // Location of tomcat 6 installation which can be used by Eclipse WTP tests
  private static final String DEFAULT_TOMCAT_INSTALL_LOCATION = "C:\\test\\apache-tomcat-6.0.18";
  
  // Set this system property to override DEFAULT_TOMCAT_INSTALL_LOCATION
  private static final String TOMCAT_INSTALL_LOCATION_PROPERTY = "tomcat.install.location";
  
  private static final String FIND_REPLACE = "Find/Replace";

  public static final String PACKAGE_EXPLORER_VIEW_ID = "org.eclipse.jdt.ui.PackageExplorer";
  
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
      getUI().close(new ViewLocator(id));
    }
  }

  protected void oneTimeSetup() throws Exception {
    super.oneTimeSetup();

    // Turn off eclipse features which make tests unreliable.
    WorkbenchPlugin.getDefault().getPreferenceStore().setValue(IPreferenceConstants.RUN_IN_BACKGROUND, true);
    PrefUtil.getAPIPreferenceStore().setValue(IWorkbenchPreferenceConstants.ENABLE_ANIMATIONS, false);

    ShellFinder.bringRootToFront(getActivePage().getWorkbenchWindow().getShell().getDisplay());

    MavenPlugin.getDefault(); // force m2e to load so its indexing jobs will be scheduled.
    Thread.sleep(5000);

    closeView("org.eclipse.ui.internal.introview");

    IPerspectiveRegistry perspectiveRegistry = PlatformUI.getWorkbench().getPerspectiveRegistry();
    IPerspectiveDescriptor perspective = perspectiveRegistry
        .findPerspectiveWithId("org.eclipse.jdt.ui.JavaPerspective");
    getActivePage().setPerspective(perspective);

    closeView("org.eclipse.ui.views.ContentOutline");
    closeView("org.eclipse.mylyn.tasks.ui.views.tasks");

    // Attempt to use local nexus as maven central proxy to speed up tests
    setupLocalMavenIndex();

    // Clean out projects left over from previous test runs.
    clearProjects();

    getUI().wait(new JobsCompleteCondition(), 300000);
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

        IViewPart indexView = showView("org.maven.ide.eclipse.views.MavenIndexesView");

        getUI().click(new CTabItemLocator("Maven Indexes"));

        getUI().wait(new SWTIdleCondition());

        // Remove maven central index.
        getUI().contextClick(new TreeItemLocator("central .*", new ViewLocator(
            "org.maven.ide.eclipse.views.MavenIndexesView")), "Remove Index");
        getUI().wait(new ShellShowingCondition("Remove Index"));
        getUI().click(new ButtonLocator("OK"));

        // Add in nexus proxy for maven central
        getUI().click(new ContributedToolItemLocator("org.maven.ide.eclipse.addIndexAction"));

        getUI().wait(new ShellShowingCondition("Add Repository Index"));
        getUI().click(new NamedWidgetLocator("repositoryUrlCombo"));
        getUI().enterText(nexusURL + "/content/groups/public/");
        getUI().click(new NamedWidgetLocator("retrieveButton"));
        getUI().wait(new JobsCompleteCondition());
        getUI().wait(new SWTIdleCondition());
        getUI().click(new ButtonLocator("OK"));
        getUI().wait(new ShellDisposedCondition("Add Repository Index"));
        getUI().contextClick(new TreeItemLocator("central-remote.*", new ViewLocator(
            "org.maven.ide.eclipse.views.MavenIndexesView")), "Update Index");
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
          project.delete(true, null);
        }
        return Status.OK_STATUS;
      }
    };

    job.setRule(ResourcesPlugin.getWorkspace().getRuleFactory().buildRule());
    job.schedule();
    getUI().wait(new JobsCompleteCondition(), 600000);

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
     
    IEditorPart editor = (IEditorPart)UIThreadTask.executeOnEventQueue(new UIThreadTask() {

       public Object runEx() throws Exception {
         return IDE.openEditor(getActivePage(), f, true);
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
    getUI().keyClick(SWT.CTRL, 'f');
    getUI().wait(new ShellShowingCondition(FIND_REPLACE));
    getUI().enterText(src);
    getUI().keyClick(WT.CR); // "find"
    getUI().close(new SWTWidgetLocator(Shell.class, FIND_REPLACE));
    getUI().wait(new ShellDisposedCondition(FIND_REPLACE));
  }

  protected void replaceText(String src, String target) throws WaitTimedOutException, WidgetSearchException {
    getUI().keyClick(SWT.CTRL, 'f');
    getUI().wait(new ShellShowingCondition(FIND_REPLACE));

    getUI().enterText(src);
    getUI().keyClick(WT.TAB);
    ScreenCapture.createScreenCapture();

    getUI().enterText(target);

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

  /**
   * Import a project and assert it has no markers of SEVERITY_ERROR
   */
  protected File doImport(String projectPath) throws Exception {
    File tempDir = unzipProject(projectPath);

    try {
      getUI().click(new MenuItemLocator("File/Import..."));
      getUI().wait(new ShellShowingCondition("Import"));
      getUI().click(new FilteredTreeItemLocator("General/Maven Projects"));
      getUI().click(new ButtonLocator("&Next >"));
      getUI().wait(new SWTIdleCondition());
      getUI().enterText(tempDir.getCanonicalPath());
      getUI().keyClick(SWT.CR);
      Thread.sleep(2000);
      getUI().click(new ButtonLocator("&Finish"));
      getUI().wait(new ShellDisposedCondition("Import Maven Projects"));
      Thread.sleep(5000);
      getUI().wait(new JobsCompleteCondition(), 300000);
      assertProjectsHaveNoErrors();

    } catch(Exception ex) {
      deleteDirectory(tempDir);
      throw ex;
    }

    return tempDir;
  }

  protected IViewPart showView(final String id) throws Exception {
    IViewPart part = (IViewPart) UIThreadTask.executeOnEventQueue(new UIThreadTask() {
      public Object runEx() throws Exception {
        return getActivePage().showView(id);
      }
    });

    getUI().wait(new SWTIdleCondition());
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
    getUI().click(locator);
    getUI().keyClick(SWT.MOD1, 'a');
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
    getUI().wait(new SWTIdleCondition());
    getUI().click(new ComboItemLocator("svn", new NamedWidgetLocator("mavenCheckoutLocation.typeCombo")));
    getUI().setFocus(new NamedWidgetLocator("mavenCheckoutLocation.urlCombo"));
    getUI().enterText(url);
    getUI().click(new ButtonLocator("&Finish"));
    getUI().wait(new ShellDisposedCondition("Checkout as Maven project from SCM"));

    getUI().wait(new JobsCompleteCondition(), 300000);

    Thread.sleep(5000);
  }
  
  protected void installTomcat6() throws Exception {
    
    String tomcatInstallLocation = System.getProperty(TOMCAT_INSTALL_LOCATION_PROPERTY);
    if (tomcatInstallLocation == null) {
      tomcatInstallLocation = DEFAULT_TOMCAT_INSTALL_LOCATION;
    }
    
    assertTrue("Can't locate tomcat installation: " + tomcatInstallLocation, new File(tomcatInstallLocation).exists());
    // Install the Tomcat server 
    showView("org.eclipse.wst.server.ui.ServersView");

    Thread.sleep(5000);
    
    getUI().contextClick(new SWTWidgetLocator(Tree.class, new ViewLocator("org.eclipse.wst.server.ui.ServersView")),
        "Ne&w/Server");
    getUI().wait(new ShellShowingCondition("New Server"));
    Thread.sleep(2000);
    getUI().click(new FilteredTreeItemLocator("Apache/Tomcat v6.0 Server"));
    getUI().click(new ButtonLocator("&Next >"));
    replaceText(new LabeledTextLocator("Tomcat installation &directory:"), tomcatInstallLocation);
    getUI().click(new ButtonLocator("&Finish"));
    getUI().wait(new ShellDisposedCondition("New Server"));
    getUI().wait(new JobsCompleteCondition());
  }
  
  protected Model getModel(final MavenPomEditor editor) throws Exception {
    Model model = (Model) UIThreadTask.executeOnEventQueue(new UIThreadTask() {

      public Object runEx() throws Exception {
        return editor.readProjectDocument();
      }
    });
    return model;
  }

 
}
