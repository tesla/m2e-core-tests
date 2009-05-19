/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.refactoring;

import org.maven.ide.eclipse.editor.pom.MavenPomEditor;

import com.windowtester.runtime.swt.condition.shell.ShellDisposedCondition;
import com.windowtester.runtime.swt.condition.shell.ShellShowingCondition;
import com.windowtester.runtime.swt.locator.ButtonLocator;
import com.windowtester.runtime.swt.locator.CTabItemLocator;
import com.windowtester.runtime.swt.locator.TableItemLocator;
import com.windowtester.runtime.swt.locator.TreeItemLocator;
import com.windowtester.runtime.swt.locator.eclipse.ViewLocator;


/**
 * @author Eugene Kuleshov
 * @author Anton Kraev 
 */
public class RefactoringTest extends RefactoringTestBase {

  private static final String _0_0_ZQ_SNAPSHOT = "0.0.zq-SNAPSHOT";
  private static final String _0_0_ZZQ_SNAPSHOT = "0.0.zzq-SNAPSHOT";
  private static final String ARTIFACT_ID = "artifactId";
  private static final String VERSION = "version";
  private static final String REFACTOR_RENAME_MAVEN_ARTIFACT = "Refactor/Rename Maven Artifact...";
  private static final String OK = "OK";
  private static final String SAVE_ALL_MODIFIED_RESOURCES = "Save All Modified Resources";
  private static final String ORG_ECLIPSE_JDT_UI_PACKAGE_EXPLORER = "org.eclipse.jdt.ui.PackageExplorer";
  private static final String RENAME_ARTIFACT = "Rename Maven Artifact";
  private static final String PROGRESS_INFORMATION = "Progress Information";
  private static final String CHILD_POM_XML = "child/pom.xml";
  private static final String MINE_POM_XML = "mine/pom.xml";

  //tests version refactoring
  public void testRefactoringVersion() throws Exception {
    //open 2 editors
    MavenPomEditor mineEditor = openPomFile(MINE_POM_XML);
    MavenPomEditor childEditor = openPomFile(CHILD_POM_XML);
 
    ui.click(new TreeItemLocator(MINE_POM_XML, new ViewLocator(
        ORG_ECLIPSE_JDT_UI_PACKAGE_EXPLORER)));
    
    //click cancel after preview
    ui.contextClick(new TreeItemLocator(MINE_POM_XML, new ViewLocator(
        ORG_ECLIPSE_JDT_UI_PACKAGE_EXPLORER)),
        REFACTOR_RENAME_MAVEN_ARTIFACT);
    ui.wait(new ShellDisposedCondition(PROGRESS_INFORMATION));
    ui.wait(new ShellShowingCondition(RENAME_ARTIFACT));
    setTextValue(VERSION, _0_0_ZQ_SNAPSHOT);
    ui.click(new ButtonLocator("Previe&w >"));
    ui.wait(new ShellDisposedCondition(PROGRESS_INFORMATION));
    ui.wait(new ShellShowingCondition(RENAME_ARTIFACT));
    ui.click(new ButtonLocator("Cancel"));
    ui.wait(new ShellDisposedCondition(RENAME_ARTIFACT));

    //refactor version
    ui.contextClick(new TreeItemLocator(MINE_POM_XML, new ViewLocator(
        ORG_ECLIPSE_JDT_UI_PACKAGE_EXPLORER)),
        REFACTOR_RENAME_MAVEN_ARTIFACT);
    ui.wait(new ShellDisposedCondition(PROGRESS_INFORMATION));
    ui.wait(new ShellShowingCondition(RENAME_ARTIFACT));
    setTextValue(VERSION, _0_0_ZQ_SNAPSHOT);
    ui.click(new ButtonLocator("Previe&w >"));
    ui.wait(new ShellDisposedCondition(PROGRESS_INFORMATION));
    ui.wait(new ShellShowingCondition(RENAME_ARTIFACT));
    ui.click(new ButtonLocator(OK));
    ui.wait(new ShellDisposedCondition(RENAME_ARTIFACT));
    ui.click(new CTabItemLocator(MINE_POM_XML));
    assertTextValue(VERSION, _0_0_ZQ_SNAPSHOT);

    //check editor is not dirty
    waitForEditorDirtyState(mineEditor, false);
    
    //make it dirty
    setTextValue(VERSION, "");
    setTextValue(VERSION, _0_0_ZQ_SNAPSHOT);
    waitForEditorDirtyState(mineEditor, true);
 //   ui.assertThat(new DirtyEditorCondition());
    
    //see if the save dialog is displayed
    ui.contextClick(new TreeItemLocator(MINE_POM_XML, new ViewLocator(
        ORG_ECLIPSE_JDT_UI_PACKAGE_EXPLORER)),
        REFACTOR_RENAME_MAVEN_ARTIFACT);
    ui.wait(new ShellShowingCondition(SAVE_ALL_MODIFIED_RESOURCES));
    ui.click(new ButtonLocator(OK));
    ui.wait(new ShellDisposedCondition(SAVE_ALL_MODIFIED_RESOURCES));

    //rename again
    ui.wait(new ShellShowingCondition(RENAME_ARTIFACT));
    setTextValue(VERSION, _0_0_ZZQ_SNAPSHOT);
    ui.click(new ButtonLocator(OK));
    
    //check to see the child pom has proper parent
    ui.click(new CTabItemLocator(CHILD_POM_XML));
    waitForEditorDirtyState(childEditor, false);
    assertTextValue("parentVersion", _0_0_ZZQ_SNAPSHOT);
  }

