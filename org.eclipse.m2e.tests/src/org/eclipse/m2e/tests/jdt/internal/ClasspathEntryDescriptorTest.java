/*******************************************************************************
 * Copyright (c) 2010 Sonatype, Inc.
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

package org.eclipse.m2e.tests.jdt.internal;

import org.junit.Assert;
import org.junit.Test;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;

import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.jdt.internal.ClasspathEntryDescriptor;


public class ClasspathEntryDescriptorTest {

  @Test
  public void testArtifactKeyDeserialization() throws Exception {
    final ArtifactKey key = new ArtifactKey("g", "a", "v", "c");

    ClasspathEntryDescriptor original = new ClasspathEntryDescriptor(IClasspathEntry.CPE_LIBRARY, IPath.fromOSString("/path"));
    original.setArtifactKey(key);

    ClasspathEntryDescriptor deserelized = new ClasspathEntryDescriptor(original.toClasspathEntry());

    Assert.assertEquals(key, deserelized.getArtifactKey());
  }
}
