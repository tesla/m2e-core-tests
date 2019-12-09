/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.pr.internal.data;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.m2e.pr.IDataGatherer;
import org.eclipse.m2e.pr.IDataGatheringContext;


public class FailingDataGatherer implements IDataGatherer {

  public void gather(IDataGatheringContext context) throws CoreException {
    throw new NullPointerException();
  }

  public String getName() {
    return "NPE";
  }

}
