/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.pr.internal.sources;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;

import org.eclipse.core.runtime.IStatus;
import org.maven.ide.eclipse.pr.IDataSource;


/**
 * Handles an {@link IStatus}.
 */
public class StatusSource implements IDataSource {

  private final String name;

  private final IStatus status;

  public StatusSource(IStatus status, String name) {
    this.status = status;
    this.name = name;
  }

  public InputStream getInputStream() {
    StringWriter sw = new StringWriter(256);

    format(status, new PrintWriter(sw), "");

    try {
      return new ByteArrayInputStream(sw.toString().getBytes("UTF-8"));
    } catch(UnsupportedEncodingException ex) {
      return new ByteArrayInputStream(new byte[0]);
    }
  }

  private void format(IStatus status, PrintWriter writer, String indent) {
    writer.println(indent + "Severity = " + status.getSeverity());
    writer.println(indent + "Plugin = " + status.getPlugin());
    writer.println(indent + "Code = " + status.getCode());
    writer.println(indent + "Message = " + status.getMessage());

    Throwable exception = status.getException();
    if(exception != null) {
      exception.printStackTrace(writer);
    }

    IStatus[] children = status.getChildren();
    if(children != null) {
      for(IStatus child : children) {
        format(child, writer, indent + "  ");
      }
    }
  }

  public String getName() {
    return name;
  }

}
