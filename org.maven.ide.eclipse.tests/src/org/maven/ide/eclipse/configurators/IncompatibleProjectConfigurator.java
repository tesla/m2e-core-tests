/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.configurators;

import org.eclipse.core.runtime.IProgressMonitor;
import org.maven.ide.eclipse.project.configurator.AbstractProjectConfigurator;
import org.maven.ide.eclipse.project.configurator.ProjectConfigurationRequest;


public class IncompatibleProjectConfigurator extends AbstractProjectConfigurator {

  public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) {
    throw new AbstractMethodError("MNGECLIPSE-1599");
  }

}
