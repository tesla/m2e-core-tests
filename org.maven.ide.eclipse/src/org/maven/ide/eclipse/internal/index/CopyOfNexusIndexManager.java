/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.index;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import org.sonatype.nexus.artifact.Gav;
import org.sonatype.nexus.artifact.GavCalculator;
import org.sonatype.nexus.artifact.M2GavCalculator;
import org.sonatype.nexus.index.ArtifactAvailablility;
import org.sonatype.nexus.index.ArtifactContext;
import org.sonatype.nexus.index.ArtifactInfo;
import org.sonatype.nexus.index.FlatSearchRequest;
import org.sonatype.nexus.index.FlatSearchResponse;
import org.sonatype.nexus.index.NexusIndexer;
import org.sonatype.nexus.index.context.IndexCreator;
import org.sonatype.nexus.index.context.IndexingContext;
import org.sonatype.nexus.index.locator.PomLocator;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.core.IMavenConstants;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.embedder.ArtifactKey;
import org.maven.ide.eclipse.index.IIndex;
import org.maven.ide.eclipse.index.IMutableIndex;
import org.maven.ide.eclipse.index.IndexInfo;
import org.maven.ide.eclipse.index.IndexManager;
import org.maven.ide.eclipse.index.IndexedArtifact;
import org.maven.ide.eclipse.index.IndexedArtifactFile;


/**
 * NexusIndexer based implementation of IndexManager.
 * 
 * @author Eugene Kuleshov
 */
public class CopyOfNexusIndexManager extends IndexManager {

  /** Field separator */
  public static final String FS = "|";

  /** Non available value */
  public static final String NA = "NA";

  // TODO do we need to redefine index fields here?

  public static final String FIELD_GROUP_ID = ArtifactInfo.GROUP_ID;

  public static final String FIELD_ARTIFACT_ID = ArtifactInfo.ARTIFACT_ID;

  public static final String FIELD_VERSION = ArtifactInfo.VERSION;

  public static final String FIELD_PACKAGING = ArtifactInfo.PACKAGING;

  public static final String FIELD_SHA1 = ArtifactInfo.SHA1;

  public static final String FIELD_NAMES = ArtifactInfo.NAMES;

  private NexusIndexer indexer;

  private final GavCalculator gavCalculator = new M2GavCalculator();

  private NexusIndex workspaceIndex;

  private NexusIndex localIndex;

  private File baseIndexDir;

  public void addDocument(String indexName, File file, String documentKey, long size, long date, File jarFile,
      int sourceExists, int javadocExists) {
    try {
      IndexingContext context = getIndexingContext(indexName);
      if(context == null) {
        // TODO log
        return;
      }

      ArtifactContext artifactContext = getArtifactContext(file, documentKey, size, date, //
          sourceExists, javadocExists, context.getRepositoryId());
      getIndexer().addArtifactToIndex(artifactContext, context);
//      fireIndexUpdated(getIndexInfo(indexName));

    } catch(Exception ex) {
      String msg = "Unable to add " + documentKey;
      MavenLogger.log(msg, ex);
    }
  }

  public void removeDocument(String indexName, File file, String documentKey) {
    try {
      IndexingContext context = getIndexingContext(indexName);
      if(context == null) {
        // TODO log
        return;
      }

      ArtifactContext artifactContext = getArtifactContext(null, documentKey, -1, -1, //
          IIndex.NOT_AVAILABLE, IIndex.NOT_AVAILABLE, context.getRepositoryId());

      getIndexer().deleteArtifactFromIndex(artifactContext, context);

//      fireIndexUpdated(getIndexInfo(indexName));

    } catch(Exception ex) {
      String msg = "Unable to remove " + documentKey;
      MavenLogger.log(msg, ex);
    }
  }

  private IndexingContext getIndexingContext(String indexName) throws CoreException {
    return indexName == null ? null : getIndexer().getIndexingContexts().get(indexName);
  }

