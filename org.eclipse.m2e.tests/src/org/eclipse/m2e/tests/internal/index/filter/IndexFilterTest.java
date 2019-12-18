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

package org.eclipse.m2e.tests.internal.index.filter;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.internal.index.IIndex;
import org.eclipse.m2e.core.internal.index.IndexedArtifact;
import org.eclipse.m2e.core.internal.index.IndexedArtifactFile;
import org.eclipse.m2e.core.internal.index.SearchExpression;
import org.eclipse.m2e.core.internal.index.filter.FilteredIndex;
import org.eclipse.m2e.core.internal.index.filter.IArtifactFilter;
import org.eclipse.m2e.core.ui.internal.views.nodes.IndexedArtifactFileNode;


public class IndexFilterTest extends TestCase {

  private static final String ARTIFACT = "artifact";

  private static final String GROUP = "group";

  static final ArtifactKey KEY_STATUS_NULL = new ArtifactKey(GROUP, ARTIFACT, "0", null);

  static final ArtifactKey KEY_STATUS_OK = new ArtifactKey(GROUP, ARTIFACT, "1", null);

  static final ArtifactKey KEY_STATUS_INFO = new ArtifactKey(GROUP, ARTIFACT, "2", null);

  static final ArtifactKey KEY_STATUS_WARNING = new ArtifactKey(GROUP, ARTIFACT, "3", null);

  static final ArtifactKey KEY_STATUS_ERROR = new ArtifactKey(GROUP, ARTIFACT, "4", null);

  public static class TestIndexFilter implements IArtifactFilter {

    @Override
    public IStatus filter(IProject project, ArtifactKey artifact) {
      if(KEY_STATUS_NULL.equals(artifact)) {
        return null;
      } else if(KEY_STATUS_OK.equals(artifact)) {
        return Status.OK_STATUS;
      } else if(KEY_STATUS_INFO.equals(artifact)) {
        return new Status(IStatus.INFO, "plugin", "message");
      } else if(KEY_STATUS_WARNING.equals(artifact)) {
        return new Status(IStatus.WARNING, "plugin", "message");
      } else if(KEY_STATUS_ERROR.equals(artifact)) {
        return new Status(IStatus.ERROR, "plugin", "message");
      }

      throw new IllegalArgumentException();
    }

  }

  static class MockIndex implements IIndex {

    private Map<String, IndexedArtifact> artifacts = new LinkedHashMap<>();

    public void addIndexedArtifact(IndexedArtifact artifact) {
      String key = artifact.getGroupId() + ":" + artifact.getArtifactId();
      artifacts.put(key, artifact);
    }

    @Override
    public IndexedArtifactFile getIndexedArtifactFile(ArtifactKey artifact) {
      throw new UnsupportedOperationException();
    }

    @Override
    public IndexedArtifactFile identify(File file) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Collection<IndexedArtifact> find(SearchExpression groupId, SearchExpression artifactId,
        SearchExpression version, SearchExpression packaging) {
      return artifacts.values();
    }

    @Override
    public Collection<IndexedArtifact> find(Collection<SearchExpression> groupId,
        Collection<SearchExpression> artifactId, Collection<SearchExpression> version,
        Collection<SearchExpression> packaging) {
      return artifacts.values();
    }

    @Override
    public Map<String, IndexedArtifact> search(SearchExpression expression, String searchType) {
      return artifacts;
    }

    @Override
    public Map<String, IndexedArtifact> search(SearchExpression expression, String searchType, int classifier) {
      return artifacts;
    }

  }

  public void testIndexedArtifactFileNodeAdapter() {
    IndexedArtifactFile file = new IndexedArtifactFile("repo", "group", "artifact", "version", "type", "classifier",
        "fname", 1, new Date(), 0, 0, null, null);
    IndexedArtifactFileNode node = new IndexedArtifactFileNode(file);

    assertEquals(new ArtifactKey(file.group, file.artifact, file.version, file.classifier),
        node.getAdapter(ArtifactKey.class));
    assertEquals(file, node.getAdapter(IndexedArtifactFile.class));
  }

  public void testFilteredIndex_nonEmptyResult() throws Exception {
    IndexedArtifact a = new IndexedArtifact(GROUP, ARTIFACT, "package", "classname", "packaging");
    addIndexedArtifactFile(a, KEY_STATUS_NULL);
    addIndexedArtifactFile(a, KEY_STATUS_OK);
    addIndexedArtifactFile(a, KEY_STATUS_INFO);
    addIndexedArtifactFile(a, KEY_STATUS_WARNING);
    addIndexedArtifactFile(a, KEY_STATUS_ERROR);

    MockIndex index = new MockIndex();
    index.addIndexedArtifact(a);

    IIndex filtered = new FilteredIndex(null, index);

    List<IndexedArtifact> result = new ArrayList<>(filtered.find((Collection<SearchExpression>) null,
        null, null, null));
    assertEquals(1, result.size());
    assertEquals(4, result.get(0).getFiles().size()); // error got filtered out
  }

  public void testFilteredIndex_emptyResult() throws Exception {
    IndexedArtifact a = new IndexedArtifact(GROUP, ARTIFACT, "package", "classname", "packaging");
    addIndexedArtifactFile(a, KEY_STATUS_ERROR);

    MockIndex index = new MockIndex();
    index.addIndexedArtifact(a);

    IIndex filtered = new FilteredIndex(null, index);

    List<IndexedArtifact> result = new ArrayList<>(
        filtered.find((Collection<SearchExpression>) null,
        null, null, null));
    assertEquals(0, result.size());
  }

  private void addIndexedArtifactFile(IndexedArtifact a, ArtifactKey k) {
    a.addFile(new IndexedArtifactFile("repo", k.getGroupId(), k.getArtifactId(), k.getVersion(), "type", k
        .getClassifier(), null, 1, new Date(), 0, 0, null, null));
  }
}
