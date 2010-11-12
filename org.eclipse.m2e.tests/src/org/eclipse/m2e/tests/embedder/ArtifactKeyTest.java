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

package org.eclipse.m2e.tests.embedder;

import junit.framework.TestCase;

import org.eclipse.m2e.core.embedder.ArtifactKey;

public class ArtifactKeyTest extends TestCase {

  public void testPortableString() {
    ArtifactKey k1 = new ArtifactKey("g", "a", "v", "c");
    assertEquals(k1, ArtifactKey.fromPortableString(k1.toPortableString()));

    ArtifactKey k2 = new ArtifactKey("g", "a", null, null);
    assertEquals(k2, ArtifactKey.fromPortableString(k2.toPortableString()));
  
    ArtifactKey k3 = new ArtifactKey("g", null, null, "c");
    assertEquals(k3, ArtifactKey.fromPortableString(k3.toPortableString()));
  }

}
