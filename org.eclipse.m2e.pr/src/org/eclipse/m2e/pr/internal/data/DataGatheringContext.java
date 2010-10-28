/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.pr.internal.data;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.m2e.pr.IDataGatheringContext;
import org.eclipse.m2e.pr.IDataTarget;


/**
 * Provides contextual information to data gatherers.
 */
class DataGatheringContext implements IDataGatheringContext {

  private IDataTarget target;

  private IProgressMonitor monitor;

  public DataGatheringContext(IDataTarget target, IProgressMonitor monitor) {
    this.target = target;
    this.monitor = (monitor != null) ? monitor : new NullProgressMonitor();
  }

  public IProgressMonitor getMonitor() {
    return monitor;
  }

  public IDataTarget getTarget() {
    return target;
  }

}
