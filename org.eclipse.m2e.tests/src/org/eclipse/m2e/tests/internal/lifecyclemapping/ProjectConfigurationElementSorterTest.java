/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
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

package org.eclipse.m2e.tests.internal.lifecyclemapping;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import org.eclipse.core.runtime.IConfigurationElement;

import org.codehaus.plexus.util.dag.CycleDetectedException;

import org.eclipse.m2e.core.internal.lifecyclemapping.ProjectConfigurationElementSorter;
import org.eclipse.m2e.core.lifecyclemapping.model.IPluginExecutionMetadata;


/**
 * Sorts a list of {@link IPluginExecutionMetadata}s
 */
public class ProjectConfigurationElementSorterTest {
  @Test
  public void testSortConfigurators_HappyPath() throws Exception {

    ConfiguratorMock c0 = ConfiguratorMock.create("c0", "c1, c2", "c4,");
    ConfiguratorMock c1 = ConfiguratorMock.create("c1", "     c3,   ,   ");
    ConfiguratorMock c2 = ConfiguratorMock.create("c2");
    ConfiguratorMock c3 = ConfiguratorMock.createLegacy("c3", "c2");
    ConfiguratorMock c4 = ConfiguratorMock.create("c4");
    Map<String, IConfigurationElement> configElements = new HashMap<>(5);
    for(ConfiguratorMock c : Arrays.asList(c0, c1, c2, c3, c4)) {
      configElements.put(c.id, c);
    }

    ProjectConfigurationElementSorter sorter = new ProjectConfigurationElementSorter(configElements);

    List<String> sortedConfigurators = sorter.getSortedConfigurators();

    assertNotNull(sortedConfigurators);
    assertEquals(configElements.size(), sortedConfigurators.size());
    String msg = "invalid configurator position in " + sortedConfigurators.toString();
    assertEquals(msg, c2.id, sortedConfigurators.get(0));
    assertEquals(msg, c3.id, sortedConfigurators.get(1));
    assertEquals(msg, c1.id, sortedConfigurators.get(2));
    assertEquals(msg, c0.id, sortedConfigurators.get(3));
    assertEquals(msg, c4.id, sortedConfigurators.get(4));

    assertEquals(0, sorter.getIncompleteConfigurators().size());
  }

  @Test
  public void testSortConfigurators_CycleDetected() throws Exception {

    ConfiguratorMock c0 = ConfiguratorMock.create("c0", "c1, c2", "c4");
    ConfiguratorMock c1 = ConfiguratorMock.create("c1", "     c3,      ");
    ConfiguratorMock c2 = ConfiguratorMock.create("c2", null, "c4");
    ConfiguratorMock c3 = ConfiguratorMock.createLegacy("c3", "c2");
    ConfiguratorMock c4 = ConfiguratorMock.create("c4", "", "c1");
    Map<String, IConfigurationElement> configElements = new HashMap<>(5);
    for(ConfiguratorMock c : Arrays.asList(c0, c1, c2, c3, c4)) {
      configElements.put(c.id, c);
    }

    try {
      new ProjectConfigurationElementSorter(configElements);
      fail("Should have failed with CycleDetectedException");
    } catch(CycleDetectedException e) {
      //expected
    }
  }

  @Test
  public void testSortConfigurators_MissingFollowingConfigurator() throws Exception {

    ConfiguratorMock c0 = ConfiguratorMock.create("c0");
    ConfiguratorMock c1 = ConfiguratorMock.create("c1", "c0", "required*");
    ConfiguratorMock c2 = ConfiguratorMock.create("c2", "c1", "optional");
    Map<String, IConfigurationElement> configElements = new HashMap<>();
    for(ConfiguratorMock c : Arrays.asList(c0, c1, c2)) {
      configElements.put(c.id, c);
    }

    ProjectConfigurationElementSorter sorter = new ProjectConfigurationElementSorter(configElements);

    List<String> sortedConfigurators = sorter.getSortedConfigurators();

    assertNotNull(sortedConfigurators);
    assertEquals(sortedConfigurators + " has an unexpected size", 1, sortedConfigurators.size());
    String msg = "invalid configurator position in " + sortedConfigurators;
    assertEquals(msg, c0.id, sortedConfigurators.get(0));

    Set<String> missingConfigurators = sorter.getMissingConfigurators();
    assertEquals(1, missingConfigurators.size());
    assertEquals("required", missingConfigurators.iterator().next());

    Map<String, String> incompleteMetadatasMap = sorter.getIncompleteConfigurators();
    Set<String> incompleteMetadatas = incompleteMetadatasMap.keySet();
    String result = incompleteMetadatas.toString();
    assertEquals(result, 2, incompleteMetadatas.size());
    assertTrue(c1.id + " is missing from " + result, incompleteMetadatas.contains(c1.id));
    assertTrue(c2.id + " is missing from " + result, incompleteMetadatas.contains(c2.id));
  }

