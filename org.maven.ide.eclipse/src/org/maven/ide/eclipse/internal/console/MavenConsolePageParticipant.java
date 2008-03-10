/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.console;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsolePageParticipant;
import org.eclipse.ui.part.IPageBookViewPage;


public class MavenConsolePageParticipant implements IConsolePageParticipant {

  private IAction consoleRemoveAction;
  private IAction debugAction;

  public void init(IPageBookViewPage page, IConsole console) {
    this.consoleRemoveAction = new MavenConsoleRemoveAction();
    this.debugAction = new MavenDebugOutputAction();

    IToolBarManager toolBarManager = page.getSite().getActionBars().getToolBarManager();
    toolBarManager.appendToGroup(IConsoleConstants.LAUNCH_GROUP, consoleRemoveAction);
    toolBarManager.prependToGroup(IConsoleConstants.OUTPUT_GROUP, debugAction);
  }

  public void dispose() {
    this.consoleRemoveAction = null;
    this.debugAction = null;
  }

  public void activated() {
  }

  public void deactivated() {
  }

  public Object getAdapter(Class adapter) {
    return null;
  }

}
