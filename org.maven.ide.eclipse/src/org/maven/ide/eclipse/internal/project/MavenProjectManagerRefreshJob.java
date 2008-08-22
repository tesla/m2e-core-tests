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
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;

import org.maven.ide.eclipse.core.IMavenConstants;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.embedder.ArtifactKey;
import org.maven.ide.eclipse.embedder.MavenRuntimeManager;
import org.maven.ide.eclipse.project.MavenUpdateRequest;

public class MavenProjectManagerRefreshJob extends Job implements IResourceChangeListener, IPreferenceChangeListener {

  private static final int DELTA_FLAGS = IResourceDelta.CONTENT | IResourceDelta.MOVED_FROM | IResourceDelta.MOVED_TO
  | IResourceDelta.COPIED_FROM | IResourceDelta.REPLACED;
  
  private final List<Command> refreshQueue = new ArrayList<Command>();

  private final List<Command> downloadSourcesQueue = new ArrayList<Command>();

  private final MavenProjectManagerImpl manager;
  
  private final MavenRuntimeManager runtimeManager;
  
  public MavenProjectManagerRefreshJob(MavenProjectManagerImpl manager, MavenRuntimeManager runtimeManager) {
    super("Updating Maven Dependencies");
    this.manager = manager;
    this.runtimeManager = runtimeManager;
    setRule(new SchedulingRule(true));
  }
  
  public void refresh(MavenUpdateRequest updateRequest) {
    queue(refreshQueue, new RemoveCommand(updateRequest));
    schedule(1000L);
  }

  public void downloadSources(IProject project, IPath path, String groupId, String artifactId, String version,
      String classifier, boolean downloadSources, boolean downloadJavaDoc) {
    queue(downloadSourcesQueue, new DownloadSourcesCommand(project, path, //
        new ArtifactKey(groupId, artifactId, version, classifier), downloadSources, downloadJavaDoc));
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

      if(context.downloadRequests.size() > 0) {
        monitor.subTask("Downloading sources");
        manager.downloadSources(context.downloadRequests, monitor);
      }

    } catch(InterruptedException ex) {
      return Status.CANCEL_STATUS;
      
    } catch(CoreException ex) {
      MavenLogger.log(ex);
      
    } catch(Exception ex) {
      MavenLogger.log(ex.getMessage(), ex);
      
    } finally {
      monitor.done();
      
    }

