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

package org.eclipse.m2e.tests;

import org.eclipse.m2e.core.internal.project.registry.Capability;


public class TestCapability extends Capability {

  private static final long serialVersionUID = -191783795747012210L;

  public TestCapability(String namespace, String id, String version) {
    super(namespace, id);
  }

}
