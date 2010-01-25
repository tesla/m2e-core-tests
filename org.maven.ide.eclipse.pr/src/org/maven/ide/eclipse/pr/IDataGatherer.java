/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.pr;

import org.eclipse.core.runtime.CoreException;


/**
 * Gathers data to be included in an issue report.
 */
public interface IDataGatherer {

  /**
   * Gets tne name of this data gatherer.
   * 
   * @return The name of this gatherer, never {@code null}.
   */
  String getName();

  /**
   * Gathers data to be included in an issue report.
   * 
   * @param context The data gathering context for the issue report, must not be {@code null}.
   * @throws CoreException If the data gathering failed for any reason.
   */
  void gather(IDataGatheringContext context) throws CoreException;

}
