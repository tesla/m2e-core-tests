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

package org.eclipse.m2e.tests.scm;

import java.io.File;
import java.io.InputStream;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.m2e.scm.MavenProjectScmInfo;
import org.eclipse.m2e.scm.spi.ScmHandler;


/**
 * @author Eugene Kuleshov
 */
public class TestScmHandler extends ScmHandler {

  @Override
  public InputStream open(String url, String revision) {
    return null;
  }

  @Override
  public void checkoutProject(MavenProjectScmInfo info, File dest, IProgressMonitor monitor) {
  }

}
