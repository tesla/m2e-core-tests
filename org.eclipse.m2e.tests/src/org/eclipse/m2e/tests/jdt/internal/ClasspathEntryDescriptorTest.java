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

package org.eclipse.m2e.tests.jdt.internal;

import org.junit.Assert;
import org.junit.Test;

import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;

import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.jdt.internal.ClasspathEntryDescriptor;


public class ClasspathEntryDescriptorTest {

  @Test
  public void testArtifactKeyDeserialization() throws Exception {
    final ArtifactKey key = new ArtifactKey("g", "a", "v", "c");

    ClasspathEntryDescriptor original = new ClasspathEntryDescriptor(IClasspathEntry.CPE_LIBRARY, new Path("/path"));
    original.setArtifactKey(key);

    ClasspathEntryDescriptor deserelized = new ClasspathEntryDescriptor(original.toClasspathEntry());

    Assert.assertEquals(key, deserelized.getArtifactKey());
  }
}
