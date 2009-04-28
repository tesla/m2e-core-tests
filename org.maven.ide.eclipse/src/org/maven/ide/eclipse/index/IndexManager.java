/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.index;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Stack;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.progress.IProgressConstants;

import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;

import org.sonatype.nexus.index.ArtifactInfo;
import org.sonatype.nexus.index.context.IndexingContext;

import org.maven.ide.eclipse.actions.OpenMavenConsoleAction;
import org.maven.ide.eclipse.core.MavenConsole;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.embedder.ArtifactKey;
import org.maven.ide.eclipse.index.IndexInfo.Type;


/**
 * @author Eugene Kuleshov
 */
public abstract class IndexManager {

  public static final int MIN_CLASS_QUERY_LENGTH = 6;

  // index ids
  
  public static final String LOCAL_INDEX = "local";
  
  public static final String WORKSPACE_INDEX = "workspace";
  
  // search keys 
  
  public static final String SEARCH_GROUP = "groupId";

  public static final String SEARCH_ARTIFACT = "artifact";

  public static final String SEARCH_PLUGIN = "plugin";

  public static final String SEARCH_ARCHETYPE = "archetype";
  
  public static final String SEARCH_CLASS_NAME = "className";

  public static final String SEARCH_PACKAGING = "packaging";

  public static final String SEARCH_MD5 = "md5";
  
  public static final String SEARCH_SHA1 = "sha1";

  // search terms

  public static final String FIELD_GROUP_ID = ArtifactInfo.GROUP_ID;

  public static final String FIELD_ARTIFACT_ID = ArtifactInfo.ARTIFACT_ID;

  public static final String FIELD_VERSION = ArtifactInfo.VERSION;

  public static final String FIELD_PACKAGING = ArtifactInfo.PACKAGING;

  public static final String FIELD_SHA1 = ArtifactInfo.SHA1;

  public static final String FIELD_NAMES = ArtifactInfo.NAMES;
  
  // index properties
  
  public static final String INDEX_ID = IndexingContext.INDEX_ID;

  public static final String INDEX_TIMESTAMP = IndexingContext.INDEX_TIMESTAMP;

  public static final String INDEX_TIME_FORMAT = IndexingContext.INDEX_TIME_FORMAT;
  
  // availability flags
  
  public static final int PRESENT = 1;
  
  public static final int NOT_PRESENT = 0;
  
  public static final int NOT_AVAILABLE = 2;
  
  // local state
  
  protected Directory workspaceIndexDirectory = new RAMDirectory();
  
  private final ArrayList<IndexListener> listeners = new ArrayList<IndexListener>();
  
  protected final MavenConsole console;

  protected final File baseIndexDir;

  private final Map<String, IndexInfo> indexes = new LinkedHashMap<String, IndexInfo>();

  private IndexUpdaterJob updaterJob;

  public IndexManager(MavenConsole console, File stateDir, String baseName) {
    this.console = console;
    this.baseIndexDir = new File(stateDir, baseName);
  }

  public File getBaseIndexDir() {
    return baseIndexDir;
  }

  /**
   * @return map of <code>String</code> to <code>IndexInfo</code>
   */
  public Map<String, IndexInfo> getIndexes() {
    synchronized(indexes) {
      return Collections.unmodifiableMap(new HashMap<String, IndexInfo>(indexes));
    }
  }
  
  protected void addIndex(String indexName, IndexInfo indexInfo) {
    synchronized(indexes) {
      indexes.put(indexName, indexInfo);
    }
  }
  
  protected void removeIndex(String indexName) {
    synchronized(indexes) {
      indexes.remove(indexName);
    }
  }

  public IndexInfo getIndexInfo(String indexName) {
    synchronized(indexes) {
      return indexes.get(indexName);
    }
  }

  protected Directory getIndexDirectory(String indexName) throws IOException {
    IndexInfo indexInfo = getIndexInfo(indexName);
    if(indexInfo == null) {
      return null;
    }
    return getIndexDirectory(indexInfo);
  }

