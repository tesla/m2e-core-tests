/*******************************************************************************
 * Copyright (c) 2013 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Red Hat, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.tests.archetype;

import junit.framework.TestCase;

import org.apache.maven.archetype.catalog.Archetype;

import org.eclipse.m2e.core.archetype.ArchetypeUtil;


/**
 * ArchetypeUtil Test
 * 
 * @author Fred Bricon
 */
public class ArchetypeUtilTest extends TestCase {

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
