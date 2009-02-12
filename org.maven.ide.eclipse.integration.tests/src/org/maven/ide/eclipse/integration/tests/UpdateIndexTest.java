
package org.maven.ide.eclipse.integration.tests;

import org.eclipse.core.runtime.jobs.Job;
import org.maven.ide.eclipse.MavenPlugin;

import com.windowtester.runtime.IUIContext;
import com.windowtester.runtime.swt.condition.SWTIdleCondition;
import com.windowtester.runtime.swt.condition.eclipse.JobsCompleteCondition;
import com.windowtester.runtime.swt.condition.shell.ShellDisposedCondition;
import com.windowtester.runtime.swt.condition.shell.ShellShowingCondition;
import com.windowtester.runtime.swt.locator.ButtonLocator;
import com.windowtester.runtime.swt.locator.CTabItemLocator;
import com.windowtester.runtime.swt.locator.NamedWidgetLocator;
import com.windowtester.runtime.swt.locator.TreeItemLocator;
import com.windowtester.runtime.swt.locator.eclipse.ContributedToolItemLocator;
import com.windowtester.runtime.swt.locator.eclipse.ViewLocator;


public class UpdateIndexTest extends UIIntegrationTestCase {

  
  
  protected void oneTimeSetup() throws Exception {
    
    MavenPlugin.getDefault();
    Thread.sleep(5000);
    Job [] jobs = Job.getJobManager().find(null);
    for(int i = 0; i < jobs.length; i++ ) {
      if (jobs[i].getClass().getName().endsWith("IndexUpdaterJob"))  {
        jobs[i].cancel();
        break;
      }
    }
    
    IUIContext ui = getUI();
    showView("org.maven.ide.eclipse.views.MavenIndexesView");

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
    ui.wait(new JobsCompleteCondition(), 120000);
    
    super.oneTimeSetup();
  }

  /**
   * Main test method.
   */
  public void testUpdateIndex() throws Exception {
    
    IUIContext ui = getUI();
    showView("org.maven.ide.eclipse.views.MavenIndexesView");

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
    ui.wait(new JobsCompleteCondition(), 120000);
  }

}
