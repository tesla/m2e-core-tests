/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.util;

public class Tracer {
  
  private Tracer() {
  }

  public static void trace(ITraceable target, String message, Object param) {
    if(target.isTraceEnabled()) {
      System.out.println(target.getClass().getName() + ": " + message + (param != null ? ": [" + param + "]" : ""));
    }
  }

  public static void trace(ITraceable target, String message, Object param, Throwable e) {
    trace(target, message, param);
    if(target.isTraceEnabled() && e != null) {
      e.printStackTrace(System.out);
    }
  }

  public static void trace(ITraceable target, String message) {
    trace(target, message, null);
  }

}
