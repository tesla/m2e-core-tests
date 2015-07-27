/*******************************************************************************
 * Copyright (c) 2014 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Takari, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.tests.builder;

import java.io.File;
import java.util.Collections;
import java.util.Set;

import org.eclipse.m2e.core.internal.builder.IIncrementalBuildFramework.BuildResultCollector;


class DummyBuildResultCollector implements BuildResultCollector {

  @Override
  public void refresh(File file) {
  }

  @Override
  public void addMessage(File file, int line, int column, String message, int severity, Throwable cause) {
  }

  @Override
  public void removeMessages(File file) {
  }

  @Override
  public Set<File> getFiles() {
    return Collections.emptySet();
  }

}
