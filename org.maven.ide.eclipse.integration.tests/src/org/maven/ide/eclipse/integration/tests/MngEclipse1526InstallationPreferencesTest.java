package org.maven.ide.eclipse.integration.tests;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.internal.resources.XMLWriter;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.editor.pom.MavenPomEditor;
import org.maven.ide.eclipse.embedder.MavenRuntime;
import org.maven.ide.eclipse.embedder.MavenRuntimeManager;
import org.maven.ide.eclipse.project.IMavenProjectFacade;

import com.windowtester.runtime.WT;
import com.windowtester.runtime.WidgetSearchException;
import com.windowtester.runtime.locator.IWidgetLocator;
import com.windowtester.runtime.locator.IWidgetReference;
import com.windowtester.runtime.swt.condition.SWTIdleCondition;
import com.windowtester.runtime.swt.condition.eclipse.JobsCompleteCondition;
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
    File tempDir = importMavenProjects("projects/someproject.zip");
    IMavenProjectFacade mavenProject = MavenPlugin.getDefault().getMavenProjectManager().getMavenProject("org.sonatype.test", "someproject", "0.0.1-SNAPSHOT");
    assertNotNull(mavenProject);
    
	  ButtonLocator addButton = new ButtonLocator("Add...");
	  ButtonLocator removeButton = new ButtonLocator("Remove");
	  //add external maven runtime
	  
    MavenRuntime newRuntime = MavenRuntimeManager.createExternalRuntime(externalRuntime);
    List<MavenRuntime> currRuntimes = MavenPlugin.getDefault().getMavenRuntimeManager().getMavenRuntimes();
    ArrayList<MavenRuntime> list = new ArrayList<MavenRuntime>(currRuntimes);
    list.add(newRuntime);
    MavenPlugin.getDefault().getMavenRuntimeManager().setRuntimes(list);
    MavenPlugin.getDefault().getMavenRuntimeManager().setDefaultRuntime(newRuntime);
    
    //create the global_settings.xml file to use
    FileOutputStream out = null;
    File repoDir = createTempDir("testing_repo");
    File globalSettingsFile = File.createTempFile("global_settings", ".xml");
    try{
      out = new FileOutputStream(globalSettingsFile);
      XMLWriter writer = new XMLWriter(out);
      writer.println("<settings>");
      writer.println("<localRepository>"+repoDir.getAbsolutePath()+"</localRepository>");
      writer.println("</settings>");
      writer.flush();
    } finally {
     out.close(); 
    }
 
    IProject project = mavenProject.getProject();
    IPath externalLocation = new Path(globalSettingsFile.getAbsolutePath());
    IFile linkedFile = project.getFile(externalLocation.lastSegment());
    linkedFile.createLink(externalLocation, IResource.NONE, null);
    
    openFile(project, externalLocation.lastSegment());
    getUI().click(new CTabItemLocator("Source"));
    Thread.sleep(3000);
    
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

    getUI().enterText(globalSettingsFile.getAbsolutePath());
    closeInstallationPrefs();
    
    //now open the index view and make sure 'local' has the right value, 
    showView("org.maven.ide.eclipse.views.MavenIndexesView");
    getUI().click(new CTabItemLocator("Maven Indexes"));
    IWidgetLocator localRepo = getUI().find(new TreeItemLocator("local : "+repoDir.getAbsolutePath()));
    assertNotNull(localRepo);
    
    
    
    showUserSettingsPrefs();
    verifyRepoSettingsValue(repoDir.getAbsolutePath());
    
    String newRepoDir = setUserSettingsXML(mavenProject.getProject());
    getUI().click(new ButtonLocator("Apply"));
    getUI().wait(new JobsCompleteCondition(), 60000);
    Thread.sleep(3000);
    verifyRepoSettingsValue(newRepoDir);
    closeUserSettingPrefs();
    
    //now open the index view and make sure 'local' has the right new value, 
    showView("org.maven.ide.eclipse.views.MavenIndexesView");
    getUI().click(new CTabItemLocator("Maven Indexes"));
    IWidgetLocator newLocalRepo = getUI().find(new TreeItemLocator("local : "+newRepoDir));
    assertNotNull(newLocalRepo);
	}

	protected String setUserSettingsXML(IProject project)throws Exception{
    //create the global_settings.xml file to use
    FileOutputStream userOut = null;
    File userRepoDir = createTempDir("user_testing_repo");
    File userSettings = File.createTempFile("user_settings", ".xml");
    try{
      userOut = new FileOutputStream(userSettings);
      XMLWriter writer = new XMLWriter(userOut);
      writer.println("<settings>");
      writer.println("<localRepository>"+userRepoDir.getAbsolutePath()+"</localRepository>");
      writer.println("</settings>");
      writer.flush();
    } finally {
     userOut.close(); 
    }
    IPath userExternalLocation = new Path(userSettings.getAbsolutePath());
    IFile linkedUserFile = project.getFile(userExternalLocation.lastSegment());
    linkedUserFile.createLink(userExternalLocation, IResource.NONE, null);
    updateUserSettingsValue(userSettings.getAbsolutePath());  
    return userRepoDir.getAbsolutePath();
	}
	
	protected ButtonLocator getApplyButton(){
	  return new ButtonLocator("Apply");
	}
	
	protected ButtonLocator getRestoreButton(){
	  return new ButtonLocator("Restore Defaults");
	}
	protected void updateUserSettingsValue(final String value) throws Exception{
	  getUI().click(new NamedWidgetLocator("userSettingsText"));
    Display.getDefault().syncExec(new Runnable(){
      public void run(){
        try{
          final IWidgetReference ref = (IWidgetReference)getUI().find(new NamedWidgetLocator("userSettingsText"));
          assertNotNull(ref);
          Text userSettingsText = (Text)ref.getWidget();
          userSettingsText.selectAll();
         
          getUI().keyClick(WT.DEL);
          getUI().click(new NamedWidgetLocator("userSettingsText"));
          getUI().enterText(value);
           
        } catch(Exception e){
          assertFalse("Value for settings file was wrong", true);
        }
      }
    });
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
