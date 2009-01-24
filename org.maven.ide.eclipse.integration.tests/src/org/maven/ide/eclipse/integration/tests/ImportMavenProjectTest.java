
package org.maven.ide.eclipse.integration.tests;

import java.io.File;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.swt.SWT;

import com.windowtester.runtime.swt.condition.SWTIdleCondition;
import com.windowtester.runtime.swt.condition.eclipse.JobsCompleteCondition;
import com.windowtester.runtime.swt.condition.shell.ShellDisposedCondition;
import com.windowtester.runtime.swt.condition.shell.ShellShowingCondition;
import com.windowtester.runtime.swt.locator.ButtonLocator;
import com.windowtester.runtime.swt.locator.FilteredTreeItemLocator;
import com.windowtester.runtime.swt.locator.MenuItemLocator;


public class ImportMavenProjectTest extends UIIntegrationTestCase {

  private File tempDir;

  public void testMultiModuleImport() throws Exception {
    tempDir = unzipProject("projects/commons-collections-3.2.1-src.zip");

    ui.click(new MenuItemLocator("File/Import..."));
    ui.wait(new ShellShowingCondition("Import"));
    ui.click(new FilteredTreeItemLocator("General/Maven Projects"));
    ui.click(new ButtonLocator("&Next >"));
    ui.wait(new SWTIdleCondition());
    ui.enterText(tempDir.getCanonicalPath());
    ui.keyClick(SWT.CR);
    ui.click(new ButtonLocator("&Finish"));
    ui.wait(new ShellDisposedCondition("Checkout as Maven project from SCM"));
    ui.wait(new JobsCompleteCondition(), 300000);

    IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
    for(IProject project : projects) {
      int severity = project.findMaxProblemSeverity(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
      assertTrue(IMarker.SEVERITY_ERROR > severity);
      assertTrue(project.hasNature(JavaCore.NATURE_ID));
      assertTrue(project.hasNature("org.maven.ide.eclipse.maven2Nature"));
    }

  }

  protected void oneTimeTearDown() throws Exception {

    super.oneTimeTearDown();

    if(tempDir != null && tempDir.exists()) {
      deleteDirectory(tempDir);
    }
  }

}