  //tests artifactId refactoring (tests version wildcard)
  public void testRefactoringArtifactId() throws Exception {

    MavenPomEditor mineEditor = openPomFile(MINE_POM_XML);
    MavenPomEditor childEditor = openPomFile(CHILD_POM_XML);
    
    //refactor artifactId
    ui.contextClick(new TreeItemLocator(MINE_POM_XML, new ViewLocator(
        ORG_ECLIPSE_JDT_UI_PACKAGE_EXPLORER)),
        REFACTOR_RENAME_MAVEN_ARTIFACT);
    ui.wait(new ShellDisposedCondition(PROGRESS_INFORMATION));
    ui.wait(new ShellShowingCondition(RENAME_ARTIFACT));
    setTextValue(ARTIFACT_ID, "parent");
    ui.click(new ButtonLocator("Previe&w >"));
    ui.wait(new ShellDisposedCondition(PROGRESS_INFORMATION));
    ui.wait(new ShellDisposedCondition(PROGRESS_INFORMATION));
    ui.click(new ButtonLocator(OK));
    ui.wait(new ShellDisposedCondition(RENAME_ARTIFACT));
    assertTextValue("parentArtifactId", "parent");

    //check editor is not dirty
    waitForEditorDirtyState(mineEditor, false);
    
    //check to see the child pom has proper parent
    ui.click(new CTabItemLocator(CHILD_POM_XML));
    waitForEditorDirtyState(childEditor, false);
    assertTextValue("parentArtifactId", "parent");
    
    //test that dependencies were refactored (including wildcard)
    ui.click(new CTabItemLocator("Dependencies"));
    ui.click(new TableItemLocator("mine : parent"));
    ui.click(new TableItemLocator("mine : parent : 0.0.zzq-SNAPSHOT"));
  }
  
  private void waitForEditorDirtyState(MavenPomEditor editor, boolean dirtyState) throws InterruptedException {
    int time = 0;
     while (time < 30000) {
       if (dirtyState == editor.isDirty()) {
         return;
       }
       Thread.sleep(5000);
       time += 5000;
     }
     fail("Timed out waiting for editor dirty state: "  + dirtyState);
 }
 
}
