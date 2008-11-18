/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.editor.xml.search;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.Test;
import org.maven.ide.eclipse.editor.xml.search.CompositeSearchEngine;
import org.maven.ide.eclipse.editor.xml.search.SearchEngine;
import org.maven.ide.eclipse.editor.xml.search.Packaging;

/**
 * @author Lukas Krecan
 */
public class TestCompositeSearchEngine {
  @Test
  public void testEmpty() {
    CompositeSearchEngine composite = new CompositeSearchEngine();
    assertNotNull(composite.findGroupIds("test", Packaging.ALL, null));
  }

  @Test
  public void testOneForGroupId() {
    TreeSet<String> result = new TreeSet<String>(Arrays
        .asList("test1", "test2"));
    SearchEngine component = createNiceMock(SearchEngine.class);
    expect(component.findGroupIds("test", Packaging.ALL, null)).andReturn(result);

    replay(component);

    CompositeSearchEngine composite = new CompositeSearchEngine();
    composite.addSearchEngine(component);

    assertEquals(result, composite.findGroupIds("test", Packaging.ALL, null));

    verify(component);
  }

  @Test
  public void testMoreForGroupId() {
    TreeSet<String> result1 = new TreeSet<String>(Arrays.asList("test1",
        "test2"));
    SearchEngine component1 = createNiceMock(SearchEngine.class);
    expect(component1.findGroupIds("test", Packaging.ALL, null)).andReturn(result1);

    TreeSet<String> result2 = new TreeSet<String>(Arrays.asList("test3",
        "test4"));
    SearchEngine component2 = createNiceMock(SearchEngine.class);
    expect(component2.findGroupIds("test", Packaging.ALL, null)).andReturn(result2);

    replay(component1, component2);

    CompositeSearchEngine composite = new CompositeSearchEngine();
    composite.addSearchEngine(component1);
    composite.addSearchEngine(component2);

    result1.addAll(result2);
    assertEquals(result1, composite.findGroupIds("test", Packaging.ALL, null));

    verify(component1, component2);
  }

  @Test
  public void testMoreForArtifactId() {
    TreeSet<String> result1 = new TreeSet<String>(Arrays.asList("test1",
        "test2"));
    SearchEngine component1 = createNiceMock(SearchEngine.class);
    expect(component1.findArtifactIds("test", "test", Packaging.ALL, null))
        .andReturn(result1);

    TreeSet<String> result2 = new TreeSet<String>(Arrays.asList("test3",
        "test4"));
    SearchEngine component2 = createNiceMock(SearchEngine.class);
    expect(component2.findArtifactIds("test", "test", Packaging.ALL, null))
        .andReturn(result2);

    replay(component1, component2);

    CompositeSearchEngine composite = new CompositeSearchEngine();
    composite.addSearchEngine(component1);
    composite.addSearchEngine(component2);

    result1.addAll(result2);
    assertEquals(result1, composite.findArtifactIds("test", "test",
        Packaging.ALL, null));

    verify(component1, component2);
  }

  @Test
  public void testMoreForVersion() {
    TreeSet<String> result1 = new TreeSet<String>(Arrays.asList("1.1", "1.0"));
    SearchEngine component1 = createNiceMock(SearchEngine.class);
    expect(component1.findVersions("test", "test", "", Packaging.ALL))
        .andReturn(result1);

    SearchEngine component2 = createNiceMock(SearchEngine.class);
    expect(component2.findVersions("test", "test", "", Packaging.ALL))
        .andReturn(Collections.<String> emptyList());

    replay(component1, component2);

    CompositeSearchEngine composite = new CompositeSearchEngine();
    composite.addSearchEngine(component1);
    composite.addSearchEngine(component2);

    List<String> result = composite.findVersions("test", "test", "", Packaging.ALL);
    assertEquals(result1, result);
    assertEquals("1.1", result.get(0));

    verify(component1, component2);
  }

  @Test
  public void testExceptionForGroupId() {
    TreeSet<String> result1 = new TreeSet<String>(Arrays.asList("test1",
        "test2"));
    SearchEngine component1 = createNiceMock(SearchEngine.class);
    expect(component1.findGroupIds("test", Packaging.ALL, null)).andReturn(result1);

    SearchEngine component2 = createNiceMock(SearchEngine.class);
    expect(component2.findGroupIds("test", Packaging.ALL, null)).andThrow(
        new NullPointerException("Test exception. Do not panic!"));

    replay(component1, component2);

    CompositeSearchEngine composite = new CompositeSearchEngine();
    composite.addSearchEngine(component1);
    composite.addSearchEngine(component2);

    assertEquals(result1, composite.findGroupIds("test", Packaging.ALL, null));

    verify(component1, component2);
  }
}
