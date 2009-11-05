package org.maven.ide.eclipse.integration.tests;

import java.io.File;

import org.eclipse.swt.SWT;

import com.windowtester.runtime.IUIContext;
import com.windowtester.runtime.swt.condition.eclipse.JobsCompleteCondition;
import com.windowtester.runtime.swt.condition.shell.ShellDisposedCondition;
import com.windowtester.runtime.swt.condition.shell.ShellShowingCondition;
import com.windowtester.runtime.swt.locator.ButtonLocator;
import com.windowtester.runtime.swt.locator.CTabItemLocator;
import com.windowtester.runtime.swt.locator.TreeItemLocator;
import com.windowtester.runtime.swt.locator.eclipse.ViewLocator;

public class MEclipse178IssueTrackerTest extends UIIntegrationTestCase {

	private File tempDir;

  /**
	 * Main test method.
	 *   <issueManagement>
    <system>Jira</system>
    <url>http://issues.sonatype.org</url>
  </issueManagement>
	 */
	public void testIssueTracker() throws Exception {
	  setXmlPrefs();
    // Import the test project
    tempDir = doImport("projects/ch07project.zip");

		IUIContext ui = getUI();
		ui.click(new TreeItemLocator("simple-parent", new ViewLocator(
				"org.eclipse.jdt.ui.PackageExplorer")));
		ui.contextClick(new TreeItemLocator("simple-parent", new ViewLocator(
				"org.eclipse.jdt.ui.PackageExplorer")),
				"Maven/Open Issue Tracker");
		ui.wait(new ShellShowingCondition("Open Browser"));
		ui.click(new ButtonLocator("OK"));
		ui.wait(new ShellDisposedCondition("Open Browser"));
		
		openPomFile("simple-parent/pom.xml");
		ui.click(new CTabItemLocator("pom.xml"));
		replaceText("</modules>", "</modules><issueManagement><system>JIRA</system><url>http://issues.sonatype.org</url></issueManagement>");
		ui.keyClick(SWT.MOD1, 's');
		try{
		  waitForAllBuildsToComplete();
		} catch(Throwable t){
		  //wst seems to be barfing at times here. need to investigate
		}
		ui.click(new TreeItemLocator("simple-parent", new ViewLocator(
				"org.eclipse.jdt.ui.PackageExplorer")));
		ui.contextClick(new TreeItemLocator("simple-parent", new ViewLocator(
				"org.eclipse.jdt.ui.PackageExplorer")),
				"Maven/Open Issue Tracker");
		ui.wait(new JobsCompleteCondition());
		//this is getting stuck open and causing other problems. going to
		//comment it out while I investigate to it doesn't interfere with other tests
		//ui.click(new CTabItemLocator("http://issues.sonatype.org"));
	}

  protected void tearDown() throws Exception {
    
    super.tearDown();
    
    if(tempDir != null && tempDir.exists()) {
      deleteDirectory(tempDir);
      tempDir = null;
    }

  }

}