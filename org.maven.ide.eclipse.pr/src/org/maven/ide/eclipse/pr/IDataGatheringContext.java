/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.pr;

import org.eclipse.core.runtime.IProgressMonitor;


/**
 * Describes the context of data gathering for an issue report.
 */
public interface IDataGatheringContext {

  /**
   * Gets the target for the gathered data.
   * 
   * @return The target for the gathered data, never {@code null}.
   */
  IDataTarget getTarget();

  /**
   * Gets the progress monitor to use.
   * 
   * @return The progress monitor, never {@code null}.
   */
  IProgressMonitor getMonitor();

}