  protected Directory getIndexDirectory(IndexInfo indexInfo) throws IOException {
    if(indexInfo.getType() == IndexInfo.Type.WORKSPACE) {
      return workspaceIndexDirectory;
    }
    return FSDirectory.getDirectory(getIndexDirectoryFile(indexInfo));
  }

  public File getIndexDirectoryFile(IndexInfo indexInfo) {
    return new File(getBaseIndexDir(), indexInfo.getIndexName());
  }

  public void scheduleIndexUpdate(String indexName, boolean force, long delay) {
    IndexInfo indexInfo = getIndexInfo(indexName);
    if(indexInfo!=null) {
      if(updaterJob == null) {
        updaterJob = new IndexUpdaterJob(this, console);
      }
      
      if(IndexInfo.Type.LOCAL.equals(indexInfo.getType())) {
        updaterJob.addCommand(new ReindexCommand(indexInfo));
        
      } else if(IndexInfo.Type.REMOTE.equals(indexInfo.getType())) {
        updaterJob.addCommand(new UpdateCommand(indexInfo, force));
        updaterJob.addCommand(new UnpackCommand(indexInfo, force));
      }
      
      updaterJob.schedule(delay);
    }
  }

  public abstract void addIndex(IndexInfo indexInfo, boolean reindex);

  public abstract void removeIndex(String indexName, boolean delete);

  public abstract void addDocument(String indexName, File pomFile, String documentKey, //
      long size, long date, File jarFile, int sourceExists, int javadocExists);

  public abstract void removeDocument(String indexName, File pomFile, String documentKey);

  /**
   * Identify file in the index
   */
  public abstract IndexedArtifactFile identify(File file) throws CoreException;
  
  /**
   * @param term - search term
   * @param searchType - query type. Should be one of the SEARCH_* values.
   * 
   * @return Map&lt;String, IndexedArtifact&gt;
   */
  public abstract Map<String, IndexedArtifact> search(String term, String searchType) throws CoreException;

  /**
   * @return Map&lt;String, IndexedArtifact&gt;
   */
  public abstract Map<String, IndexedArtifact> search(String indexName, String prefix, String searchGroup) throws CoreException;

  /**
   * @param indexName name of the index to search or null to search in all indexes
   * @param query Lucene query that could use combinations of fields 
   *    {@link IndexManager#FIELD_GROUP_ID}, 
   *    {@link IndexManager#FIELD_ARTIFACT_ID},
   *    {@link IndexManager#FIELD_VERSION}, 
   *    {@link IndexManager#FIELD_PACKAGING}
   * 
   * @return Map&lt;String, IndexedArtifact&gt;
   */
  public abstract Map<String, IndexedArtifact> search(String indexName, Query query) throws CoreException;

  /**
   * Creates query for given field and expression
   * 
   * @param field One of 
   *    {@link IndexManager#FIELD_GROUP_ID}, 
   *    {@link IndexManager#FIELD_ARTIFACT_ID},
   *    {@link IndexManager#FIELD_VERSION}, 
   *    {@link IndexManager#FIELD_PACKAGING}
   * @param expression search text   
   */
  public abstract Query createQuery(String field, String expression) throws CoreException;
  
  public abstract IndexedArtifactFile getIndexedArtifactFile(String indexName, String documentKey) throws CoreException;
  
  public abstract IndexedArtifactGroup[] getGroups(String indexId) throws CoreException;

  public abstract IndexedArtifactGroup[] getRootGroups(String indexId) throws CoreException;

  public abstract Date reindex(String indexName, IProgressMonitor monitor) throws CoreException;

  public abstract Date fetchAndUpdateIndex(String indexName, boolean force, IProgressMonitor monitor) throws CoreException;

  public abstract Properties fetchIndexProperties(String repositoryUrl, String indexUpdateUrl, IProgressMonitor monitor)
      throws CoreException;
  
  public abstract Date replaceIndex(String indexName, InputStream is) throws CoreException;

  public abstract Date mergeIndex(String indexName, InputStream is) throws CoreException;

  public abstract Date getIndexArchiveTime(InputStream is) throws IOException;

  public void addIndexListener(IndexListener indexListener) {
    synchronized(listeners) {
      listeners.add(indexListener);
    }
  }

