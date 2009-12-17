package org.maven.ide.eclipse.integration.tests;

import org.eclipse.core.resources.IProject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.editor.pom.MavenPomEditor;
import org.maven.ide.eclipse.project.IMavenProjectFacade;

import com.windowtester.runtime.IUIContext;
import com.windowtester.runtime.WidgetSearchException;
import com.windowtester.runtime.locator.IWidgetLocator;
import com.windowtester.runtime.swt.condition.SWTIdleCondition;
import com.windowtester.runtime.swt.condition.shell.ShellDisposedCondition;
import com.windowtester.runtime.swt.condition.shell.ShellShowingCondition;
import com.windowtester.runtime.swt.locator.ButtonLocator;
import com.windowtester.runtime.swt.locator.CTabItemLocator;
import com.windowtester.runtime.swt.locator.FilteredTreeItemLocator;
import com.windowtester.runtime.swt.locator.NamedWidgetLocator;
import com.windowtester.runtime.swt.locator.TreeItemLocator;
import com.windowtester.runtime.swt.locator.eclipse.ViewLocator;

public class MngEclipse1515LifecyleMappingTest extends M2EUIIntegrationTestCase {

  public MngEclipse1515LifecyleMappingTest(){
    super();
    this.setSkipIndexes(true);
  }
  
	/**
	 * Main test method.
	 */
	public void testMgnEclipse1515() throws Exception {
//	  setXmlPrefs();
//	  String projectName = "lifecycleTest";
//		IUIContext ui = getUI();
//		IProject project = createQuickstartProject(projectName);
//    assertNotNull(project);
//    
//    //open project prefs, navigate to maven->lifecycle mapping, make sure that the 'generic' lifecycle mapping is showing
//    showGenericLifecycle(projectName);
//    
//    
//    openPomFile(projectName+"/pom.xml");
//    ui.click(new CTabItemLocator("pom.xml"));
//    
//    //then set to customizable and make sure that one is showing
//    findText("</project");
//    getUI().keyClick(SWT.ARROW_LEFT);
//    
//    getUI().enterText("<build><plugins><plugin> <groupId>org.maven.ide.eclipse</ <artifactId>lifecycle-mapping</ <version>0.9.9-SNAPSHOT</  <configuration><mappingId>customizable</ <configurators></ <mojoExecutions></ </ </</</");
//    getUI().keyClick(SWT.MOD1, 's');
//    waitForAllBuildsToComplete();
//    showCustomizableLifecycle(projectName);
//
//    //then, back to generic
//    openPomFile(projectName+"/pom.xml");
//    ui.click(new CTabItemLocator("pom.xml"));
//    replaceTextWithWrap("customizable", "generic", true);
//    getUI().wait(new ShellDisposedCondition(FIND_REPLACE));
//    getUI().keyClick(SWT.MOD1, 's');
//    waitForAllBuildsToComplete();
//    showGenericLifecycle(projectName);
//    
//    //then switch to empty lifecycle mapping
//    openPomFile(projectName+"/pom.xml");
//    ui.click(new CTabItemLocator("pom.xml"));
//    replaceTextWithWrap("generic", "NULL", true);
//    getUI().wait(new ShellDisposedCondition(FIND_REPLACE));
//    getUI().keyClick(SWT.MOD1, 's');
//    waitForAllBuildsToComplete();
//    showEmptyLifecycle(projectName);
	}
	
  protected void selectEditorTab(final String id) throws Exception {
    final MavenPomEditor editor = (MavenPomEditor) getActivePage().getActiveEditor();
    Display.getDefault().syncExec(new Runnable() {
      public void run() {
        editor.setActivePage(id);
      }
    });
    getUI().wait(new SWTIdleCondition());
  }
  /**
   * @throws WidgetSearchException 
   * 
   */
  private void showEmptyLifecycle(String projectName) throws WidgetSearchException {
    showLifecyclePropsPage(projectName);
    IWidgetLocator widgetLocator = getUI().find(new NamedWidgetLocator("noInfoLabel"));
    assertNotNull(widgetLocator);
    hideLifecyclePropsPage(projectName);
  }


  /**
   * @throws WidgetSearchException 
   * 
   */
  private void showCustomizableLifecycle(String projectName) throws WidgetSearchException {
    showLifecyclePropsPage(projectName);
    IWidgetLocator widgetLocator = getUI().find(new NamedWidgetLocator("projectConfiguratorsTable"));
    assertNotNull(widgetLocator);
    hideLifecyclePropsPage(projectName);
  }

  private void hideLifecyclePropsPage(String projectName) throws WidgetSearchException{
    getUI().click(new ButtonLocator("Cancel"));
    getUI().wait(new ShellDisposedCondition("Properties for "+projectName)); 
  }
  /**
   * @throws WidgetSearchException 
   * 
   */
  private void showLifecyclePropsPage(String projectName) throws WidgetSearchException {
    getUI().contextClick(new TreeItemLocator(projectName, new ViewLocator("org.eclipse.jdt.ui.PackageExplorer")), "Properties");
    getUI().wait(new ShellShowingCondition("Properties for "+projectName));
    getUI().click(new FilteredTreeItemLocator("Maven/Lifecycle Mapping"));
    getUI().wait(new SWTIdleCondition());
  }

  /**
   * @throws WidgetSearchException 
   * 
   */
  private void showGenericLifecycle(String projectName) throws WidgetSearchException {
    showLifecyclePropsPage(projectName);
    try{
    IWidgetLocator widgetLocator = getUI().find(new NamedWidgetLocator("goalsText"));
    } catch(Exception e){
      e.printStackTrace();
    }
    hideLifecyclePropsPage(projectName);
  }

}
