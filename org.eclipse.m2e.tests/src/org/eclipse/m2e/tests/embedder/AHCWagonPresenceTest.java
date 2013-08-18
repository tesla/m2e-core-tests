/*******************************************************************************
 * Copyright (c) 2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.tests.embedder;

import junit.framework.TestCase;

import org.apache.maven.wagon.Wagon;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.embedder.MavenImpl;


public class AHCWagonPresenceTest extends TestCase {
  public void testAHCisHere() throws ComponentLookupException, CoreException {
    assertEquals("org.sonatype.maven.wagon.AhcWagon",
        ((MavenImpl) MavenPlugin.getMaven()).getPlexusContainer().lookup(Wagon.class, "http").getClass().getName());
  }
}