  public void fireIndexAdded(IndexInfo indexInfo) {
    synchronized(listeners) {
      for(IndexListener listener : listeners) {
        listener.indexAdded(indexInfo);
      }
    }
  }

  public void fireIndexRemoved(IndexInfo indexInfo) {
    synchronized(listeners) {
      for(IndexListener listener : listeners) {
        listener.indexRemoved(indexInfo);
      }
    }
  }

  public void fireIndexUpdated(IndexInfo indexInfo) {
    synchronized(listeners) {
      for(IndexListener listener : listeners) {
        listener.indexChanged(indexInfo);
      }
    }
  }

  public IndexInfo getIndexInfoByUrl(String url) {
    synchronized(indexes) {
      for(IndexInfo info : indexes.values()) {
        if(IndexInfo.Type.REMOTE.equals(info.getType()) && url.equals(info.getRepositoryUrl())) {
          return info;
        }
      }
      return null;
    }
  }

  public IndexedArtifactGroup resolveGroup(IndexedArtifactGroup group) {
    String indexName = group.info.getIndexName();
    String prefix = group.prefix;
  
    try {
      IndexedArtifactGroup g = new IndexedArtifactGroup(group.info, group.prefix);
      for(IndexedArtifact a : search(indexName, prefix, IndexManager.SEARCH_GROUP).values()) {
        String groupId = a.group;
        if(groupId.equals(prefix)) {
          g.files.put(a.artifact, a);
        } else if(groupId.startsWith(prefix + ".")) {
          int start = prefix.length() + 1;
          int end = groupId.indexOf('.', start);
          String key = end > -1 ? groupId.substring(0, end) : groupId;
          g.nodes.put(key, new IndexedArtifactGroup(group.info, key));
        }
      }
  
      return g;
  
    } catch(CoreException ex) {
      MavenLogger.log("Can't retrieve groups for " + indexName + ":" + prefix, ex);
      return group;
    }
  }

  public List<ArtifactRepository> getArtifactRepositories(ArtifactRepositoryPolicy snapshotsPolicy, ArtifactRepositoryPolicy releasesPolicy) {
    DefaultRepositoryLayout layout = new DefaultRepositoryLayout();
    
    List<ArtifactRepository> artifactRepositories = new ArrayList<ArtifactRepository>();
    for(Iterator<IndexInfo> it = getIndexes().values().iterator(); it.hasNext();) {
      IndexInfo info = it.next();
      if(Type.REMOTE.equals(info.getType())) {
        artifactRepositories.add(new DefaultArtifactRepository( //
            info.getIndexName(), info.getRepositoryUrl(), layout, snapshotsPolicy, releasesPolicy));
      }
    }
    return artifactRepositories;
  }

  public String getDocumentKey(ArtifactKey artifact) {
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
    if(classifier!=null) {
      key += "-" + classifier;
    }
  
    // TODO use artifact handler to retrieve extension
    return key + ".pom";
  }

  
  /**
   * Abstract index command
   */
  abstract static class IndexCommand {
    
    protected IndexInfo info;
    
    abstract void run(IndexManager indexManager, MavenConsole console, IProgressMonitor monitor);
    
  }

  /**
   * Reindex command
   */
  static class ReindexCommand extends IndexCommand {

    ReindexCommand(IndexInfo indexInfo) {
      this.info = indexInfo;
    }

    public void run(IndexManager indexManager, MavenConsole console, IProgressMonitor monitor) {
      monitor.setTaskName("Reindexing local repository");
      try {
        indexManager.reindex(info.getIndexName(), monitor);
        console.logMessage("Updated local repository index");
      } catch(CoreException ex) {
        console.logError("Unable to reindex local repository");
      }
    }
  }

  /**
   * Update command
   */
  static class UpdateCommand extends IndexCommand {
    private final boolean force;

    UpdateCommand(IndexInfo info, boolean force) {
      this.info = info;
      this.force = force;
    }

