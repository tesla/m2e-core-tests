/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.editor.xml;

/**
 * Abstract log to log errors to. I do not want to be dependant on Eclipse here.
 * 
 * @author Lukas Krecan
 */
public interface Log {
  public static final Log NULL_LOG = new Log() {
    public void logError(String message, Throwable exception) {

    }

    public void logWarn(String message, Throwable exception) {

    }
  };

  public void logError(String message, Throwable exception);

  public void logWarn(String message, Throwable exception);
}
