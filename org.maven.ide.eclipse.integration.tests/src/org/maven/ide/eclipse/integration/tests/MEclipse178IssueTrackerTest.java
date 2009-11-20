package org.maven.ide.eclipse.integration.tests;

import org.eclipse.swt.SWT;

import com.windowtester.runtime.IUIContext;
import com.windowtester.runtime.swt.condition.shell.ShellDisposedCondition;
import com.windowtester.runtime.swt.condition.shell.ShellShowingCondition;
import com.windowtester.runtime.swt.locator.ButtonLocator;
import com.windowtester.runtime.swt.locator.CTabItemLocator;
import com.windowtester.runtime.swt.locator.TreeItemLocator;
import com.windowtester.runtime.swt.locator.eclipse.ViewLocator;

public class MEclipse178IssueTrackerTest extends UIIntegrationTestCase {


	public void testIssueTracker() throws Exception {
	  setXmlPrefs();
	  createQuickstartProject("test-project");

		IUIContext ui = getUI();
		ui.click(new TreeItemLocator("test-project", new ViewLocator(
				"org.eclipse.jdt.ui.PackageExplorer")));
		ui.contextClick(new TreeItemLocator("test-project", new ViewLocator(
				"org.eclipse.jdt.ui.PackageExplorer")),
				"Maven/Open Issue Tracker");
		ui.wait(new ShellShowingCondition("Open Browser"));
		ui.click(new ButtonLocator("OK"));
		ui.wait(new ShellDisposedCondition("Open Browser"));
		
		openPomFile("test-project/pom.xml");
		ui.click(new CTabItemLocator("pom.xml"));
		replaceText("</dependencies>", "</dependencies><issueManagement><system>JIRA</system><url>http://issues.sonatype.org</url></issueManagement>");
		ui.keyClick(SWT.MOD1, 's');
		  waitForAllBuildsToComplete();
		ui.click(new TreeItemLocator("test-project", new ViewLocator(
				"org.eclipse.jdt.ui.PackageExplorer")));
		ui.contextClick(new TreeItemLocator("test-project", new ViewLocator(
				"org.eclipse.jdt.ui.PackageExplorer")),
				"Maven/Open Issue Tracker");
		Thread.sleep(10000);
		waitForAllBuildsToComplete();
		ui.click(new CTabItemLocator("http://issues.sonatype.org"));
	}


}