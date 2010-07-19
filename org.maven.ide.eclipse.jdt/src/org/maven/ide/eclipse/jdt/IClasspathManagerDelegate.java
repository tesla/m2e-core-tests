/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.jdt;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.maven.ide.eclipse.project.IMavenProjectFacade;

/**
 * IBuildPathManagerDelegate
 * 
 * @author igor
 */
public interface IClasspathManagerDelegate {

  public void populateClasspath(IClasspathDescriptor classpath, IMavenProjectFacade projectFacade, int kind,
      IProgressMonitor monitor) throws CoreException;

}
