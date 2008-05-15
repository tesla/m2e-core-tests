/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.editor.xml.search;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
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

  @SuppressWarnings("unchecked")
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
        BooleanQuery query = new BooleanQuery();

        query.add(new TermQuery(new Term(IndexManager.FIELD_PACKAGING, packaging.getText())), Occur.MUST);
        
        // TODO remove '-' workaround once Nexus indexer is fixed
        // query.add(indexManager.createQuery(IndexManager.FIELD_GROUP_ID, searchExpression.replace('-', '*')), Occur.MUST);

        Map<String, IndexedArtifact> result = indexManager.search(null, query);
        for(IndexedArtifact artifact : result.values()) {
          ids.add(artifact.group);
        }
      }
      
      return subSet(ids, searchExpression);
      
    } catch(IOException e) {
      throw new SearchException("Unexpected exception when searching", e);
    }
  }

  @SuppressWarnings("unchecked")
  public Collection<String> findArtifactIds(String groupId, String searchExpression, Packaging packaging, ArtifactInfo containingArtifact) {
    //TODO add support for implicit groupIds in plugin dependencies "org.apache.maven.plugins", ...
    try {
      IndexManager indexManager = getIndexManager();
      
      BooleanQuery query = new BooleanQuery();

      if(packaging != Packaging.ALL) {
        query.add(new TermQuery(new Term(IndexManager.FIELD_PACKAGING, packaging.getText())), Occur.MUST);
      }
      
      // TODO remove '-' workaround once Nexus indexer is fixed
      query.add(indexManager.createQuery(IndexManager.FIELD_GROUP_ID, groupId.replace('-', '*')), Occur.MUST);
      
      if(searchExpression.length()>0) {
        // TODO remove '-' workaround once Nexus indexer is fixed
        int lastDash = searchExpression.lastIndexOf('-');
        String searchRange = lastDash==-1 ? searchExpression : searchExpression.substring(0, lastDash);
        query.add(indexManager.createQuery(IndexManager.FIELD_ARTIFACT_ID, searchRange), Occur.SHOULD);
      }

      Collection<IndexedArtifact> values = indexManager.search(null, query).values();

      TreeSet<String> ids = new TreeSet<String>();
      for(IndexedArtifact artifact : values) {
        ids.add(artifact.artifact);
      }
      return subSet(ids, searchExpression);
    } catch(IOException e) {
      throw new SearchException("Unexpected exception when searching", e);
    }
  }

  @SuppressWarnings("unchecked")
  public Collection<String> findVersions(String groupId, String artifactId, String searchExpression, Packaging packaging) {
    try {
      IndexManager indexManager = getIndexManager();

      BooleanQuery query = new BooleanQuery();

      if(packaging != Packaging.ALL) {
        query.add(new TermQuery(new Term(IndexManager.FIELD_PACKAGING, packaging.getText())), Occur.MUST);
      }

      // TODO remove '-' workaround once Nexus indexer is fixed
      query.add(indexManager.createQuery(IndexManager.FIELD_GROUP_ID, groupId.replace('-', '*')), Occur.MUST);
      
      query.add(indexManager.createQuery(IndexManager.FIELD_ARTIFACT_ID, artifactId), Occur.MUST);

      Map<String, IndexedArtifact> result = indexManager.search(null, query);
      IndexedArtifact artifact = result.values().iterator().next();
      Set<IndexedArtifactFile> files = artifact.files;
      
      TreeSet<String> ids = new TreeSet<String>();
      for(IndexedArtifactFile artifactFile : files) {
        ids.add(artifactFile.version);
      }
      return subSet(ids, searchExpression);
    } catch(IOException ex) {
      throw new SearchException("Unexpected exception when searching", ex);
    }
  }
  
  public Collection<String> findClassifiers(String groupId, String artifactId, String version, String prefix, Packaging packaging) {
    //TODO implement
    return Arrays.asList("jdk14","jdk15");
  }
  
  public Collection<String> findTypes(String groupId, String artifactId, String version, String prefix, Packaging packaging) {
    //TODO implement
    return Arrays.asList("jar","war");
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
