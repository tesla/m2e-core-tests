package org.maven.ide.eclipse.integration.tests;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.ViewForm;
import org.eclipse.swt.widgets.Composite;

import com.windowtester.runtime.IUIContext;
import com.windowtester.runtime.WT;
import com.windowtester.runtime.locator.IWidgetLocator;
import com.windowtester.runtime.swt.condition.SWTIdleCondition;
import com.windowtester.runtime.swt.condition.eclipse.JobsCompleteCondition;
import com.windowtester.runtime.swt.condition.shell.ShellDisposedCondition;
import com.windowtester.runtime.swt.condition.shell.ShellShowingCondition;
import com.windowtester.runtime.swt.locator.ButtonLocator;
import com.windowtester.runtime.swt.locator.FilteredTreeItemLocator;
import com.windowtester.runtime.swt.locator.MenuItemLocator;
import com.windowtester.runtime.swt.locator.NamedWidgetLocator;
import com.windowtester.runtime.swt.locator.SWTWidgetLocator;

public class QuickStartCreateTest extends UIIntegrationTestCase {

	/**
	 * Main test method.
	 */
	public void testQuickStartCreate() throws Exception {
	   IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject("project");
	   assertFalse(project.exists());
	    
		IUIContext ui = getUI();
		ui.click(new SWTWidgetLocator(ViewForm.class, new SWTWidgetLocator(
				CTabFolder.class, 0, new SWTWidgetLocator(Composite.class))));
		ui.click(new MenuItemLocator("File/New/Project..."));
		ui.wait(new ShellShowingCondition("New Project"));
		ui.click(new FilteredTreeItemLocator("Plug-in Project"));
		ui.click(new FilteredTreeItemLocator("Maven/Maven Project"));
		ui.click(new ButtonLocator("&Next >"));
		ui.click(new ButtonLocator("&Next >"));
		ui.click(new ButtonLocator("&Next >"));
		ui.wait(new SWTIdleCondition());
		IWidgetLocator groupCombo = ui.find(new NamedWidgetLocator("groupId"));
		ui.setFocus(groupCombo);
		ui.enterText("org.sonatype.test");
		ui.setFocus(ui.find(new NamedWidgetLocator("artifactId")));
		ui.enterText("project");
		ui.click(new ButtonLocator("&Finish"));
		ui.wait(new ShellDisposedCondition("New Maven Project"));
		ui.wait(new JobsCompleteCondition());
		
		project = ResourcesPlugin.getWorkspace().getRoot().getProject("project");
		assertTrue(project.exists());
		int severity = project.findMaxProblemSeverity(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
		assertFalse(severity == IMarker.SEVERITY_ERROR);
		IFile f = project.getFile("src/main/java/org/sonatype/test/project/App.java");
		assertTrue(f.exists());
		f = project.getFile("pom.xml");
		assertTrue(f.exists());
		f = project.getFile("src/test/java/org/sonatype/test/project/AppTest.java");
		assertTrue(f.exists());
		
	}

}
