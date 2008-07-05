/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.editor.xml.search;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.BooleanClause.Occur;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.index.IndexManager;
import org.maven.ide.eclipse.index.IndexedArtifact;
import org.maven.ide.eclipse.index.IndexedArtifactFile;
import org.maven.ide.eclipse.index.IndexedArtifactGroup;


/**
 * Search engine integrating {@link IndexManager} with POM XML editor.
 * 
 * @author Lukas Krecan
 * @author Eugene Kuleshov
 */
public class IndexManagerSearchEngine implements SearchEngine {

  public Collection<String> findGroupIds(String searchExpression, Packaging packaging, ArtifactInfo containingArtifact) {
    try {
      TreeSet<String> ids = new TreeSet<String>();
      
      IndexManager indexManager = getIndexManager();
      if(packaging==Packaging.ALL) {
        Set<String> indexIds = indexManager.getIndexes().keySet();
        for(String indexId : indexIds) {
          IndexedArtifactGroup[] indexGroups = indexManager.getGroups(indexId);
          for(IndexedArtifactGroup indexedArtifactGroup : indexGroups) {
            ids.add(indexedArtifactGroup.prefix);
          }
        }
      } else {
        for(IndexedArtifact artifact : find(null, null, null, packaging)) {
          ids.add(artifact.group);
        }
      }
      
      return subSet(ids, searchExpression);
      
    } catch(IOException e) {
      throw new SearchException("Unexpected exception when searching", e);
    }
  }

  public Collection<String> findArtifactIds(String groupId, String searchExpression, Packaging packaging, ArtifactInfo containingArtifact) {
    // TODO add support for implicit groupIds in plugin dependencies "org.apache.maven.plugins", ...
    try {
      TreeSet<String> ids = new TreeSet<String>();
      for(IndexedArtifact artifact : find(groupId, null, null, packaging)) {
        ids.add(artifact.artifact);
      }
      return subSet(ids, searchExpression);
    } catch(IOException e) {
      throw new SearchException("Unexpected exception when searching", e);
    }
  }

  public Collection<String> findVersions(String groupId, String artifactId, String searchExpression, Packaging packaging) {
    try {
      Collection<IndexedArtifact> values = find(groupId, artifactId, null, packaging);
      if(values.isEmpty()) {
        return Collections.emptySet();
      }
      
      TreeSet<String> ids = new TreeSet<String>();
      Set<IndexedArtifactFile> files = values.iterator().next().files;
      for(IndexedArtifactFile artifactFile : files) {
        ids.add(artifactFile.version);
      }
      return subSet(ids, searchExpression);
    } catch(IOException ex) {
      throw new SearchException("Unexpected exception when searching", ex);
    }
  }
  
  public Collection<String> findClassifiers(String groupId, String artifactId, String version, //
      String searchExpression, Packaging packaging) {
    try {
      Collection<IndexedArtifact> values = find(groupId, artifactId, null, packaging);
      if(values.isEmpty()) {
        return Collections.emptySet();
      }
      
      TreeSet<String> ids = new TreeSet<String>();
      Set<IndexedArtifactFile> files = values.iterator().next().files;
      for(IndexedArtifactFile artifactFile : files) {
        if(artifactFile.classifier!=null) {
          ids.add(artifactFile.classifier);
        }
      }
      return subSet(ids, searchExpression);
    } catch(IOException ex) {
      throw new SearchException("Unexpected exception when searching", ex);
    }
  }
  
  public Collection<String> findTypes(String groupId, String artifactId, String version, //
      String searchExpression, Packaging packaging) {
    try {
      Collection<IndexedArtifact> values = find(groupId, artifactId, null, packaging);
      if(values.isEmpty()) {
        return Collections.emptySet();
      }
      
      TreeSet<String> ids = new TreeSet<String>();
      Set<IndexedArtifactFile> files = values.iterator().next().files;
      for(IndexedArtifactFile artifactFile : files) {
        if(artifactFile.type!=null) {
          ids.add(artifactFile.type);
        }
      }
      return subSet(ids, searchExpression);
    } catch(IOException ex) {
      throw new SearchException("Unexpected exception when searching", ex);
    }
  }

  private Collection<IndexedArtifact> find(String groupId, String artifactId, String version,
      Packaging packaging) throws IOException {
    IndexManager indexManager = getIndexManager();
 
    BooleanQuery query = new BooleanQuery();
 
    if(packaging != Packaging.ALL) {
      query.add(new TermQuery(new Term(IndexManager.FIELD_PACKAGING, packaging.getText())), Occur.MUST);
    }

    if(groupId!=null) {
      // TODO remove '-' workaround once Nexus indexer is fixed
      query.add(indexManager.createQuery(IndexManager.FIELD_GROUP_ID, groupId.replace('-', '*')), Occur.MUST);
    }

    if(artifactId != null) {
      query.add(indexManager.createQuery(IndexManager.FIELD_ARTIFACT_ID, artifactId), Occur.MUST);
    }
 
    if(version != null) {
      query.add(indexManager.createQuery(IndexManager.FIELD_VERSION, version), Occur.MUST);
    }
 
    return indexManager.search(null, query).values();
  }

  private Collection<String> subSet(TreeSet<String> ids, String searchExpression) {
    if(searchExpression==null || searchExpression.length()==0) {
      return ids;
    }
    int n = searchExpression.length();
    return ids.subSet(searchExpression, //
        searchExpression.substring(0, n - 1) + ((char) (searchExpression.charAt(n - 1) + 1)));
  }
  
  private IndexManager getIndexManager() {
    return MavenPlugin.getDefault().getIndexManager();
  }

}
