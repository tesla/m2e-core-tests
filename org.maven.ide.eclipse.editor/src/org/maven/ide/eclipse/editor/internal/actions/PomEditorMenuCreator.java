/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.editor.internal.actions;

import org.eclipse.jface.action.IMenuManager;
import org.maven.ide.eclipse.actions.AbstractMavenMenuCreator;
import org.maven.ide.eclipse.actions.SelectionUtil;
import org.maven.ide.eclipse.editor.MavenEditorImages;


/**
 * @author Eugene Kuleshov
 */
public class PomEditorMenuCreator extends AbstractMavenMenuCreator {

  public void createMenu(IMenuManager mgr) {
    int selectionType = SelectionUtil.getSelectionType(selection);
    if(selectionType == SelectionUtil.JAR_FILE) {
      mgr.appendToGroup(OPEN, getAction(new ShowDependencyHierarchyAction(), //
          ShowDependencyHierarchyAction.ID,
          "Show Dependency Hierarchy", MavenEditorImages.HIERARCHY));
    }
  }

}
