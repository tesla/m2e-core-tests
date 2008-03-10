/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.console;

import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleFactory;
import org.eclipse.ui.console.IConsoleManager;

import org.maven.ide.eclipse.MavenConsole;
import org.maven.ide.eclipse.MavenPlugin;

/**
 * Console factory is used to show the console from the Console view "Open Console"
 * drop-down action. This factory is registered via the org.eclipse.ui.console.consoleFactory 
 * extension point.
 * 
 *  @author Dmitri Maximovich
 */
public class MavenConsoleFactory implements IConsoleFactory {

  public void openConsole() {
    showConsole();
  }

  public static void showConsole() {
    MavenConsole console = MavenPlugin.getDefault().getConsole();
    if (console != null) {
      IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
      IConsole[] existing = manager.getConsoles();
      boolean exists = false;
      for (int i = 0; i < existing.length; i++) {
        if(console == existing[i])
          exists = true;
      }
      if(!exists) {
        manager.addConsoles(new IConsole[] {console});
      }
      manager.showConsoleView(console);
    }
  }
  
  public static void closeConsole() {
    IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
    MavenConsole console = MavenPlugin.getDefault().getConsole();
    if (console != null) {
      manager.removeConsoles(new IConsole[] {console});
      ConsolePlugin.getDefault().getConsoleManager().addConsoleListener(console.newLifecycle());
    }
  }
  
}
