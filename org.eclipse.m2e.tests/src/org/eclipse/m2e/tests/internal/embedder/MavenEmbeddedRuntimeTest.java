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

package org.eclipse.m2e.tests.internal.embedder;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;

import org.junit.Test;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.MavenRuntime;
import org.eclipse.m2e.core.embedder.MavenRuntimeManager;
import org.eclipse.m2e.core.internal.Messages;


@SuppressWarnings("restriction")
public class MavenEmbeddedRuntimeTest {
  @Test
  public void testGetVersion() throws Exception {
    MavenRuntimeManager runtimeManager = MavenPlugin.getMavenRuntimeManager();
    MavenRuntime embeddedRuntime = runtimeManager.getRuntime(MavenRuntimeManager.EMBEDDED);
    String mavenVersion = embeddedRuntime.getVersion();
    assertNotNull(mavenVersion);
    assertNotSame("", mavenVersion);
    assertNotSame(Messages.MavenEmbeddedRuntime_unknown, mavenVersion);
  }
}
