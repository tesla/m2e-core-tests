/*******************************************************************************
 * Copyright (c) 2013 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Red Hat, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.tests.archetype;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import org.apache.maven.archetype.catalog.Archetype;

import org.eclipse.m2e.core.ui.internal.util.ArchetypeUtil;


/**
 * ArchetypeUtil Test
 * 
 * @author Fred Bricon
 */
public class ArchetypeUtilTest {
  @Test
  public void testAreEqual() {

    Archetype one = null, another = null;
    assertTrue(ArchetypeUtil.areEqual(one, another));
    one = new Archetype();
    assertFalse(ArchetypeUtil.areEqual(one, another));
    another = one;
    assertTrue(ArchetypeUtil.areEqual(one, another));

    one = createArchetype();
    another = createArchetype();
    another.setDescription("something");
    assertTrue(ArchetypeUtil.areEqual(one, another));

    another.setVersion("2");
    assertFalse(ArchetypeUtil.areEqual(one, another));

  }

  private Archetype createArchetype() {
    Archetype a = new Archetype();
    a.setGroupId("g");
    a.setArtifactId("a");
    a.setVersion("1");
    a.setDescription("description");
    return a;
  }

  @Test
  public void testGetHashCode() {
    assertEquals(-1, ArchetypeUtil.getHashCode(null));

    Archetype one = createArchetype();
    Archetype another = createArchetype();
    assertEquals(ArchetypeUtil.getHashCode(one), ArchetypeUtil.getHashCode(another));

    another.setDescription("another");
    assertEquals(ArchetypeUtil.getHashCode(one), ArchetypeUtil.getHashCode(another));

    another.setVersion("2");
    assertFalse(ArchetypeUtil.getHashCode(one) == ArchetypeUtil.getHashCode(another));
  }

}
