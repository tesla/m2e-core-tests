/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.core;

import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleListener;

/**
 * Maven Console
 *
 * @author Eugene Kuleshov
 */
public interface MavenConsole extends IConsole {

  void logMessage(String msg);

  void logError(String msg);

  IConsoleListener newLifecycle();

  void shutdown();

  void showConsole();
  
  void closeConsole();
  
}
