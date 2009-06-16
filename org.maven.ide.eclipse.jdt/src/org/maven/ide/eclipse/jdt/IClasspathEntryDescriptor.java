/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.jdt;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;


/**
 * IClasspathEntryDescriptor
 *
 * @author igor
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IClasspathEntryDescriptor {

  public IClasspathEntry getClasspathEntry();

  public String getScope();

  /**
   * @return true if this entry corresponds to an optional maven dependency, false otherwise
   */
  public boolean isOptionalDependency();

  public void addClasspathAttribute(IClasspathAttribute attribute);

  public String getGroupId();

  public void setClasspathEntry(IClasspathEntry entry);

  public String getArtifactId();

  public IPath getPath();

}
