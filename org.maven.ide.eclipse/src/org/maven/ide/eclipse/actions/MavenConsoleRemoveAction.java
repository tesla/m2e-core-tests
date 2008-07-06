/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.actions;

import org.eclipse.jface.action.Action;
import org.maven.ide.eclipse.MavenPlugin;

public class MavenConsoleRemoveAction extends Action {

  public MavenConsoleRemoveAction() {
    setToolTipText("Close Maven2 Console");
    setImageDescriptor(MavenPlugin.getImageDescriptor("icons/close.gif"));
  }
  
  public void run() {
    MavenPlugin.getDefault().getConsole().closeConsole();
  }

}
