/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.tests.scm;

import junit.framework.TestCase;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.m2e.core.scm.ScmHandler;
import org.eclipse.m2e.core.scm.ScmHandlerFactory;

/**
 * @author Eugene Kuleshov
 */
public class ScmHandlerFactoryTest extends TestCase {

  public void testScmHandlerFactory() throws CoreException {
    ScmHandler handler = ScmHandlerFactory.getHandler("scm:test:foo");
    assertEquals(TestScmHandler.class.getName(), handler.getClass().getName());
    assertEquals("test", handler.getType());
    assertEquals(1000, handler.getPriority());
  }

}
