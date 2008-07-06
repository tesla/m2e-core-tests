/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.core;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;


/**
 * Maven Logger
 *
 * @author Eugene Kuleshov
 */
public class MavenLogger {

  private static ILog LOG;

  public static void setLog(ILog log) {
    LOG = log;
  }
  
  public static void log(IStatus status) {
    LOG.log(status);
  }

  public static void log(CoreException ex) {
    IStatus s = ex.getStatus();
    if(s.getException()==null) {
      log(new Status(s.getSeverity(), s.getPlugin(), s.getCode(), s.getMessage(), ex));
    } else {
      log(s);
    }
  }

  public static void log(String msg, Throwable t) {
    log(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, 0, msg, t));
  }

}
