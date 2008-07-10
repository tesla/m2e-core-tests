/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.editor.pom;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;

import com.windowtester.runtime.IUIContext;
import com.windowtester.runtime.WT;
import com.windowtester.runtime.WaitTimedOutException;
import com.windowtester.runtime.WidgetSearchException;
import com.windowtester.runtime.condition.HasTextCondition;
import com.windowtester.runtime.swt.UITestCaseSWT;
import com.windowtester.runtime.swt.condition.shell.ShellDisposedCondition;
import com.windowtester.runtime.swt.condition.shell.ShellShowingCondition;
import com.windowtester.runtime.swt.locator.ButtonLocator;
import com.windowtester.runtime.swt.locator.CTabItemLocator;
import com.windowtester.runtime.swt.locator.MenuItemLocator;
import com.windowtester.runtime.swt.locator.NamedWidgetLocator;
import com.windowtester.runtime.swt.locator.SWTWidgetLocator;
import com.windowtester.runtime.swt.locator.TableItemLocator;
import com.windowtester.runtime.swt.locator.TreeItemLocator;
import com.windowtester.runtime.swt.locator.eclipse.ViewLocator;

/**
 * @author Eugene Kuleshov
 * @author Anton Kraev
 */
public class PomEditorTest extends UITestCaseSWT {

	private static final String POM_XML_TAB = "pom.xml";
	private static final String OVERVIEW_TAB = "Overview";
	private IUIContext ui;

	protected void setUp() throws Exception {
		ui = getUI();
	}

	protected void oneTimeSetup() throws Exception {
		super.oneTimeSetup();

		ui = getUI();
		ui.close(new CTabItemLocator("Welcome"));

		ui.click(new MenuItemLocator("Window/Open Perspective/Other..."));
		ui.wait(new ShellShowingCondition("Open Perspective"));
		ui.click(2, new TableItemLocator("Java( \\(default\\))?"));
		ui.wait(new ShellDisposedCondition("Open Perspective"));

		//create new project with POM
		ui.contextClick(new SWTWidgetLocator(Tree.class, new ViewLocator(
				"org.eclipse.jdt.ui.PackageExplorer")), "Ne&w/Maven Project");
		ui.wait(new ShellShowingCondition("New Maven Project"));
		ui.click(new ButtonLocator(
				"Create a &simple project (skip archetype selection)"));
		ui.click(new ButtonLocator("&Next >"));
		ui.enterText("org.foo");
		ui.keyClick(WT.TAB);
		ui.enterText("test-pom");
		ui.click(new ButtonLocator("&Finish"));
		ui.wait(new ShellDisposedCondition("New Maven Project"));
		ui.click(2, new TreeItemLocator("test-pom/pom.xml", new ViewLocator(
				"org.eclipse.jdt.ui.PackageExplorer")));
		ui.keyClick(SWT.CTRL, 'm');
	}

	public void test1() throws Exception {
		//test XML->FORM update of artifactId
		ui.click(new CTabItemLocator(POM_XML_TAB));
		replaceText("test-pom", "test-pom1");
		ui.click(new CTabItemLocator(OVERVIEW_TAB));
		testTextValue("artifactId", "test-pom1");
	}

	public void test2() throws Exception {
		//test FORM->XML and XML->FORM update of parentArtifactId
		ui.click(new SWTWidgetLocator(Label.class, "Parent"));
		setTextValue("parentArtifactId", "parent2");
		ui.click(new CTabItemLocator(POM_XML_TAB));
		replaceText("parent2", "parent3");
		ui.click(new CTabItemLocator(OVERVIEW_TAB));
		testTextValue("parentArtifactId", "parent3");
	}

	private void testTextValue(String id, String value)
			throws WidgetSearchException {
		NamedWidgetLocator locator = new NamedWidgetLocator(id);
		ui.assertThat(new HasTextCondition(locator, value));
	}

	private void setTextValue(String id, String value)
			throws WidgetSearchException {
		NamedWidgetLocator locator = new NamedWidgetLocator(id);
		ui.setFocus(locator);
		ui.keyClick(SWT.CTRL, 'a');
		ui.enterText(value);
	}

	private void replaceText(String src, String target)
			throws WaitTimedOutException, WidgetSearchException {
		ui.keyClick(SWT.CTRL, 'f');
		ui.wait(new ShellShowingCondition("Find/Replace"));

		ui.enterText(src);
		ui.keyClick(WT.TAB);
		ui.enterText(target);
		ui.keyClick(SWT.ALT, 'a'); // "replace all"
		ui.close(new SWTWidgetLocator(Shell.class, "Find/Replace"));
		ui.wait(new ShellDisposedCondition("Find/Replace"));
	}

}
