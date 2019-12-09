/*******************************************************************************
 * Copyright (c) 2015 Anton Tanasenko
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Anton Tanasenko. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.tests.mocks;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.VMRunnerConfiguration;


public class MockVMRunner implements IVMRunner {
  private VMRunnerConfiguration configuration;

  public void run(VMRunnerConfiguration configuration, ILaunch launch, IProgressMonitor monitor) {
    this.configuration = configuration;
  }

  public VMRunnerConfiguration getConfiguration() {
    return this.configuration;
  }
}