  @Test
  public void testSortConfigurators_MissingPreviousConfigurator() throws Exception {

    ConfiguratorMock c0 = ConfiguratorMock.create("c0", "optional?");
    ConfiguratorMock c1 = ConfiguratorMock.create("c1", "c0", "c2");
    ConfiguratorMock c2 = ConfiguratorMock.create("c2", "required");
    ConfiguratorMock c3 = ConfiguratorMock.create("c3", "c2", "c5");
    ConfiguratorMock c4 = ConfiguratorMock.create("c4", "c1");
    ConfiguratorMock c5 = ConfiguratorMock.create("c5", "c4");
    ConfiguratorMock c6 = ConfiguratorMock.create("c6", "c3");
    Map<String, IConfigurationElement> configElements = new HashMap<>();
    for(ConfiguratorMock c : Arrays.asList(c0, c1, c2, c3, c4, c5, c6)) {
      configElements.put(c.id, c);
    }

    ProjectConfigurationElementSorter sorter = new ProjectConfigurationElementSorter(configElements);
    List<String> sortedConfigurators = sorter.getSortedConfigurators();

    assertNotNull(sortedConfigurators);
    assertEquals(sorter.getIncompleteConfigurators() + " has an unexpected size", 3,
        sorter.getIncompleteConfigurators().size());//c2 (missing required), c3 (depends on c2), c6 (depends on c3)
    String msg = "invalid configurator position in " + sortedConfigurators.toString();
    assertEquals(msg, c0.id, sortedConfigurators.get(0));
    assertEquals(msg, c1.id, sortedConfigurators.get(1));
    assertEquals(msg, c4.id, sortedConfigurators.get(2));
    assertEquals(msg, c5.id, sortedConfigurators.get(3));

    Set<String> missingConfigurators = sorter.getMissingConfigurators();
    assertEquals(1, missingConfigurators.size());
    assertEquals("required", missingConfigurators.iterator().next());

    Map<String, String> incompleteMetadatasMap = sorter.getIncompleteConfigurators();
    Set<String> incompleteMetadatas = incompleteMetadatasMap.keySet();
    String result = incompleteMetadatas.toString();
    assertEquals(result, 3, incompleteMetadatas.size());
    assertTrue(c2.id + " is missing from " + result, incompleteMetadatas.contains(c2.id));
    assertTrue(c3.id + " is missing from " + result, incompleteMetadatas.contains(c3.id));
    assertTrue(c6.id + " is missing from " + result, incompleteMetadatas.contains(c6.id));
  }

  @Test
  public void testSortConfigurators_isRoot() throws Exception {

    ConfiguratorMock jdt = ConfiguratorMock.create("jdt");
    ConfiguratorMock android = ConfiguratorMock.create("android", "jdt?");
    Map<String, IConfigurationElement> configElements = new HashMap<>();
    for(ConfiguratorMock c : Arrays.asList(jdt, android)) {
      configElements.put(c.id, c);
    }

    ProjectConfigurationElementSorter sorter = new ProjectConfigurationElementSorter(Arrays.asList(android.id),
        configElements);
    List<String> sortedConfigurators = sorter.getSortedConfigurators();

    assertNotNull(sortedConfigurators);
    String msg = "invalid configurator position in " + sortedConfigurators.toString();
    assertEquals(msg, android.id, sortedConfigurators.get(0));

    assertTrue(android.id + " should be found as root configurator", sorter.isRootConfigurator(android.id));
  }

  @Test
  public void testSortConfigurators_471840() throws Exception {

    ConfiguratorMock jdt = ConfiguratorMock.create("jdt");
    ConfiguratorMock jpa = ConfiguratorMock.create("jpa", "jdt?,wtp?");
    ConfiguratorMock weirdo = ConfiguratorMock.create("weirdo", "jdt?,wtp?");
    ConfiguratorMock groovy = ConfiguratorMock.create("groovy");
    Map<String, IConfigurationElement> configElements = new HashMap<>();
    for(ConfiguratorMock c : Arrays.asList(jdt, jpa, groovy, weirdo)) {
      configElements.put(c.id, c);
    }

    ProjectConfigurationElementSorter sorter = new ProjectConfigurationElementSorter(
        Arrays.asList(jpa.id, groovy.id, weirdo.id), configElements);
    List<String> sortedConfigurators = sorter.getSortedConfigurators();

    assertEquals(3, sortedConfigurators.size());
    assertFalse(sortedConfigurators.contains("jdt"));

    assertTrue(groovy.id + " should be found as root configurator", sorter.isRootConfigurator(groovy.id));
    assertFalse(jpa.id + " should not be found as root configurator", sorter.isRootConfigurator(jpa.id));
    assertFalse(weirdo.id + " should not be found as root configurator", sorter.isRootConfigurator(weirdo.id));
  }

  private static class ConfiguratorMock extends ConfigElementMock {

    String id;

    private String after;

    private String before;

    private String secondaryTo;

    public ConfiguratorMock(String id) {
      this.id = id;
    }

    static ConfiguratorMock create(String id) {
      return new ConfiguratorMock(id);
    }

    static ConfiguratorMock createLegacy(String id, String secondaryTo) {
      ConfiguratorMock mock = create(id);
      mock.secondaryTo = secondaryTo;
      return mock;
    }

    static ConfiguratorMock create(String id, String runsAfter) {
      ConfiguratorMock mock = create(id);
      mock.after = runsAfter;
      return mock;
    }

    static ConfiguratorMock create(String id, String runsAfter, String runsBefore) {
      ConfiguratorMock mock = create(id, runsAfter);
      mock.before = runsBefore;
      return mock;
    }

    @Override
    public String getAttribute(String attrName) {
      switch(attrName) {
        case "runsAfter":
          return after;
        case "runsBefore":
          return before;
        case "secondaryTo":
          return secondaryTo;
        default:
          return null;
      }
    }

    @Override
    public String toString() {
      return id;
    }
  }

}
