/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.project;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.project.MavenUpdateRequest;


public class MavenProjectManagerRefreshJob extends Job implements IResourceChangeListener {

  private final MavenProjectManagerImpl manager;

  private final List refreshQueue = new ArrayList();

  private final List downloadSourcesQueue = new ArrayList();

  private static final int DELTA_FLAGS = IResourceDelta.CONTENT | IResourceDelta.MOVED_FROM | IResourceDelta.MOVED_TO
      | IResourceDelta.COPIED_FROM | IResourceDelta.REPLACED;

  public MavenProjectManagerRefreshJob(MavenProjectManagerImpl manager) {
    super("Updating Maven Dependencies");
    this.manager = manager;
  }
  
  public void refresh(MavenUpdateRequest updateRequest) {
    queue(refreshQueue, new RemoveCommand(updateRequest));
    schedule(1000L);
  }

  public void downloadSources(IProject project, IPath path, String groupId, String artifactId, String version, String classifier) {
    queue(downloadSourcesQueue, new DownloadSourcesCommand(project, path, new ArtifactKey(groupId, artifactId, version, classifier)));
    schedule(1000L);
  }
  

  // Job
  
  protected IStatus run(IProgressMonitor monitor) {
    monitor.beginTask("Refreshing Maven model", IProgressMonitor.UNKNOWN);
    try {
      CommandContext context = new CommandContext(manager);

      // find all poms that need to be refreshed 
      executeCommands(refreshQueue, context, monitor);

      // refresh affected poms
      int updateRequestCount = context.updateRequests.size();
      if(updateRequestCount > 0) {
        monitor.subTask("Refreshing Maven model");
        manager.refresh(context.updateRequests, monitor);
      }

      // download sources
      executeCommands(downloadSourcesQueue, context, monitor);

      if(context.downloadSources.size() > 0) {
        monitor.subTask("Downloading sources");
        manager.downloadSources(context.downloadSources, monitor);
      }

    } catch(InterruptedException ex) {
      return Status.CANCEL_STATUS;
      
    } catch(CoreException ex) {
      MavenPlugin.log(ex);
      
    } catch(Exception ex) {
      MavenPlugin.log(ex.getMessage(), ex);
      
    } finally {
      monitor.done();
      
    }

    return Status.OK_STATUS;
  }

  private void executeCommands(List queue, CommandContext context, IProgressMonitor monitor)
      throws InterruptedException {
    Command[] commands;
    synchronized(queue) {
      commands = (Command[]) queue.toArray(new Command[queue.size()]);
      queue.clear();
    }

    if(commands.length > 0) {
      for(int i = 0; i < commands.length; i++ ) {
        if(monitor.isCanceled()) {
          throw new InterruptedException();
        }
        commands[i].execute(context, monitor);
      }
    }
  }

  
  // IResourceChangeListener
  
  public void resourceChanged(IResourceChangeEvent event) {
    // XXX should respect the global settings
    boolean offline = false;  
    boolean updateSnapshots = false;

    int type = event.getType();
    
    if(IResourceChangeEvent.PRE_CLOSE == type || IResourceChangeEvent.PRE_DELETE == type) {
      queue(refreshQueue, new RemoveCommand(new MavenUpdateRequest((IProject) event.getResource(), //
          offline, updateSnapshots)));
      
    } else {
      // if (IResourceChangeEvent.POST_CHANGE == type)
      IResourceDelta delta = event.getDelta(); // workspace delta
      IResourceDelta[] projectDeltas = delta.getAffectedChildren();
      Set removeProjects = new LinkedHashSet();
      Set refreshProjects = new LinkedHashSet();
      for(int i = 0; i < projectDeltas.length; i++ ) {
        try {
          projectChanged(projectDeltas[i], removeProjects, refreshProjects);
        } catch(CoreException ex) {
          MavenPlugin.log(ex);
        }
      }
      
      if(!removeProjects.isEmpty()) {
        IProject[] projects = (IProject[]) removeProjects.toArray(new IProject[removeProjects.size()]);
        queue(refreshQueue, new RemoveCommand(new MavenUpdateRequest(projects, offline, updateSnapshots)));
      }
      if(!refreshProjects.isEmpty()) {
        IProject[] projects = (IProject[]) refreshProjects.toArray(new IProject[refreshProjects.size()]);
        queue(refreshQueue, new RefreshCommand(new MavenUpdateRequest(projects, offline, updateSnapshots)));
      }
    }

    synchronized(refreshQueue) {
      if(!refreshQueue.isEmpty()) {
        schedule(1000L);
      }
    }
  }

