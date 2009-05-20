package org.maven.ide.eclipse.integration.tests;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.project.IMavenProjectFacade;

import com.windowtester.runtime.IUIContext;
import com.windowtester.runtime.swt.condition.shell.ShellDisposedCondition;
import com.windowtester.runtime.swt.condition.shell.ShellShowingCondition;
import com.windowtester.runtime.swt.locator.ButtonLocator;
import com.windowtester.runtime.swt.locator.NamedWidgetLocator;
import com.windowtester.runtime.swt.locator.TreeItemLocator;
import com.windowtester.runtime.swt.locator.eclipse.ViewLocator;

public class MngEclipse1394RefactorRenameTest extends UIIntegrationTestCase {

	/**
	 * Main test method.
	 */
	public void testMngEclipse1394RefactorRename() throws Exception {
		IUIContext ui = getUI();
		
    createArchetypeProjct("maven-archetype-quickstart", "project");
    IMavenProjectFacade mavenProject = MavenPlugin.getDefault().getMavenProjectManager().getMavenProject("org.sonatype.test", "project", "0.0.1-SNAPSHOT");
    assertNotNull(mavenProject);
    
		ui.contextClick(new TreeItemLocator("project/pom.xml", new ViewLocator("org.eclipse.jdt.ui.PackageExplorer")), "Refactor/Rename Maven Artifact...");
		ui.wait(new ShellDisposedCondition("Progress Information"));
		ui.wait(new ShellShowingCondition("Rename Maven Artifact"));
	  replaceText(new NamedWidgetLocator("groupId"), "x.y.z");
	  replaceText(new NamedWidgetLocator("artifactId"), "project2");
		replaceText(new NamedWidgetLocator("version"), "1.1.1");
		ui.click(new NamedWidgetLocator("rename"));
		ui.click(new ButtonLocator("OK"));
	//	ui.wait(new ShellDisposedCondition("Rename Maven Artifact"));
		waitForAllBuildsToComplete();
		
		IProject project2 = ResourcesPlugin.getWorkspace().getRoot().getProject("project2");
		assertTrue(project2.exists());
		mavenProject = MavenPlugin.getDefault().getMavenProjectManager().getMavenProject("org.sonatype.test", "project", "0.0.1-SNAPSHOT");
		assertNull(mavenProject);
		IMavenProjectFacade mavenProject2 = MavenPlugin.getDefault().getMavenProjectManager().getMavenProject("x.y.z", "project2", "1.1.1");
		assertNotNull(mavenProject2);
		
	}

}