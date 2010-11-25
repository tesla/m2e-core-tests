package org.eclipse.m2e.tests.util.search;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.index.IIndex;
import org.eclipse.m2e.core.index.IndexedArtifact;
import org.eclipse.m2e.core.index.IndexedArtifactFile;
import org.eclipse.m2e.core.util.search.IndexSearchEngine;
import org.eclipse.m2e.core.util.search.Packaging;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;

public class IndexSearchEngineTest extends AbstractMavenProjectTestCase {

  public void testGroupIDProposal() throws Exception {
    IndexSearchEngine engine = new IndexSearchEngine(new TestIndex());
    Collection<String> results = engine.findGroupIds("group", Packaging.ALL, null);
    assertTrue(results.contains("group"));
    assertTrue(results.contains("groupSomething"));
    assertFalse(results.contains("grouoo"));
  }
  
  private static class TestIndex implements IIndex {
    String[] entries = new String[] {
        "group",
        "groupSomething",
        "grouoo"
    };

    public TestIndex() {
    }

    public IndexedArtifactFile getIndexedArtifactFile(ArtifactKey artifact) throws CoreException {
      return null;
    }

    public IndexedArtifactFile identify(File file) throws CoreException {
      return null;
    }

    public Collection<IndexedArtifact> find(String groupId, String artifactId, String version, String packaging)
        throws CoreException {
      Set<IndexedArtifact> results = new HashSet<IndexedArtifact>();
      for (String entry : entries) {
        if (entry.startsWith(groupId)) {
          IndexedArtifact artifact = new IndexedArtifact(entry, null, null, null, null);
          results.add(artifact);
        }
      }
      return results;
    }
    
  }
}