    public void run(IndexManager indexManager, MavenConsole console, IProgressMonitor monitor) {
      String displayName = info.getDisplayName();
      monitor.setTaskName("Updating index " + displayName);
      console.logMessage("Updating index " + displayName);
      try {
        Date indexTime = indexManager.fetchAndUpdateIndex(info.getIndexName(), force, monitor);
        if(indexTime==null) {
          console.logMessage("No index update available for " + displayName);
        } else {
          console.logMessage("Updated index for " + displayName + " " + indexTime);
        }
      } catch(CoreException ex) {
        String msg = "Unable to update index for " + displayName;
        MavenLogger.log(msg, ex);
        console.logError(msg);
      } catch (OperationCanceledException ex) {
        console.logMessage("Updating index " + displayName + " is canceled");
      }
    }
  }

  /**
   * Unpack command
   */
  static class UnpackCommand extends IndexCommand {

    private final boolean force;

    UnpackCommand(IndexInfo info, boolean force) {
      this.info = info;
      this.force = force;
    }
    
    public void run(IndexManager indexManager, MavenConsole console, IProgressMonitor monitor) {
      URL indexArchive = info.getArchiveUrl();
      if(indexArchive==null) {
        return;
      }

      String indexName = info.getIndexName();
      String displayName = info.getDisplayName();
      monitor.setTaskName("Unpacking " + displayName);
      
      Date archiveIndexTime = null;
      if(info.isNew() && info.getArchiveUrl()!=null) {
        try {
          archiveIndexTime = indexManager.getIndexArchiveTime(indexArchive.openStream());
        } catch(IOException ex) {
          MavenLogger.log("Unable to read creation time for index " + displayName, ex);
        }
      }
      
      boolean replace = force || info.isNew();
      if(!replace) {
        if(archiveIndexTime!=null) {
          Date currentIndexTime = info.getUpdateTime();
          replace = currentIndexTime==null || archiveIndexTime.after(currentIndexTime);
        }
      }

      if(replace) {
        File index = new File(indexManager.getBaseIndexDir(), indexName);
        if(!index.exists()) {
          if(!index.mkdirs()) {
            MavenLogger.log("Can't create index folder " + index.getAbsolutePath(), null);
          }
        } else {
          File[] files = index.listFiles();
          for(int j = 0; j < files.length; j++ ) {
            if(!files[j].delete()) {
              MavenLogger.log("Can't delete " + files[j].getAbsolutePath(), null);
            }
          }
        }
        
        InputStream is = null;
        try {
          is = indexArchive.openStream();
          indexManager.replaceIndex(indexName, is);

          console.logMessage("Unpacked index for " + displayName + " " + archiveIndexTime);
          
          // XXX update index and repository urls
          // indexManager.removeIndex(indexName, false);
          // indexManager.addIndex(extensionIndexInfo, false);
          
        } catch(Exception ex) {
          MavenLogger.log("Unable to unpack index " + displayName, ex);
        } finally {
          try {
            if(is != null) {
              is.close();
            }
          } catch(IOException ex) {
            MavenLogger.log("Unable to close stream", ex);
          }
        }
      }
    }
    
  }

  public static class IndexUpdaterRule implements ISchedulingRule {

    public boolean contains(ISchedulingRule rule) {
      return rule == this;
    }

    public boolean isConflicting(ISchedulingRule rule) {
      return rule == this;
    }
    
  }
  
  static class IndexUpdaterJob extends Job {

    private final IndexManager indexManager;
    
    private final MavenConsole console;

    private final Stack<IndexCommand> updateQueue = new Stack<IndexCommand>(); 
    
    public IndexUpdaterJob(IndexManager indexManager, MavenConsole console) {
      super("Updating indexes");
      this.indexManager = indexManager;
      this.console = console;
      setProperty(IProgressConstants.ACTION_PROPERTY, new OpenMavenConsoleAction());
      setRule(new IndexUpdaterRule());
    }

    public void addCommand(IndexCommand indexCommand) {
      updateQueue.add(indexCommand);
    }
    
    protected IStatus run(IProgressMonitor monitor) {
      monitor.beginTask(getName(), IProgressMonitor.UNKNOWN);
      
      while(!updateQueue.isEmpty()) {
        IndexCommand command = updateQueue.pop();
        command.run(indexManager, console, monitor);
      }
      
      monitor.done();
      
      return Status.OK_STATUS;
    }
    
  }  
}
