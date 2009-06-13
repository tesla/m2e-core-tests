/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.jdt;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;

import org.maven.ide.eclipse.project.configurator.AbstractProjectConfigurator;


/**
 * AbstractJavaProjectConfigurator
 * 
 * @author igor
 */
public abstract class AbstractJavaProjectConfigurator extends AbstractProjectConfigurator {

  protected void setRawClasspath(IJavaProject javaProject, Map<IPath, IClasspathEntry> cp, IProgressMonitor monitor)
      throws JavaModelException {
    Collection<IClasspathEntry> entries = cp.values();

    javaProject.setRawClasspath(entries.toArray(new IClasspathEntry[entries.size()]), monitor);
  }

  protected Map<IPath, IClasspathEntry> getRawClasspath(IJavaProject javaProject) throws JavaModelException {
    Map<IPath, IClasspathEntry> cp = new LinkedHashMap<IPath, IClasspathEntry>();

    for(IClasspathEntry cpe : javaProject.getRawClasspath()) {
      cp.put(cpe.getPath(), cpe);
    }

    return cp;
  }

}