  private synchronized NexusIndexer getIndexer() {
    if(indexer == null) {
      indexer = MavenPlugin.lookup(NexusIndexer.class);
    }
    return indexer;
  }

  private ArtifactContext getArtifactContext(File file, String documentKey, long size, long date, int sourceExists,
      int javadocExists, String repository) {
    Gav gav = gavCalculator.pathToGav(documentKey);

    String groupId = gav.getGroupId();
    String artifactId = gav.getArtifactId();
    String version = gav.getVersion();
    String classifier = gav.getClassifier();

    ArtifactInfo ai = new ArtifactInfo(repository, groupId, artifactId, version, classifier);
    ai.sourcesExists = ArtifactAvailablility.fromString(Integer.toString(sourceExists));
    ai.javadocExists = ArtifactAvailablility.fromString(Integer.toString(javadocExists));
    ai.size = size;
    ai.lastModified = date;

    File pomFile;
    File artifactFile;

    if(file == null || "pom.xml".equals(file.getName())) {
      pomFile = file;
      artifactFile = null;
      // TODO set ai.classNames

    } else if(file.getName().endsWith(".pom")) {
      pomFile = file;
      String path = file.getAbsolutePath();
      artifactFile = new File(path.substring(0, path.length() - 4) + ".jar");

    } else {
      pomFile = new PomLocator().locate(file, gavCalculator, gav);
      artifactFile = file;
    }

    return new ArtifactContext(pomFile, artifactFile, null, ai, gav);
  }

  public IMutableIndex getWorkspaceIndex() {
    return workspaceIndex;
  }

  public IMutableIndex getLocalIndex() {
    return localIndex;
  }

  static String getDocumentKey(ArtifactKey artifact) {
    String groupId = artifact.getGroupId();
    if(groupId == null) {
      groupId = "[inherited]";
    }

    String artifactId = artifact.getArtifactId();

    String version = artifact.getVersion();
    if(version == null) {
      version = "[inherited]";
    }

    String key = groupId.replace('.', '/') + '/' + artifactId + '/' + version + '/' + artifactId + "-" + version;

    String classifier = artifact.getClassifier();
    if(classifier != null) {
      key += "-" + classifier;
    }

    // TODO use artifact handler to retrieve extension
    return key + ".pom";
  }

  public void addRemoteIndex(IndexInfo indexInfo) {
    try {
      getIndexer().addIndexingContextForced(indexInfo.getRepositoryUrl(), //
          indexInfo.getRepositoryUrl(), //
          null, //
          getIndexDirectory(indexInfo), //
          indexInfo.getRepositoryUrl(), //
          null, //
          getIndexCreators(indexInfo));

//      fireIndexAdded(indexInfo);

    } catch(IOException ex) {
      // XXX how to recover from this?
      String msg = "Error on adding indexing context for repository " + indexInfo.getRepositoryUrl();
      MavenLogger.log(msg, ex);
    }
  }

  /**
   * @param indexInfo
   * @return
   */
  private List<? extends IndexCreator> getIndexCreators(IndexInfo indexInfo) {
    // TODO Auto-generated method getIndexCreators
    return null;
  }

  protected Directory getIndexDirectory(IndexInfo indexInfo) throws IOException {
    return FSDirectory.getDirectory(getIndexDirectoryFile(indexInfo));
  }

  public File getIndexDirectoryFile(IndexInfo indexInfo) {
    String indexId = indexInfo.getRepositoryUrl(); // TODO escape/replace bad characters

    return new File(getBaseIndexDir(), indexId);
  }

  public File getBaseIndexDir() {
    return baseIndexDir;
  }

  public Query createQuery(String field, String expression) {
    return getIndexer().constructQuery(field, expression);
  }

