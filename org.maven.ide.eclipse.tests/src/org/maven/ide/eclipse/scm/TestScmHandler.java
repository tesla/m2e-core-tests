/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.scm;

import java.io.File;
import java.io.InputStream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.maven.ide.eclipse.project.MavenProjectScmInfo;


/**
 * @author Eugene Kuleshov
 */
public class TestScmHandler extends ScmHandler {

  public InputStream open(String url, String revision) throws CoreException {
    return null;
  }

  public void checkoutProject(MavenProjectScmInfo info, File dest, IProgressMonitor monitor)
      throws CoreException, InterruptedException {
  }

}
