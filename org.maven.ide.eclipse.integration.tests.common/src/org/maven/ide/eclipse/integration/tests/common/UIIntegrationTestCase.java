/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.integration.tests.common;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.codehaus.plexus.util.IOUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWTException;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.utils.SWTUtils;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.eclipse.swtbot.swt.finder.widgets.TimeoutException;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.maven.ide.components.pom.Model;
import org.maven.ide.eclipse.editor.pom.MavenPomEditor;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

/**
 * @author rseddon
 * @author Marvin Froeder
 * 
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public abstract class UIIntegrationTestCase {
	protected static SWTWorkbenchBot bot;

	protected static final IProgressMonitor monitor = new NullProgressMonitor();

	@BeforeClass
	public static void beforeClass() throws Exception {
		bot = new SWTWorkbenchBot();
		try {
			bot.viewByTitle("Welcome").close();
		} catch (WidgetNotFoundException ex) {
			// already closed by another test
		}

		bot.perspectiveByLabel("Java").activate();
	}

	@AfterClass
	public static void sleep() throws Exception {
		removeServer();
		clearProjects();
		takeScreenShot("cleared projects");

		bot.sleep(1000);
	}

	@After
	public void finalShot() throws IOException {
		takeScreenShot(getClass().getSimpleName());
	}

	public static void takeScreenShot(String classifier) throws IOException {
		SWTUtils.captureScreenshot(File.createTempFile("swtbot-",
				"-" + classifier + ".png", new File("target"))
				.getAbsolutePath());
	}

	public static void takeScreenShot() throws IOException {
		takeScreenShot("screen");
	}


	protected void importZippedProject(File f) throws IOException {
		try {
			bot.menu("File").menu("Import...").click();
			SWTBotShell shell = bot.shell("Import");
			try {
				shell.activate();

				bot.tree().expandNode("General").select(
						"Existing Projects into Workspace");
				bot.button("Next >").click();
				// bot.button("Select root directory:").click();
				bot.radio("Select archive file:").click();
				bot.text(1).setText(f.getCanonicalPath());

				bot.button("Refresh").click();
				bot.button("Finish").click();
			} finally {
				SwtbotUtil.waitForClose(shell);
			}

			waitForAllBuildsToComplete();
		} finally {
			f.delete();
		}
	}

	protected static void waitForAllBuildsToComplete() {
		bot.sleep(5000);

		try {
			waitForJobsToComplete(monitor);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}

	}

	protected void createNewFolder(String projectName, String folderName) {
		// Add a new src folder with simple source file
		SWTBotTree tree = selectProject(projectName, true);

		ContextMenuHelper.clickContextMenu(tree, "New", "Folder");

		SWTBotShell shell = bot.shell("New Folder");
		try {
			shell.activate();

			bot.textWithLabel("Folder name:").setText(folderName);
			bot.button("Finish").click();

		} finally {
			SwtbotUtil.waitForClose(shell);
		}
	}

	protected File importMavenProjects(String pluginId, String projectPath)
			throws Exception {
		File tempDir = unzipProject(pluginId, projectPath);
		waitForAllBuildsToComplete();
		// try {
		// getUI().click(new ButtonLocator("Cancel"));
		// // if there is a dialog up here, take a screenshot but get rid of it
		// // - so we can keep going
		// ScreenCapture.createScreenCapture();
		// } catch (Exception e) {
		// // make sure that there are no dialogs up here
		// }fi
		try {
			bot.menu("File").menu("Import...").click();

			SWTBotShell shell = bot.shell("Import");
			try {
				shell.activate();

				bot.tree().expandNode("Maven")
						.select("Existing Maven Projects");
				bot.button("Next >").click();
				bot.comboBoxWithLabel("Root Directory:").setText(
						tempDir.getCanonicalPath());

				bot.button("Refresh").click();
				bot.button("Finish").click();
			} finally {
				SwtbotUtil.waitForClose(shell);
			}

			waitForAllBuildsToComplete();

		} catch (Exception ex) {
			deleteDirectory(tempDir);
			throw ex;
		}

		return tempDir;
	}

	protected void openResource(String resourceName) {
		bot.menu("Navigate").menu("Open Resource...").click();
		SWTBotShell shell = bot.shell("Open Resource");
		try {
			shell.activate();

			bot.text().setText(resourceName);
			bot.button("Open").click();
		} finally {
			SwtbotUtil.waitForClose(shell);
		}
	}

	protected void checkoutProjectsFromSVN(String url) throws Exception {
		bot.menu("File").menu("Import...").click();

		SWTBotShell shell = bot.shell("Import");
		try {
			shell.activate();

			bot.tree().expandNode("Maven").select(
					"Check out Maven Projects from SCM");
			bot.button("Next >").click();
			// for some reason, in eclipse 3.5.1 and WT, the direct combo
			// selection
			// is
			// not triggering the UI events, so the finish button never gets
			// enabled
			// getUI().click(new ComboItemLocator("svn", new
			// NamedWidgetLocator("mavenCheckoutLocation.typeCombo")));
			// getUI().setFocus(
			// new NamedWidgetLocator("mavenCheckoutLocation.typeCombo"));
			// for (int i = 0; i < 9; i++) {
			// getUI().keyClick(WT.ARROW_DOWN);
			// }
			try {
				bot.comboBoxWithLabel("SCM URL:").setSelection("svn");
			} catch (RuntimeException ex) {
				throw new RuntimeException("Available options: "
						+ Arrays.asList(bot.comboBoxWithLabel("SCM URL:")
								.items()), ex);
			}
			bot.comboBox(1).setText(url);

			bot.button("Finish").click();
		} finally {
			SwtbotUtil.waitForClose(shell);
		}

		waitForAllBuildsToComplete();
	}

	public void importZippedProject(String pluginID, String pluginPath)
			throws Exception {
		File f = copyPluginResourceToTempFile(pluginID, pluginPath);
		try {
			importZippedProject(f);
		} finally {
			f.delete();
		}
	}

	protected IViewPart showView(final String id) throws Exception {
		IViewPart part = (IViewPart) UIThreadTask
				.executeOnEventQueue(new UIThreadTask() {
					public Object runEx() throws Exception {
						IViewPart part = getActivePage().showView(id);

						return part;
					}
				});

		waitForAllBuildsToComplete();
		Assert.assertFalse(part == null);

		SWTBotView view = bot.viewById(id);
		view.show();

		return part;
	}

	protected static SWTBotView openView(final String id) {
		SWTBotView view;
		try {
			view = bot.viewById(id);
		} catch (WidgetNotFoundException e) {
			IViewPart part;
			try {
				part = (IViewPart) UIThreadTask
						.executeOnEventQueue(new UIThreadTask() {
							public Object runEx() throws Exception {
								IViewPart part = getActivePage().showView(id);

								return part;
							}
						});
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}

			Assert.assertFalse(part == null);

			view = bot.viewById(id);
			view.show();
		}

		return view;
	}

	protected void updateProjectConfiguration(String projectName)
			throws Exception {
		// SWTBotView packageExplorerView = bot.viewByTitle("Package Explorer");
		//
		// SWTBotTree tree =
		// packageExplorerView.bot().tree().select(projectName);
		//
		// ContextMenuHelper.clickContextMenu(tree, "Maven",
		// "Update Project Configuration");

		SWTBotTree tree = selectProject(projectName, true);

		ContextMenuHelper.clickContextMenu(tree, "Maven",
				"Update Project Configuration");

		waitForAllBuildsToComplete();
	}

	protected SWTBotTree selectProject(String projectName, boolean searchForIt) {
		SWTBotTree tree = bot.viewByTitle("Package Explorer").bot().tree();
		SWTBotTreeItem treeItem = null;
		try {
			treeItem = tree.getTreeItem(projectName);
		} catch (WidgetNotFoundException ex) {
			if (searchForIt) {
				SWTBotTreeItem[] allItems = tree.getAllItems();
				for (SWTBotTreeItem item : allItems) {
					// workaround required due to SVN/CVS that does add extra
					// informations to project name
					if (item.getText().contains(projectName)) {
						treeItem = item;
						break;
					}
				}
			}

			if (treeItem == null) {
				throw ex;
			}
		}
		treeItem.select();
		return tree;
	}

	protected void installTomcat6() throws Exception {

		String tomcatInstallLocation = System
				.getProperty(TOMCAT_INSTALL_LOCATION_PROPERTY);
		if (tomcatInstallLocation == null) {
			tomcatInstallLocation = DEFAULT_TOMCAT_INSTALL_LOCATION;
		}

		Assert.assertTrue("Can't locate tomcat installation: "
				+ tomcatInstallLocation, new File(tomcatInstallLocation)
				.exists());
		// Install the Tomcat server

		Thread.sleep(5000);

		showView(SERVERS_VIEW_ID);
		SWTBotView serversView = bot.viewById(SERVERS_VIEW_ID);

		SWTBotTree tree = serversView.bot().tree();
		Assert.assertEquals("Server view already contains a server "
				+ tree.getAllItems(), 0, tree.getAllItems().length);

		ContextMenuHelper.clickContextMenu(tree, "New", "Server");

		SWTBotShell shell = bot.shell("New Server");
		try {
			shell.activate();

			bot.tree().expandNode("Apache").select("Tomcat v6.0 Server");
			bot.button("Next >").click();

			SWTBotButton b = bot.button("Finish");
			if (!b.isEnabled()) {
				// First time...
				bot.textWithLabel("Tomcat installation &directory:").setText(
						tomcatInstallLocation);
			}
			b.click();
		} finally {
			SwtbotUtil.waitForClose(shell);
		}

		waitForAllBuildsToComplete();
	}

	protected void deployProjectsIntoTomcat() throws Exception {
		// Deploy the test project into tomcat
		SWTBotView serversView = bot.viewById(SERVERS_VIEW_ID);

		SWTBotTree tree = serversView.bot().tree().select(0);
		if (isEclipseVersion(3, 5)) {
			ContextMenuHelper.clickContextMenu(tree, "Add and Remove...");
		} else {
			ContextMenuHelper.clickContextMenu(tree,
					"Add and Remove Projects...");
		}
		String title = isEclipseVersion(3, 5) ? "Add and Remove..."
				: "Add and Remove Projects";

		SWTBotShell shell = bot.shell(title);
		try {
			shell.activate();
			bot.button("Add All >>").click();
			bot.button("Finish").click();
		} catch (TimeoutException ex) {
			takeScreenShot("Add all");
			throw ex;
		} finally {
			SwtbotUtil.waitForClose(shell);
		}

		ContextMenuHelper.clickContextMenu(tree, "Start");

		waitForAllBuildsToComplete();
		// getUI().click(new CTabItemLocator("Servers"));
		// Thread.sleep(3000);
	}

	protected void shutdownServer() {
		try {
			// shutdown the server
			SWTBotView serversView = bot.viewById(SERVERS_VIEW_ID);
			SWTBotTree tree = serversView.bot().tree().select(0);

			ContextMenuHelper.clickContextMenu(tree, "Stop");

			waitForAllBuildsToComplete();

			SWTBotShell shell = bot.shell("Terminate Server");
			try {
				shell.activate();
				bot.button("OK").click();
			} finally {
				SwtbotUtil.waitForClose(shell);
			}
		} catch (WidgetNotFoundException ex) {
			// this only happen when server takes too long to stop
		}
	}

	public static void removeServer() {
		// shutdown the server
		try {
			SWTBotView serversView = bot.viewById(SERVERS_VIEW_ID);
			SWTBotTree tree = serversView.bot().tree();

			for (int i = 0; i < tree.getAllItems().length; i++) {
				SWTBotTree server = tree.select(0);
				ContextMenuHelper.clickContextMenu(server, "Delete");

				SWTBotShell shell = bot.shell("Delete Server");
				try {
					shell.activate();

					bot.button("OK").click();
				} finally {
					SwtbotUtil.waitForClose(shell);
				}
			}

			waitForAllBuildsToComplete();
		} catch (WidgetNotFoundException e) {
			// not an issue, mean this is not a server test
			return;
		} catch (SWTException e) {
			if (e.getCause() instanceof WidgetNotFoundException) {
				return; // not a problem
			} else {
				throw e;
			}
		}
	}

	protected void restartServer(boolean republish)
			throws WidgetNotFoundException, IOException {
		// shutdown the server
		SWTBotView serversView = bot.viewById(SERVERS_VIEW_ID);
		serversView.show();

		SWTBotTree tree;
		try {
			tree = serversView.bot().tree().select(0);
		} catch (WidgetNotFoundException ex) {
			takeScreenShot("restart server");
			throw ex;
		}

		shutdownServer();

		if (republish) {
			ContextMenuHelper.clickContextMenu(tree, "Publish");
			waitForAllBuildsToComplete();
		}

		ContextMenuHelper.clickContextMenu(tree, "Start");
		waitForAllBuildsToComplete();
	}

	protected void findText(String src) {
		findTextWithWrap(src, false);
	}

	public static final String FIND_REPLACE = "Find/Replace";

	protected void findTextWithWrap(String src, boolean wrap) {
		bot.menu("Edit").menu("Find/Replace...").click();

		SWTBotShell shell = bot.shell(FIND_REPLACE);
		try {
			shell.activate();

			bot.comboBoxWithLabel("Find:").setText(src);
			if (wrap) {
				bot.checkBox("Wrap search").select();
			} else {
				bot.checkBox("Wrap search").deselect();
			}

			bot.button("Find").click();
		} finally {
			SwtbotUtil.waitForClose(shell);
		}
	}

	protected void replaceText(String src, String target) {
		replaceTextWithWrap(src, target, false);
	}

	protected void replaceTextWithWrap(String src, String target, boolean wrap) {
		bot.menu("Edit").menu("Find/Replace...").click();

		SWTBotShell shell = bot.shell(FIND_REPLACE);
		try {
			shell.activate();

			bot.comboBoxWithLabel("Find:").setText(src);
			bot.comboBoxWithLabel("Replace with:").setText(target);

			if (wrap) {
				bot.checkBox("Wrap search").select();
			} else {
				bot.checkBox("Wrap search").deselect();
			}

			bot.button("Replace All").click();

			shell.close();
		} finally {
			SwtbotUtil.waitForClose(shell);
		}

	}

	public static boolean isEclipseVersion(int major, int minor) {
		Bundle bundle = ResourcesPlugin.getPlugin().getBundle();
		String version = (String) bundle.getHeaders().get(
				org.osgi.framework.Constants.BUNDLE_VERSION);
		Version v = org.osgi.framework.Version.parseVersion(version);
		return v.getMajor() == major && v.getMinor() == minor;
	}

	protected static IWorkbenchPage getActivePage() {
		IWorkbench workbench = PlatformUI.getWorkbench();
		return workbench.getWorkbenchWindows()[0].getActivePage();
	}

	protected File copyPluginResourceToTempFile(String plugin, String file)
			throws MalformedURLException, IOException {
		URL url = FileLocator.find(Platform.getBundle(plugin), new Path("/"
				+ file), null);
		return copyPluginResourceToTempFile(plugin, url);
	}

	protected File copyPluginResourceToTempFile(String plugin, URL url)
			throws MalformedURLException, IOException {
		File f = File.createTempFile("temp", "."
				+ new Path(url.getFile()).getFileExtension());
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

	/**
	 * Import a project and assert it has no markers of SEVERITY_ERROR
	 */
	protected File doImport(String pluginId, String projectPath)
			throws Exception {
		return doImport(pluginId, projectPath, true);
	}

	protected File doImport(String pluginId, String projectPath,
			boolean assertNoErrors) throws Exception {
		File tempDir = importMavenProjects(pluginId, projectPath);
		if (assertNoErrors) {
			assertProjectsHaveNoErrors();
		}
		return tempDir;
	}

	protected void assertProjectsHaveNoErrors() throws Exception {
		StringBuffer messages = new StringBuffer();
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot()
				.getProjects();
		int count = 0;
		for (IProject project : projects) {
			if ("Servers".equals(project.getName())) {
				continue;
			}
			if (count >= 10) {
				break;
			}
			IMarker[] markers = project.findMarkers(IMarker.PROBLEM, true,
					IResource.DEPTH_INFINITE);
			for (int i = 0; i < markers.length; i++) {
				if (markers[i].getAttribute(IMarker.SEVERITY, 0) == IMarker.SEVERITY_ERROR) {
					count++;
					messages.append('\t');
					if (messages.length() > 0) {
						messages.append(System.getProperty("line.separator"));
					}
					messages.append(project.getName()
							+ ":"
							+ markers[i].getAttribute(IMarker.LOCATION,
									"unknown location")
							+ " "
							+ markers[i].getAttribute(IMarker.MESSAGE,
									"unknown message"));
				}
			}
		}
		if (count > 0) {
			Assert.fail("One or more compile errors found:"
					+ System.getProperty("line.separator") + messages);

		}
	}

	private void unzipFile(String pluginId, String pluginPath, File dest)
			throws IOException {
		URL url = FileLocator.find(Platform.getBundle(pluginId), new Path("/"
				+ pluginPath), null);
		InputStream is = new BufferedInputStream(url.openStream());
		ZipInputStream zis = new ZipInputStream(is);
		try {
			ZipEntry entry = zis.getNextEntry();
			while (entry != null) {
				File f = new File(dest, entry.getName());
				if (entry.isDirectory()) {
					f.mkdirs();
				} else {
					if (!f.getParentFile().exists()) {
						f.getParentFile().mkdirs();
					}
					OutputStream os = new BufferedOutputStream(
							new FileOutputStream(f));
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

	public File unzipProject(String pluginId, String pluginPath)
			throws Exception {
		File tempDir = createTempDir("sonatype");
		unzipFile(pluginId, pluginPath, tempDir);
		return tempDir;
	}

	protected File createTempDir(String prefix) throws IOException {
		File temp = null;
		temp = File.createTempFile(prefix, "");
		if (!temp.delete()) {
			throw new IOException("Unable to delete temp file:"
					+ temp.getName());
		}
		if (!temp.mkdir()) {
			throw new IOException("Unable to create temp dir:" + temp.getName());
		}
		return temp;
	}

	protected void deleteDirectory(File dir) {
		File[] fileArray = dir.listFiles();
		if (fileArray != null) {
			for (int i = 0; i < fileArray.length; i++) {
				if (fileArray[i].isDirectory())
					deleteDirectory(fileArray[i]);
				else
					fileArray[i].delete();
			}
		}
		dir.delete();
	}

	// Location of tomcat 6 installation which can be used by Eclipse WTP tests
	private static final String DEFAULT_TOMCAT_INSTALL_LOCATION = "c:/test/apache-tomcat-6.0.18";

	// Set this system property to override DEFAULT_TOMCAT_INSTALL_LOCATION
	private static final String TOMCAT_INSTALL_LOCATION_PROPERTY = "tomcat.install.location";

	public static final String SERVERS_VIEW_ID = "org.eclipse.wst.server.ui.ServersView";

	public static final String TOMCAT_SERVER_NAME = "Tomcat.*";

	public static void clearProjects() {
		WorkspaceJob job = new WorkspaceJob("deleting test projects") {
			public IStatus runInWorkspace(IProgressMonitor monitor)
					throws CoreException {
				ResourcesPlugin.getWorkspace().getRoot().refreshLocal(
						IResource.DEPTH_INFINITE, null);
				IProject[] projects = ResourcesPlugin.getWorkspace().getRoot()
						.getProjects();
				for (IProject project : projects) {
					// project.delete(true, true, monitor);
					project.delete(true, monitor);
				}
				return Status.OK_STATUS;
			}
		};
		job
				.setRule(ResourcesPlugin.getWorkspace().getRuleFactory()
						.buildRule());
		job.schedule();

		try {
			job.join();
		} catch (InterruptedException ex) {
			throw new RuntimeException(ex);
		}
	}

	protected MavenPomEditor openPomFile(String name) throws Exception {

		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IFile file = root.getFile(new Path(name));

		final IEditorInput editorInput = new FileEditorInput(file);
		MavenPomEditor editor = (MavenPomEditor) UIThreadTask
				.executeOnEventQueue(new UIThreadTask() {

					public Object runEx() throws Exception {
						IEditorPart part = getActivePage().openEditor(
								editorInput,
								"org.maven.ide.eclipse.editor.MavenPomEditor",
								true);
						if (part instanceof MavenPomEditor) {
							return part;
						}
						return null;
					}
				});

		waitForAllBuildsToComplete();

		return editor;
	}

	protected Model getModel(final MavenPomEditor editor) throws Exception {
		Model model = (Model) UIThreadTask
				.executeOnEventQueue(new UIThreadTask() {

					public Object runEx() throws Exception {
						return editor.readProjectDocument();
					}
				});
		return model;
	}

	// TODO duplicated from AsbtractMavenProjectTestCase

	public static void waitForJobsToComplete(IProgressMonitor monitor)
			throws InterruptedException, CoreException {
		/*
		 * First, make sure refresh job gets all resource change events
		 * 
		 * Resource change events are delivered after
		 * WorkspaceJob#runInWorkspace returns and during IWorkspace#run. Each
		 * change notification is delivered by only one thread/job, so we make
		 * sure no other workspaceJob is running then call IWorkspace#run from
		 * this thread.
		 * 
		 * Unfortunately, this does not catch other jobs and threads that call
		 * IWorkspace#run so we have to hard-code workarounds
		 * 
		 * See
		 * http://www.eclipse.org/articles/Article-Resource-deltas/resource-deltas
		 * .html
		 */
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IJobManager jobManager = Job.getJobManager();
		jobManager.suspend();
		try {
			Job[] jobs = jobManager.find(null);
			for (int i = 0; i < jobs.length; i++) {
				Job job = jobs[i];
				if (!job.isSystem()) {
					job.join();
				}
			}
			workspace.run(new IWorkspaceRunnable() {
				public void run(IProgressMonitor monitor) throws CoreException {
				}
			}, workspace.getRoot(), 0, monitor);

			// Now we flush all background processing queues
			boolean processed = flushProcessingQueues(jobManager, monitor);
			for (int i = 0; i < 10 && processed; i++) {
				processed = flushProcessingQueues(jobManager, monitor);
			}

			Assert.assertFalse("Could not flush background processing queues: "
					+ getProcessingQueues(jobManager), processed);
		} finally {
			jobManager.resume();
		}

	}

	private static boolean flushProcessingQueues(IJobManager jobManager,
			IProgressMonitor monitor) throws InterruptedException {
		boolean processed = false;
		for (Job queue : getProcessingQueues(jobManager)) {
			queue.join();
		}
		return processed;
	}

	private static List<Job> getProcessingQueues(IJobManager jobManager) {
		ArrayList<Job> queues = new ArrayList<Job>();
		for (Job job : jobManager.find(null)) {
			if (!job.isSystem()) {
				queues.add(job);
			}
		}
		return queues;
	}

}
