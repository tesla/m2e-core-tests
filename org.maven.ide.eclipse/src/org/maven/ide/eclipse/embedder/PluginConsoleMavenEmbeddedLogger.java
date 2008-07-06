/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.embedder;

import org.apache.maven.embedder.MavenEmbedderLogger;

import org.maven.ide.eclipse.core.MavenConsole;


class PluginConsoleMavenEmbeddedLogger implements MavenEmbedderLogger {
  private MavenConsole console;
  private int treshold;
  
  public PluginConsoleMavenEmbeddedLogger(MavenConsole console, boolean debug) {
    this.console = console;
    this.treshold = debug ? LEVEL_DEBUG : LEVEL_INFO;
  }

  private void out(String s) {
    console.logMessage(s);
  }

  private void outError(String s) {
    console.logError(s);
  }
  
  public void debug( String msg ) {
    if (isDebugEnabled()) {
      out("[DEBUG] "+msg);
    }
  }

  public void debug( String msg, Throwable t) {
    if (isDebugEnabled()) {
      out( "[DEBUG] "+msg+" "+t.getMessage());
    }
  }

  public void info( String msg ) {
    if (isInfoEnabled()) {
      out( "[INFO] "+msg);
    }
  }

  public void info( String msg, Throwable t ) {
    if (isInfoEnabled()) {
      out( "[INFO] "+msg+" "+t.getMessage());
    }
  }

  public void warn( String msg ) {
    if (isWarnEnabled()) {
      out("[WARN] "+msg);
    }
  }
  
  public void warn( String msg, Throwable t ) {
    if (isWarnEnabled()) {
      out( "[WARN] "+msg+" "+t.getMessage());
    }
  }
  
  public void fatalError( String msg ) {
    if (isFatalErrorEnabled()) {
      outError( "[FATAL ERROR] "+msg);
    }
  }
  
  public void fatalError( String msg, Throwable t ) {
    if (isFatalErrorEnabled()) {
      outError( "[FATAL ERROR] "+msg+" "+t.getMessage());
    }
  }
  
  public void error( String msg ) {
    if (isErrorEnabled()) {
      outError( "[ERROR] "+msg);
    }
  }
  
  public void error( String msg, Throwable t ) {
    if (isErrorEnabled()) {
      outError( "[ERROR] "+msg+" "+t.getMessage());
    }
  }
  
  public boolean isDebugEnabled() {
    return this.treshold <= LEVEL_DEBUG;
  }
  
  public boolean isInfoEnabled() {
    return this.treshold <= LEVEL_INFO;
  }

  public boolean isWarnEnabled() {
    return this.treshold <= LEVEL_WARN;
  }

  public boolean isErrorEnabled() {
    return this.treshold <= LEVEL_ERROR;
  }

  public boolean isFatalErrorEnabled() {
    return this.treshold <= LEVEL_FATAL;
  }

  public void setThreshold( int treshold ) {
    this.treshold = treshold;
  }

  public int getThreshold() {
    return treshold;
  }

  public void close() {
  }

}

