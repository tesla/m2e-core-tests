package org.maven.ide.eclipse.integration.tests;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.settings.Settings;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IViewPart;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.editor.pom.MavenPomEditor;
import org.maven.ide.eclipse.embedder.IMaven;
import org.maven.ide.eclipse.embedder.MavenRuntime;
import org.maven.ide.eclipse.embedder.MavenRuntimeManager;

import com.windowtester.runtime.ClickDescription;
import com.windowtester.runtime.WT;
import com.windowtester.runtime.WidgetSearchException;
import com.windowtester.runtime.locator.IWidgetLocator;
import com.windowtester.runtime.locator.IWidgetReference;
import com.windowtester.runtime.swt.condition.SWTIdleCondition;
import com.windowtester.runtime.swt.condition.shell.ShellDisposedCondition;
import com.windowtester.runtime.swt.condition.shell.ShellShowingCondition;
import com.windowtester.runtime.swt.locator.ButtonLocator;
import com.windowtester.runtime.swt.locator.CTabItemLocator;
import com.windowtester.runtime.swt.locator.FilteredTreeItemLocator;
import com.windowtester.runtime.swt.locator.MenuItemLocator;
import com.windowtester.runtime.swt.locator.NamedWidgetLocator;
import com.windowtester.runtime.swt.locator.TableItemLocator;
import com.windowtester.runtime.swt.locator.TreeItemLocator;

public class MngEclipse1526InstallationPreferencesTest extends UIIntegrationTestCase {

  protected String externalRuntime;
  
  public MngEclipse1526InstallationPreferencesTest(){
    super();
    this.setSkipIndexes(true);
    externalRuntime = "C:\\apache-maven-2.1.0";
  }
  
	/**
	 * Main test method.
	 */
	public void testInstallationPrefs() throws Exception {
	  
	  ButtonLocator addButton = new ButtonLocator("Add...");
	  ButtonLocator removeButton = new ButtonLocator("Remove");
	  //add external maven runtime
	  
    MavenRuntime newRuntime = MavenRuntimeManager.createExternalRuntime(externalRuntime);
    List<MavenRuntime> currRuntimes = MavenPlugin.getDefault().getMavenRuntimeManager().getMavenRuntimes();
    ArrayList<MavenRuntime> list = new ArrayList<MavenRuntime>(currRuntimes);
    list.add(newRuntime);
    MavenPlugin.getDefault().getMavenRuntimeManager().setRuntimes(list);
    MavenPlugin.getDefault().getMavenRuntimeManager().setDefaultRuntime(newRuntime);
    
		showInstallationPrefs();
		assertFalse(new ButtonLocator("Browse...").isEnabled(getUI()));
		
		IWidgetReference browseButtonRef = (IWidgetReference)getUI().find(new ButtonLocator("Browse..."));
		
		verifyGlobalSettingsValue(externalRuntime+"\\conf\\settings.xml");
		
		//now check the embedded and make sure its blank
		TableItemLocator locator = new TableItemLocator(WT.CHECK, "Embedded");
		getUI().click(locator);
		verifyGlobalSettingsValue("");
		assertTrue(new ButtonLocator("Browse...").isEnabled(getUI()));
    getUI().click(new NamedWidgetLocator("globalSettingsText"));
    getUI().enterText("resources/global_settings.xml");
    closeInstallationPrefs();
    
    //now open the index view and make sure 'local' has the right value, 
    showView("org.maven.ide.eclipse.views.MavenIndexesView");
    getUI().click(new CTabItemLocator("Maven Indexes"));
    String value = "testing_repo";
   
    String globalSettings = MavenPlugin.getDefault().getMavenRuntimeManager().getGlobalSettingsFile();
    IMaven lookup = MavenPlugin.lookup(IMaven.class);
    Settings settings = lookup.buildSettings(globalSettings, null);
    String localRepository = settings.getLocalRepository();
    
//    String globalSettingsFile = MavenPlugin.getDefault().getMavenRuntimeManager().getGlobalSettingsFile();
//    IWidgetLocator[] localRepos = getUI().findAll(new TreeItemLocator("*"+value));
//    assertNotNull(localRepos);
    
    
    
    Thread.sleep(3000);
    
    showUserSettingsPrefs();
    verifyRepoSettingsValue(value);
    Thread.sleep(1000);
    closeUserSettingPrefs();
    Thread.sleep(1000);
	}

	protected ButtonLocator getApplyButton(){
	  return new ButtonLocator("Apply");
	}
	
	protected ButtonLocator getRestoreButton(){
	  return new ButtonLocator("Restore Defaults");
	}
  protected void verifyRepoSettingsValue(final String value) throws Exception{
    Display.getDefault().syncExec(new Runnable(){
      public void run(){
        try{
          final IWidgetReference ref = (IWidgetReference)getUI().find(new NamedWidgetLocator("localRepositoryText"));
          assertNotNull(ref);
          Text localRepoText = (Text)ref.getWidget();
          String settingsValue = localRepoText.getText();
          assert(settingsValue.endsWith(value));        
        } catch(Exception e){
          assertFalse("Value for settings file was wrong", true);
        }
      }
    });
  }	
  protected void verifyGlobalSettingsValue(final String value) throws Exception{
	  Display.getDefault().syncExec(new Runnable(){
      public void run(){
        try{
          final IWidgetReference ref = (IWidgetReference)getUI().find(new NamedWidgetLocator("globalSettingsText"));
          assertNotNull(ref);
          Text globalSettingsText = (Text)ref.getWidget();
          String settingsValue = globalSettingsText.getText();
          assert(settingsValue.equals(value));        
        } catch(Exception e){
          assertFalse("Value for settings file was wrong", true);
        }
      }
    });
	}
	public void testUserSettingsPrefs() throws Exception {
	  
	  
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

  private void closeInstallationPrefs() throws Exception {
    getUI().click(new ButtonLocator("OK"));
    getUI().wait(new ShellDisposedCondition("Preferences"));
  }
  private void closeUserSettingPrefs() throws Exception {
    getUI().click(new ButtonLocator("OK"));
    getUI().wait(new ShellDisposedCondition("Preferences"));
  }
  /**
   * @throws WidgetSearchException 
   * 
   */
  private void showInstallationPrefs() throws WidgetSearchException {
    getUI().click(new MenuItemLocator("Window/Preferences"));
    getUI().wait(new ShellShowingCondition("Preferences"));
    getUI().click(new FilteredTreeItemLocator("Maven/Installations"));
  }
  private void showUserSettingsPrefs() throws WidgetSearchException {
    getUI().click(new MenuItemLocator("Window/Preferences"));
    getUI().wait(new ShellShowingCondition("Preferences"));
    getUI().click(new FilteredTreeItemLocator("Maven/User Settings"));
  }

}