  public Map<String, IndexedArtifact> search(String indexName, Query query) throws CoreException {
    Map<String, IndexedArtifact> result = new TreeMap<String, IndexedArtifact>();
    try {
      IndexingContext context = getIndexingContext(indexName);
      FlatSearchResponse response;
      if(context == null) {
        response = getIndexer().searchFlat(new FlatSearchRequest(query));
      } else {
        response = getIndexer().searchFlat(new FlatSearchRequest(query, context));
      }

      for(ArtifactInfo artifactInfo : response.getResults()) {
        IndexedArtifactFile af = getIndexedArtifactFile(artifactInfo);
        addArtifactFile(result, af, null, null, artifactInfo.packaging);
      }

    } /*catch(IndexContextInInconsistentStateException ex) {
      String msg = "Inconsistent index context state " + ex.getMessage();
      console.logError(msg);
      MavenLogger.log(msg, ex);
      } */catch(IOException ex) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, "Search error", ex));
    }
    return result;
  }

  private void addArtifactFile(Map<String, IndexedArtifact> result, IndexedArtifactFile af, String className,
      String packageName, String packaging) {
    String key = className + " : " + packageName + " : " + af.group + " : " + af.artifact;
    IndexedArtifact indexedArtifact = result.get(key);
    if(indexedArtifact == null) {
      indexedArtifact = new IndexedArtifact(af.group, af.artifact, packageName, className, packaging);
      result.put(key, indexedArtifact);
    }
    indexedArtifact.addFile(af);
  }

  IndexedArtifactFile getIndexedArtifactFile(ArtifactInfo artifactInfo) {
    String groupId = artifactInfo.groupId;
    String artifactId = artifactInfo.artifactId;
    String repository = artifactInfo.repository;
    String version = artifactInfo.version;
    String classifier = artifactInfo.classifier;
    String packaging = artifactInfo.packaging;
    String fname = artifactInfo.fname;
    if(fname == null) {
      fname = artifactId + '-' + version + (classifier != null ? '-' + classifier : "")
          + (packaging != null ? ('.' + packaging) : "");
    }

    long size = artifactInfo.size;
    Date date = new Date(artifactInfo.lastModified);

    int sourcesExists = artifactInfo.sourcesExists.ordinal();
    int javadocExists = artifactInfo.javadocExists.ordinal();

    String prefix = artifactInfo.prefix;
    List<String> goals = artifactInfo.goals;

    return new IndexedArtifactFile(repository, groupId, artifactId, version, packaging, classifier, fname, size, date,
        sourcesExists, javadocExists, prefix, goals);
  }

  public IndexedArtifactFile getIndexedArtifactFile(String indexName, ArtifactKey artifactKey) throws CoreException {

    try {
      String key = getGAV(artifactKey.getGroupId(), //
          artifactKey.getArtifactId(), artifactKey.getVersion(), artifactKey.getClassifier());

      TermQuery query = new TermQuery(new Term(ArtifactInfo.UINFO, key));
      ArtifactInfo artifactInfo = getIndexer().identify(query, Collections.singleton(getIndexingContext(indexName)));
      if(artifactInfo != null) {
        return getIndexedArtifactFile(artifactInfo);
      }
    } catch(Exception ex) {
      String msg = "Illegal artifact coordinate " + ex.getMessage();
      MavenLogger.log(msg, ex);
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, "Search error", ex));
    }
    return null;
  }

  public static String getGAV(String groupId, String artifactId, String version, String classifier) {
    return new StringBuilder() //
        .append(groupId).append(FS) //
        .append(artifactId).append(FS) //
        .append(version).append(FS) //
        .append(nvl(classifier)).toString();
  }

  public static String nvl(String v) {
    return v == null ? NA : v;
  }

  public IndexedArtifactFile identify(String indexId, File file) throws CoreException {
    // XXX index-specific search
    try {
      ArtifactInfo artifactInfo = getIndexer().identify(file);
      return artifactInfo==null ? null : getIndexedArtifactFile(artifactInfo);
    } catch(IOException ex) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, "Search error", ex));
    }
  }
  
}