    return Status.OK_STATUS;
  }

  private void executeCommands(List<Command> queue, CommandContext context, IProgressMonitor monitor)
      throws InterruptedException {
    Command[] commands;
    synchronized(queue) {
      commands = queue.toArray(new Command[queue.size()]);
      queue.clear();
    }

    if(commands.length > 0) {
      for(Command command : commands) {
        if(monitor.isCanceled()) {
          throw new InterruptedException();
        }
        command.execute(context, monitor);
      }
    }
  }

  
  // IResourceChangeListener
  
  public void resourceChanged(IResourceChangeEvent event) {
    boolean offline = runtimeManager.isOffline();  
    boolean updateSnapshots = false;

    int type = event.getType();

    if(IResourceChangeEvent.PRE_CLOSE == type || IResourceChangeEvent.PRE_DELETE == type) {
      queue(refreshQueue, new RemoveCommand(new MavenUpdateRequest((IProject) event.getResource(), //
          offline, updateSnapshots)));

    } else {
      // if (IResourceChangeEvent.POST_CHANGE == type)
      IResourceDelta delta = event.getDelta(); // workspace delta
      IResourceDelta[] projectDeltas = delta.getAffectedChildren();
      Set<IProject> removeProjects = new LinkedHashSet<IProject>();
      Set<IProject> refreshProjects = new LinkedHashSet<IProject>();
      for(int i = 0; i < projectDeltas.length; i++ ) {
        try {
          projectChanged(projectDeltas[i], removeProjects, refreshProjects);
        } catch(CoreException ex) {
          MavenLogger.log(ex);
        }
      }
      
      if(!removeProjects.isEmpty()) {
        IProject[] projects = removeProjects.toArray(new IProject[removeProjects.size()]);
        MavenUpdateRequest updateRequest = new MavenUpdateRequest(projects, offline, updateSnapshots);
        updateRequest.setForce(false);
        queue(refreshQueue, new RemoveCommand(updateRequest));
      }
      if(!refreshProjects.isEmpty()) {
        IProject[] projects = refreshProjects.toArray(new IProject[refreshProjects.size()]);
        MavenUpdateRequest updateRequest = new MavenUpdateRequest(projects, offline, updateSnapshots);
        updateRequest.setForce(false);
        queue(refreshQueue, new RefreshCommand(updateRequest));
      }
    }

    synchronized(refreshQueue) {
      if(!refreshQueue.isEmpty()) {
        schedule(1000L);
      }
    }
  }

  private void projectChanged(IResourceDelta delta, Set<IProject> removeProjects, final Set<IProject> refreshProjects)
      throws CoreException {
    final IProject project = (IProject) delta.getResource();

    for(IPath path : MavenProjectManagerImpl.METADATA_PATH) {
      if (delta.findMember(path) != null) {
        removeProjects.add(project);
        return;
      }
    }

    delta.accept(new IResourceDeltaVisitor() {
      public boolean visit(IResourceDelta delta) {
        IResource resource = delta.getResource();
        if(resource instanceof IFile && IMavenConstants.POM_FILE_NAME.equals(resource.getName())) {
          // XXX ignore output folders
          if(delta.getKind() == IResourceDelta.REMOVED 
              || delta.getKind() == IResourceDelta.ADDED
              || (delta.getKind() == IResourceDelta.CHANGED && ((delta.getFlags() & DELTA_FLAGS) != 0))) 
          {
            // XXX check for interesting resources
            refreshProjects.add(project);
          }
        }
        return true;
      }
    });
  }

  private void queue(List<Command> queue, Command command) {
    synchronized(queue) {
      queue.add(command);
    }
  }

  static final class CommandContext {

    public final MavenProjectManagerImpl manager;

    public final List<DownloadRequest> downloadRequests = new ArrayList<DownloadRequest>();

    public int sourcesCount = 0;

    /**
     * Set of <code>MavenUpdateRequest</code>
     */
    public Set<DependencyResolutionContext> updateRequests = new LinkedHashSet<DependencyResolutionContext>();

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
      DependencyResolutionContext resolutionContext = new DependencyResolutionContext(updateRequest);
      resolutionContext.forcePomFiles(context.manager.remove(updateRequest.getPomFiles(), updateRequest.isForce()));
      context.updateRequests.add(resolutionContext);
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
      context.updateRequests.add(new DependencyResolutionContext(updateRequest));
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
    private final boolean downloadSources;
    private final boolean downloadJavaDoc;

    DownloadSourcesCommand(IProject project, IPath path, ArtifactKey artifactKey, //
        boolean downloadSources, boolean downloadJavaDoc) {
      this.project = project;
      this.path = path;
      this.artifactKey = artifactKey;
      this.downloadSources = downloadSources;
      this.downloadJavaDoc = downloadJavaDoc;
    }

    void execute(CommandContext context, IProgressMonitor monitor) {
      context.downloadRequests.add(new DownloadRequest(project, path, artifactKey, downloadSources, downloadJavaDoc));
      context.sourcesCount++ ;
    }

    public String toString() {
      return "DOWNLOADSOURCES " + project.getName() + " " + path;
    }
  }

  public void preferenceChange(PreferenceChangeEvent event) {
    boolean offline = runtimeManager.isOffline();  
    boolean updateSnapshots = false;

    if (event.getSource() instanceof IProject) {
      queue(refreshQueue, new RemoveCommand(new MavenUpdateRequest(new IProject[] {(IProject) event.getSource()}, offline, updateSnapshots)));
    }
  }
}
