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

package org.eclipse.m2e.tests.internal.conversion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;

import org.codehaus.plexus.util.dag.CycleDetectedException;

import org.apache.maven.model.Model;

import org.eclipse.m2e.core.internal.project.conversion.DuplicateConversionParticipantException;
import org.eclipse.m2e.core.internal.project.conversion.ProjectConversionParticipantSorter;
import org.eclipse.m2e.core.project.conversion.AbstractProjectConversionParticipant;


/**
 * ProjectConversionParticipantSorterTest
 * 
 * @author Fred Bricon
 */
public class ProjectConversionParticipantSorterTest {

  @Test
  public void testSortConverters_After() throws Exception {

    AbstractProjectConversionParticipant c0 = new MockConverter("c0", "c1, c2");
    AbstractProjectConversionParticipant c1 = new MockConverter("c1", "     c3,      ");
    AbstractProjectConversionParticipant c2 = new MockConverter("c2", "c999");
    AbstractProjectConversionParticipant c3 = new MockConverter("c3", "c2");
    AbstractProjectConversionParticipant c4 = new MockConverter("c4");

    List<AbstractProjectConversionParticipant> converters = Arrays.asList(c0, c1, c2, c3, c4);
    ProjectConversionParticipantSorter sorter = new ProjectConversionParticipantSorter(converters);
    List<AbstractProjectConversionParticipant> sortedConverters = sorter.getSortedConverters();

    assertNotNull(sortedConverters);
    assertEquals(converters.size(), sortedConverters.size());
    String msg = "invalid converter position in " + sortedConverters.toString();
    assertSame(msg, c2, sortedConverters.get(0));
    assertSame(msg, c3, sortedConverters.get(1));
    assertSame(msg, c1, sortedConverters.get(2));
    assertSame(msg, c0, sortedConverters.get(3));
    assertSame(msg, c4, sortedConverters.get(4));
  }

  @Test
  public void testSortConverters_After2() throws Exception {

    AbstractProjectConversionParticipant c0 = new MockConverter("c0", "c2, c1, c2");
    AbstractProjectConversionParticipant c1 = new MockConverter("c1");
    AbstractProjectConversionParticipant c2 = new MockConverter("c2");

    List<AbstractProjectConversionParticipant> converters = Arrays.asList(c0, c1, c2);
    ProjectConversionParticipantSorter sorter = new ProjectConversionParticipantSorter(converters);
    List<AbstractProjectConversionParticipant> sortedConverters = sorter.getSortedConverters();

    assertNotNull(sortedConverters);
    assertEquals(converters.size(), sortedConverters.size());
    String msg = "invalid converter position in " + sortedConverters.toString();
    assertSame(msg, c2, sortedConverters.get(0));
    assertSame(msg, c1, sortedConverters.get(1));
    assertSame(msg, c0, sortedConverters.get(2));
  }

  @Test
  public void testSortConverters_AfterBefore() throws Exception {
    AbstractProjectConversionParticipant c0 = new MockConverter("c0", "c1, c2", "c3");
    AbstractProjectConversionParticipant c1 = new MockConverter("c1", " ", " ");
    AbstractProjectConversionParticipant c2 = new MockConverter("c2", null, "c1");
    AbstractProjectConversionParticipant c3 = new MockConverter("c3", "c2");

    List<AbstractProjectConversionParticipant> converters = Arrays.asList(c0, c1, c2, c3);
    ProjectConversionParticipantSorter sorter = new ProjectConversionParticipantSorter(converters);
    List<AbstractProjectConversionParticipant> sortedConverters = sorter.getSortedConverters();

    assertNotNull(sortedConverters);
    assertEquals(converters.size(), sortedConverters.size());
    String msg = "invalid converter position in " + sortedConverters.toString();
    assertSame(msg, c2, sortedConverters.get(0));
    assertSame(msg, c1, sortedConverters.get(1));
    assertSame(msg, c0, sortedConverters.get(2));
    assertSame(msg, c3, sortedConverters.get(3));
  }

  @Test
  public void testSortConverters_Cycle_After() throws Exception {

    AbstractProjectConversionParticipant c0 = new MockConverter("c0", "c1, c2");
    AbstractProjectConversionParticipant c1 = new MockConverter("c1");
    AbstractProjectConversionParticipant c2 = new MockConverter("c2", "c0");

    try {
      new ProjectConversionParticipantSorter(Arrays.asList(c0, c1, c2));
      fail("A cycle should have been detected");
    } catch(CycleDetectedException ex) {
      //expected
    }
  }

  @Test
  public void testSortConverters_Cycle_Before() throws Exception {

    AbstractProjectConversionParticipant c0 = new MockConverter("c0", null, "c1");
    AbstractProjectConversionParticipant c1 = new MockConverter("c1", null, "c0");

    try {
      new ProjectConversionParticipantSorter(Arrays.asList(c0, c1));
      fail("A cycle should have been detected");
    } catch(CycleDetectedException ex) {
      //expected
    }
  }

  @Test
  public void testSortConverters_DuplicateConverters() throws Exception {

    AbstractProjectConversionParticipant c0 = new MockConverter("c0");
    AbstractProjectConversionParticipant c1 = new MockConverter(c0.getId());

    try {
      new ProjectConversionParticipantSorter(Arrays.asList(c0, c1));
      fail("Duplicate ConversionParticipant should have been detected");
    } catch(DuplicateConversionParticipantException ex) {
      //expected
    }
  }

  private static class MockConverter extends AbstractProjectConversionParticipant {

    private String id;

    private String after;

    private String before;

    MockConverter(String id) {
      this(id, null, null);
    }

    MockConverter(String id, String after) {
      this(id, after, null);
    }

    MockConverter(String id, String after, String before) {
      super();
      this.id = id;
      this.after = after;
      this.before = before;
    }

    @Override
    public String getId() {
      return id;
    }

    @Override
    public boolean accept(IProject project) {
      return true;
    }

    @Override
    public void convert(IProject project, Model model, IProgressMonitor monitor) {
    }

    @Override
    public String[] getPrecedingConverterIds() {
      return split(after);
    }

    @Override
    public String[] getSucceedingConverterIds() {
      return split(before);
    }

    @Override
    public String toString() {
      return getId();
    }
  }
}
