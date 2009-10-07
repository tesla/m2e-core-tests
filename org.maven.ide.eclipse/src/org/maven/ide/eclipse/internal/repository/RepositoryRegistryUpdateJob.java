/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.repository;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import org.maven.ide.eclipse.jobs.IBackgroundProcessingQueue;

/**
 * RepositoryRegistryUpdateJob
 *
 * @author igor
 */
public class RepositoryRegistryUpdateJob extends Job implements IBackgroundProcessingQueue {
  
  private final RepositoryRegistry registry;

  public RepositoryRegistryUpdateJob(RepositoryRegistry registry) {
    super("Repository registry initialization");
    this.registry = registry;
  }

  public IStatus run(IProgressMonitor monitor) {
    try {
      registry.updateRegistry(monitor);
    } catch(CoreException ex) {
      return ex.getStatus();
    }
    return Status.OK_STATUS;
  }

  public boolean isEmpty() {
    return getState() == Job.NONE;
  }  
}
