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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.m2e.scm.internal.ScmHandlerFactory;
import org.eclipse.m2e.scm.spi.ScmHandler;

public class ScmHandlerFactoryTest {
  @Test
  public void testScmHandlerFactory() throws CoreException {
    ScmHandler handler = ScmHandlerFactory.getHandler("scm:test:foo");
    assertEquals(TestScmHandler.class.getName(), handler.getClass().getName());
    assertEquals("test", handler.getType());
    assertEquals(1000, handler.getPriority());
  }

}
