/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.embedder;

import org.apache.maven.monitor.event.EventMonitor;

import org.maven.ide.eclipse.MavenConsole;


public class PluginConsoleEventMonitor implements EventMonitor {

  private MavenConsole console;

  public PluginConsoleEventMonitor(MavenConsole console) {
    this.console = console;
  }

  public void startEvent( String eventName, String target, long timestamp ) {
    if( "mojo-execute".equals( eventName ) ) {
      console.logMessage( target );
    }
  }

  public void endEvent( String eventName, String target, long timestamp ) {
    if( "project-execute".equals( eventName ) ) {
      console.logMessage( "BUILD SUCCESSFUL" );
    }
  }

  public void errorEvent( String eventName, String target, long timestamp, Throwable cause ) {
    console.logMessage("ERROR " + eventName + " : " + target + (cause == null ? "" : " : " + cause.getMessage()));
  }

}

