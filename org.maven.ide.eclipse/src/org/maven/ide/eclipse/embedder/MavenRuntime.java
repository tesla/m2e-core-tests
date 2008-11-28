/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.embedder;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.maven.ide.eclipse.internal.embedder.MavenExternalRuntime;

/**
 * Maven runtime
 *
 * @author Eugene Kuleshov
 */
public abstract class MavenRuntime {
  
  public abstract boolean isEditable();
  
  public abstract String getMainTypeName();

  public abstract String getOptions(File tpmfolder, String[] forcedComponents) throws CoreException;

  public abstract String[] getClasspath(String[] forcedComponents) throws CoreException;

  public abstract void getSourcePath(IClasspathCollector collector, IProgressMonitor monitor) throws CoreException;
  
  public abstract String getLocation();

  public abstract String getSettings();
  
  public abstract boolean isAvailable();
  
  public static MavenRuntime createExternalRuntime(String location) {
    return new MavenExternalRuntime(location);
  }

}