  private void projectChanged(IResourceDelta delta, Set removeProjects, final Set refreshProjects)
      throws CoreException {
    final IProject project = (IProject) delta.getResource();
    
    // hasNature seem to report *before* state here
    // XXX OPEN is not delta.getKind(), it is delta.getFlags()
    if(IResourceDelta.OPEN == delta.getKind()) {
      // IResourceDelta.CHANGED is coming shortly, don't need to refresh twice
      return;
    }
    
    if(delta.findMember(new Path(".project")) != null || delta.findMember(new Path(".classpath")) != null) {
      // let's play safe here, and force refresh of the maven project cache
      removeProjects.add(project);
      
    } else {
      delta.accept(new IResourceDeltaVisitor() {
        public boolean visit(IResourceDelta delta) {
          IResource resource = delta.getResource();
          if(resource instanceof IFile && MavenPlugin.POM_FILE_NAME.equals(resource.getName())) {
            // XXX ignore output folders
            if(delta.getKind() == IResourceDelta.REMOVED || (delta.getFlags() & DELTA_FLAGS) != 0) {
              // XXX check for interesting resources
              refreshProjects.add(project);
            }
          }
          return true;
        }
      });
    }
  }
  
  private void queue(List queue, Command command) {
    synchronized(queue) {
      queue.add(command);
    }
  }

  static final class CommandContext {

    public final MavenProjectManagerImpl manager;
    
    public final Set onlinePoms = new LinkedHashSet();

    public final Set offlinePoms = new LinkedHashSet();

    public final List downloadSources = new ArrayList();

    public int sourcesCount = 0;

    /**
     * Set of <code>MavenUpdateRequest</code>
     */
    public Set updateRequests = new LinkedHashSet();

    CommandContext(MavenProjectManagerImpl manager) {
      this.manager = manager;
    }
  }

  /**
   * Command for background execution
   */
  static abstract class Command {
    abstract void execute(CommandContext context, IProgressMonitor monitor);
  }
  
  /**
   * Remove (forced refresh)
   */
  private static class RemoveCommand extends Command {
    private final MavenUpdateRequest updateRequest;

    RemoveCommand(MavenUpdateRequest updateRequest) {
      this.updateRequest = updateRequest;
    }

    void execute(CommandContext context, IProgressMonitor monitor) {
      updateRequest.addPomFiles(context.manager.remove(updateRequest.getPomFiles()));
      context.updateRequests.add(updateRequest);
    }

    public String toString() {
      return "REMOVE " + updateRequest.toString();
    }
  }

  /**
   * Refresh
   */
  private static class RefreshCommand extends Command {
    private final MavenUpdateRequest updateRequest;

    RefreshCommand(MavenUpdateRequest updateRequest) {
      this.updateRequest = updateRequest;
    }

    void execute(CommandContext context, IProgressMonitor monitor) {
      context.updateRequests.add(updateRequest);
    }

    public String toString() {
      return "REFRESH " + updateRequest.toString();
    }
  }

  /**
   * Download sources
   */
  private static class DownloadSourcesCommand extends Command {
    private final IProject project;
    private final IPath path;
    private final ArtifactKey artifactKey;

    DownloadSourcesCommand(IProject project, IPath path, ArtifactKey artifactKey) {
      this.project = project;
      this.path = path;
      this.artifactKey = artifactKey;
    }

    void execute(CommandContext context, IProgressMonitor monitor) {
      context.downloadSources.add(new DownloadSourceRequest(project, path, artifactKey));
      context.sourcesCount++ ;
    }

    public String toString() {
      return "DOWNLOADSOURCES " + project.getName() + " " + path;
    }
  }
}
