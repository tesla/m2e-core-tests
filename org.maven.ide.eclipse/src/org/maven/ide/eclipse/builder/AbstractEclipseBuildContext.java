/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.builder;

import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.sonatype.plexus.build.incremental.BuildContext2;


/**
 * AbstractEclipseBuildContext
 * 
 * @author igor
 */
public abstract class AbstractEclipseBuildContext implements BuildContext2 {

  public static class Message {
    public final File file;

    public final int line;

    public final int column;

    public final String message;

    public final Throwable cause;

    Message(File file, int line, int column, String message, Throwable cause) {
      this.file = file;
      this.line = line;
      this.column = column;
      this.message = message;
      this.cause = cause;
    }
  }

  private final Set<File> refresh = new HashSet<File>();

  protected final Map<String, Object> context;

  private final ArrayList<Message> errorMessages = new ArrayList<Message>();

  private final ArrayList<Message> warningMessages = new ArrayList<Message>();

  protected AbstractEclipseBuildContext(Map<String, Object> context) {
    this.context = context;
  }

  public void refresh(File file) {
    refresh.add(file);
  }

  public Set<File> getFiles() {
    return refresh;
  }

  public OutputStream newFileOutputStream(File file) {
    return new ChangedFileOutputStream(file, this);
  }

  /**
   * Returns path relative to delta resource location.
   */
  protected IPath getRelativePath(File file) {
    IPath basepath = getBaseResource().getLocation();
    IPath path = Path.fromOSString(file.getAbsolutePath());

    if(!basepath.isPrefixOf(path)) {
      return null;
    }

    return path.removeFirstSegments(basepath.segmentCount());
  }

  protected abstract IResource getBaseResource();

  public void setValue(String key, Object value) {
    context.put(key, value);
  }

  public Object getValue(String key) {
    return context.get(key);
  }

  public void addError(File file, int line, int column, String message, Throwable cause) {
    errorMessages.add(new Message(file, line, column, message, cause));
  }

  public void addWarning(File file, int line, int column, String message, Throwable cause) {
    warningMessages.add(new Message(file, line, column, message, cause));
  }

  public List<Message> getErrorMessages() {
    return errorMessages;
  }

  public List<Message> getWarningMessages() {
    return warningMessages;
  }
}
