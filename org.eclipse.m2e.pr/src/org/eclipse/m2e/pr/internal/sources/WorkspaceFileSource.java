/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.pr.internal.sources;

import java.io.InputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.m2e.pr.IDataSource;


/**
 * Returns the contents of a file in the workspace.
 */
public class WorkspaceFileSource implements IDataSource {

  private final IFile file;

  public WorkspaceFileSource(IFile file) {
    this.file = file;
  }

  public InputStream getInputStream() throws CoreException {
    if(file != null && file.isAccessible()) {
      return file.getContents();
    } else {
      return null;
    }
  }

  public String getName() {
    return file.getName();
  }

}
