package org.maven.ide.eclipse.internal.index;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.Stack;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import org.maven.ide.eclipse.MavenConsole;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.index.IndexInfo;
import org.maven.ide.eclipse.index.IndexManager;

/**
 * Index Updater Job
 * 
 * @author Eugene Kuleshov
 */
public class IndexUpdaterJob extends Job {

  private final IndexManager indexManager;
  
  private final MavenConsole console;

  private final Stack<IndexCommand> updateQueue = new Stack<IndexCommand>(); 
  
  public IndexUpdaterJob(IndexManager indexManager, MavenConsole console) {
    super("Updating indexes");
    this.indexManager = indexManager;
    this.console = console;
  }

  public void scheduleUpdate(IndexInfo indexInfo, boolean force, long delay) {
    if(IndexInfo.Type.LOCAL.equals(indexInfo.getType())) {
      updateQueue.add(new ReindexCommand(indexInfo));
      
    } else if(IndexInfo.Type.REMOTE.equals(indexInfo.getType())) {
      updateQueue.add(new UpdateCommand(indexInfo, force));
      updateQueue.add(new UnpackCommand(indexInfo, force));
    }

    schedule(delay);
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
      } catch(IOException ex) {
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
      monitor.setTaskName("Updating index " + info.getIndexName());
      try {
        Date indexTime = indexManager.fetchAndUpdateIndex(info.getIndexName(), force, monitor);
        if(indexTime==null) {
          console.logMessage("No index update available for " + info.getIndexName());
        } else {
          console.logMessage("Updated index for " + info.getIndexName() + " " + indexTime);
        }
      } catch(IOException ex) {
        String msg = "Unable to update index for " + info.getIndexName() + " " + info.getRepositoryUrl();
        MavenPlugin.log(msg, ex);
        console.logError(msg);
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
      monitor.setTaskName("Unpacking " + indexName);
      
      Date archiveIndexTime = null;
      if(info.isNew() && info.getArchiveUrl()!=null) {
        try {
          archiveIndexTime = indexManager.getIndexArchiveTime(indexArchive.openStream());
        } catch(IOException ex) {
          MavenPlugin.log("Unable to read creation time for index " + indexName, ex);
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
          index.mkdirs();
        } else {
          File[] files = index.listFiles();
          for(int j = 0; j < files.length; j++ ) {
            files[j].delete();
          }
        }
        
        InputStream is = null;
        try {
          is = indexArchive.openStream();
          indexManager.replaceIndex(indexName, is);

          console.logMessage("Unpacked index for " + info.getIndexName() + " " + archiveIndexTime);
          
          // XXX update index and repository urls
          // indexManager.removeIndex(indexName, false);
          // indexManager.addIndex(extensionIndexInfo, false);
          
        } catch(Exception ex) {
          MavenPlugin.log("Unable to unpack index " + indexName, ex);
        } finally {
          try {
            if(is != null) {
              is.close();
            }
          } catch(IOException ex) {
            MavenPlugin.log("Unable to close stream", ex);
          }
        }
      }
    }
    
  }
  
  
}