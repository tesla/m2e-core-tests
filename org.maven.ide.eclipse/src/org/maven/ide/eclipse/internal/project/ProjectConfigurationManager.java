/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.project;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.ui.IWorkingSet;

import org.codehaus.plexus.util.StringUtils;

import org.apache.maven.archetype.ArchetypeGenerationRequest;
import org.apache.maven.archetype.ArchetypeGenerationResult;
import org.apache.maven.archetype.catalog.Archetype;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.archetype.ArchetypeCatalogFactory.RemoteCatalogFactory;
import org.maven.ide.eclipse.archetype.ArchetypeManager;
import org.maven.ide.eclipse.core.IMavenConstants;
import org.maven.ide.eclipse.core.MavenConsole;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.embedder.IMaven;
import org.maven.ide.eclipse.embedder.IMavenConfiguration;
import org.maven.ide.eclipse.embedder.MavenModelManager;
import org.maven.ide.eclipse.project.IMavenMarkerManager;
import org.maven.ide.eclipse.project.IMavenProjectChangedListener;
import org.maven.ide.eclipse.project.IMavenProjectFacade;
import org.maven.ide.eclipse.project.IMavenProjectImportResult;
import org.maven.ide.eclipse.project.IProjectConfigurationManager;
import org.maven.ide.eclipse.project.LocalProjectScanner;
import org.maven.ide.eclipse.project.MavenProjectChangedEvent;
import org.maven.ide.eclipse.project.MavenProjectInfo;
import org.maven.ide.eclipse.project.MavenProjectManager;
import org.maven.ide.eclipse.project.MavenUpdateRequest;
import org.maven.ide.eclipse.project.ProjectImportConfiguration;
import org.maven.ide.eclipse.project.ResolverConfiguration;
import org.maven.ide.eclipse.project.configurator.AbstractProjectConfigurator;
import org.maven.ide.eclipse.project.configurator.ILifecycleMapping;
import org.maven.ide.eclipse.project.configurator.ProjectConfigurationRequest;
import org.maven.ide.eclipse.util.Util;

/**
 * import Maven projects
 * update project configuration from Maven 
 * enable Maven nature
 * create new project
 *
 * @author igor
 */
public class ProjectConfigurationManager implements IProjectConfigurationManager, IMavenProjectChangedListener {

  final MavenConsole console;

  final MavenProjectManager projectManager;

  final MavenModelManager mavenModelManager;

  final IMavenMarkerManager mavenMarkerManager;

  final IMaven maven;

  final IMavenConfiguration mavenConfiguration;

  public ProjectConfigurationManager(IMaven maven, MavenConsole console, MavenProjectManager projectManager,
      MavenModelManager mavenModelManager, IMavenMarkerManager mavenMarkerManager, IMavenConfiguration mavenConfiguration) {
    this.console = console;
    this.projectManager = projectManager;
    this.mavenModelManager = mavenModelManager;
    this.mavenMarkerManager = mavenMarkerManager;
    this.maven = maven;
    this.mavenConfiguration = mavenConfiguration;
  }

  public List<IMavenProjectImportResult> importProjects(Collection<MavenProjectInfo> projectInfos, ProjectImportConfiguration configuration, IProgressMonitor monitor) throws CoreException {
    long t1 = System.currentTimeMillis();

    SubMonitor progress = SubMonitor.convert(monitor, "Importing Maven projects", 100);

    ArrayList<IMavenProjectImportResult> result = new ArrayList<IMavenProjectImportResult>();
    ArrayList<IProject> projects = new ArrayList<IProject>();

    SubMonitor subProgress =
      SubMonitor.convert( progress.newChild( 10 ), projectInfos.size() * 100 );

    // first, create all projects with basic configuration
    for(MavenProjectInfo projectInfo : projectInfos) {
      if(monitor.isCanceled()) {
        throw new OperationCanceledException();
      }

      IProject project = create(projectInfo, configuration, subProgress.newChild(100));

      result.add(new MavenProjectImportResult(projectInfo, project));

      if (project != null) {
        projects.add(project);
        
        addToWorkingSets(project, configuration.getWorkingSets());              
      }
    }

    hideNestedProjectsFromParents(projects);
    // then configure maven for all projects
    configureNewMavenProject(projects, progress.newChild(90));

    long t2 = System.currentTimeMillis();
    console.logMessage("Project import completed " + ((t2 - t1) / 1000) + " sec");

    return result;
  }

  
  private void setHidden(IResource resource) {
    // Invoke IResource.setHidden() through reflection since it is only avaiable in Eclispe 3.4 & later
    try {
      Method m = IResource.class.getMethod("setHidden", boolean.class);
      m.invoke(resource, Boolean.TRUE);
    } catch (Exception ex) {
      MavenLogger.log("Failed to hide resource; " + resource.getLocation().toOSString(), ex);
    }
  }
  
  private void hideNestedProjectsFromParents(List<IProject> projects) {
    
    if (!MavenPlugin.getDefault().getMavenConfiguration().isHideFoldersOfNestedProjects()) {
      return;
    }
    // Prevent child project folders from showing up in parent project folders.
    
    Bundle bundle = ResourcesPlugin.getPlugin().getBundle();
    String version = (String) bundle.getHeaders().get(org.osgi.framework.Constants.BUNDLE_VERSION);
    Version currentVersion = org.osgi.framework.Version.parseVersion(version);
    Version e34Version = new Version(3,4,0);
    if (currentVersion.compareTo(e34Version) < 0) {
      return; // IResource.setHidden doesn't exist in Eclipse prior to version 3.4
    }
    HashMap<File, IProject> projectFileMap = new HashMap<File, IProject>();
    
    for (IProject project: projects) {
      projectFileMap.put(project.getLocation().toFile(), project);
    }
    for (IProject project: projects) {
      File projectFile = project.getLocation().toFile();
      IProject physicalParentProject = projectFileMap.get(projectFile.getParentFile());
      if (physicalParentProject == null) {
        continue;
      }
      IFolder folder = physicalParentProject.getFolder(projectFile.getName());
      if (folder.exists()) {
          setHidden(folder);
      }
    }
  }
  
  private void configureNewMavenProject(List<IProject> projects, IProgressMonitor monitor)
      throws CoreException {
    SubMonitor progress = SubMonitor.convert(monitor, "Configuring Maven projects", 100);

    //SubProgressMonitor sub = new SubProgressMonitor(monitor, projects.size()+1);
    
      // first, resolve maven dependencies for all projects
      MavenUpdateRequest updateRequest = new MavenUpdateRequest(mavenConfiguration.isOffline(), false);
      for (IProject project : projects) {
        updateRequest.addPomFile(project);
      }
      progress.subTask("Refreshing projects");
      projectManager.refresh(updateRequest, progress.newChild(75));

    // TODO this emits project change events, which may be premature at this point


    //Creating maven facades 
    SubMonitor subProgress = SubMonitor.convert(progress.newChild(5), projects.size() * 100);
    List<IMavenProjectFacade> facades = new ArrayList<IMavenProjectFacade>(projects.size());
    for(IProject project : projects) {
      if(progress.isCanceled()) {
        throw new OperationCanceledException();
      } 
      IMavenProjectFacade facade = projectManager.create(project, subProgress.newChild(100));
      if (facade != null) {
        facades.add(facade);
      }
    }

    //MNGECLIPSE-1028 : Sort projects by build order here, 
    //as dependent projects need to be configured before depending projects (in WTP integration for ex.)
    sortProjects(facades, progress.newChild(5));
    //Then, perform detailed project configuration
    subProgress = SubMonitor.convert(progress.newChild(15), facades.size() * 100);
    for(IMavenProjectFacade facade : facades) {
      if(progress.isCanceled()) {
        throw new OperationCanceledException();
      }
      progress.subTask("Updating configuration for "+facade.getProject().getName());
      MavenProject mavenProject = facade.getMavenProject(subProgress.newChild(5));
      MavenSession mavenSession = createMavenSession(facade, subProgress.newChild(5));
      ProjectConfigurationRequest request = new ProjectConfigurationRequest(facade, mavenProject, mavenSession, false /*updateSources*/);
      updateProjectConfiguration(request, subProgress.newChild(90));
    }
  }

  private MavenSession createMavenSession(IMavenProjectFacade facade, IProgressMonitor monitor) throws CoreException {
    MavenExecutionRequest request = projectManager.createExecutionRequest(facade.getPom(), facade.getResolverConfiguration(), monitor);
    return maven.createSession(request, facade.getMavenProject(monitor));
  }

  public void sortProjects(List<IMavenProjectFacade> facades, IProgressMonitor monitor) throws CoreException {
      HashMap<MavenProject, IMavenProjectFacade> mavenProjectToFacadeMap = new HashMap<MavenProject, IMavenProjectFacade>(facades.size());
      for(IMavenProjectFacade facade:facades) {
        mavenProjectToFacadeMap.put(facade.getMavenProject(monitor), facade);
      }
      facades.clear();
      for(MavenProject mavenProject: maven.getSortedProjects(new ArrayList<MavenProject>(mavenProjectToFacadeMap.keySet()))) {
        facades.add(mavenProjectToFacadeMap.get(mavenProject));
      }
  }

  // PlatformUI.getWorkbench().getWorkingSetManager().addToWorkingSets(project, new IWorkingSet[] {workingSet});
  private void addToWorkingSets(IProject project, IWorkingSet[] workingSets) {
    if (workingSets != null && workingSets.length > 0) {
    // IAdaptable[] elements = workingSet.adaptElements(new IAdaptable[] {project});
    // if(elements.length == 1) {
      for (IWorkingSet workingSet : workingSets) {
        IAdaptable[] oldElements = workingSet.getElements();
        IAdaptable[] newElements = new IAdaptable[oldElements.length + 1];
        System.arraycopy(oldElements, 0, newElements, 0, oldElements.length);
        newElements[oldElements.length] = project;
        
        // Eclipse 3.2 compatibility
        workingSet.setElements(Util.proxy(workingSet, A.class).adaptElements(newElements));
        // }
      }
    }
  }
  
  /**
   * A compatibility proxy stub
   */
  private static interface A {
    public IAdaptable[] adaptElements(IAdaptable[] objects);
  }

  public void updateProjectConfiguration(IProject project, ResolverConfiguration configuration, String goalToExecute, IProgressMonitor monitor) throws CoreException {
    IFile pom = project.getFile(IMavenConstants.POM_FILE_NAME);
    if (pom.isAccessible()) {
      projectManager.refresh(new MavenUpdateRequest(project, mavenConfiguration.isOffline(), false), monitor); 
      IMavenProjectFacade facade = projectManager.create(pom, false, monitor);
      if (facade != null) { // facade is null if pom.xml cannot be read
        ProjectConfigurationRequest request = new ProjectConfigurationRequest(facade, facade.getMavenProject(monitor), createMavenSession(facade, monitor), true /*updateSources*/);
        updateProjectConfiguration(request, monitor);
      }
    }
  }

  private void updateProjectConfiguration(ProjectConfigurationRequest request,
      IProgressMonitor monitor) throws CoreException {
    mavenMarkerManager.deleteMarkers(request.getProject());

    IProject project = request.getProject();
    addMavenNature(project, monitor);

    ILifecycleMapping lifecycleMapping = getLifecycleMapping(request.getMavenProjectFacade(), monitor);

    lifecycleMapping.configure(request, monitor);

  }

  public void enableMavenNature(IProject project, ResolverConfiguration configuration, IProgressMonitor monitor)
      throws CoreException {
    monitor.subTask("Enable Maven nature");
    enableBasicMavenNature(project, configuration, monitor);

    ArrayList<IProject> projects = new ArrayList<IProject>();
    projects.add(project);
    configureNewMavenProject(projects, monitor);
  }

  private void enableBasicMavenNature(IProject project, ResolverConfiguration configuration, IProgressMonitor monitor)
      throws CoreException {
    projectManager.setResolverConfiguration(project, configuration);

    // add maven nature even for projects without valid pom.xml file
    addMavenNature(project, monitor);
  }

  private void addMavenNature(IProject project, IProgressMonitor monitor) throws CoreException {
    if (!project.hasNature(IMavenConstants.NATURE_ID)) {
      IProjectDescription description = project.getDescription();
      String[] prevNatures = description.getNatureIds();
      String[] newNatures = new String[prevNatures.length + 1];
      System.arraycopy(prevNatures, 0, newNatures, 1, prevNatures.length);
      newNatures[0] = IMavenConstants.NATURE_ID;
      description.setNatureIds(newNatures);
      project.setDescription(description, monitor);
    }
  }

  public void disableMavenNature(IProject project, IProgressMonitor monitor) throws CoreException {
    monitor.subTask("Disable Maven nature");

    IMavenProjectFacade facade = projectManager.create(project, monitor);
    if(facade!=null) {
      ILifecycleMapping lifecycleMapping = getLifecycleMapping(facade, monitor);
      ProjectConfigurationRequest request = new ProjectConfigurationRequest(facade, facade.getMavenProject(monitor), createMavenSession(facade, monitor), false /*updateSources*/);
      lifecycleMapping.unconfigure(request, monitor);
    }

    project.deleteMarkers(IMavenConstants.MARKER_ID, true, IResource.DEPTH_INFINITE);

    IProjectDescription description = project.getDescription();
    ArrayList<String> newNatures = new ArrayList<String>();
    for(String natureId : description.getNatureIds()) {
      if(!IMavenConstants.NATURE_ID.equals(natureId)) {
        newNatures.add(natureId);
      }
    }
    description.setNatureIds(newNatures.toArray(new String[newNatures.size()]));
    ArrayList<ICommand> newCommands = new ArrayList<ICommand>();
    for (ICommand command : description.getBuildSpec()) {
      if (!IMavenConstants.BUILDER_ID.equals(command.getBuilderName())) {
        newCommands.add(command);
      }
    }
    description.setBuildSpec(newCommands.toArray(new ICommand[newCommands.size()]));
    project.setDescription(description, null);
  }

  // project creation

  /**
   * Creates simple Maven project
   * <p>
   * The following steps are executed in the given order:
   * <ul>
   * <li>Creates the workspace project</li>
   * <li>Adds project to working set</li>
   * <li>Creates the required folders</li>
   * <li>Creates the POM</li>
   * <li>Configures project</li>
   * </ul>
   * </p>
   */
  // XXX should use Maven plugin configurations instead of manually specifying folders
  public void createSimpleProject(IProject project, IPath location, Model model, String[] directories,
      ProjectImportConfiguration configuration, IProgressMonitor monitor) throws CoreException {
    String projectName = project.getName();
    monitor.beginTask("Creating project " + projectName, 5);

    monitor.subTask("Creating workspace project...");
    IProjectDescription description = ResourcesPlugin.getWorkspace().newProjectDescription(projectName);
    description.setLocation(location);
    project.create(description, monitor);
    project.open(monitor);
    monitor.worked(1);
    
    addToWorkingSets(project, configuration.getWorkingSets());
    monitor.worked(1);

    monitor.subTask("Creating the POM file...");
    IFile pomFile = project.getFile(IMavenConstants.POM_FILE_NAME);    
    mavenModelManager.createMavenModel(pomFile, model);
    monitor.worked(1);

    monitor.subTask("Creating project folders...");
    for(int i = 0; i < directories.length; i++ ) {
      Util.createFolder(project.getFolder(directories[i]), false);
    }
    monitor.worked(1);

    monitor.subTask("Configuring project...");
    enableMavenNature(project, configuration.getResolverConfiguration(), monitor);
    monitor.worked(1);
  }
  
  /**
   * Creates project structure using Archetype and then imports created project
   */
  public void createArchetypeProject(IProject project, IPath location, Archetype archetype, String groupId,
      String artifactId, String version, String javaPackage, Properties properties,
      ProjectImportConfiguration configuration, IProgressMonitor monitor) throws CoreException {
    monitor.beginTask("Creating project " + project.getName(), 2);

    IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
  
    monitor.subTask("Executing Archetype " + archetype.getGroupId() + ":" + archetype.getArtifactId());
    if(location == null) {
      // if the project should be created in the workspace, figure out the path
      location = workspaceRoot.getLocation();
    }

    try {
      
      
      
      Artifact artifact = resolveArchetype(archetype, monitor);
      
      ArchetypeGenerationRequest request = new ArchetypeGenerationRequest() //
          .setTransferListener(maven.createTransferListener(monitor)) //
          .setArchetypeGroupId(artifact.getGroupId()) //
          .setArchetypeArtifactId(artifact.getArtifactId()) //
          .setArchetypeVersion(artifact.getVersion()) //
          .setArchetypeRepository(archetype.getRepository()) //
          .setGroupId(groupId) //
          .setArtifactId(artifactId) //
          .setVersion(version) //
          .setPackage(javaPackage) // the model does not have a package field
          .setLocalRepository(maven.getLocalRepository()) //
          .setRemoteArtifactRepositories(maven.getArtifactRepositories(true))
          .setProperties(properties).setOutputDirectory(location.toPortableString());

      MavenSession session = maven.createSession(maven.createExecutionRequest(monitor), null);

      MavenSession oldSession = MavenPlugin.getDefault().setSession(session);

      ArchetypeGenerationResult result;
      try {
        result = getArchetyper().generateProjectFromArchetype(request);
      } finally {
        MavenPlugin.getDefault().setSession(oldSession);
      }

      Exception cause = result.getCause();
      if(cause != null) {
        String msg = "Unable to create project from archetype " + archetype.toString();
        MavenLogger.log(msg, cause);
        throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, msg, cause));
      }
      monitor.worked(1);

      // XXX Archetyper don't allow to specify project folder
      String projectFolder = location.append(artifactId).toFile().getAbsolutePath();

      LocalProjectScanner scanner = new LocalProjectScanner(workspaceRoot.getLocation().toFile(), //
          projectFolder, true, mavenModelManager, console);
      scanner.run(monitor);
      
      Set<MavenProjectInfo> projectSet = collectProjects(scanner.getProjects());
      
      importProjects(projectSet, configuration, monitor);
      
      monitor.worked(1);
    } catch (CoreException e) {
      throw e;
    } catch (InterruptedException e) {
      throw new CoreException(Status.CANCEL_STATUS);
    } catch (Exception ex) {
      throw new CoreException(new Status(IStatus.ERROR, "org.maven.ide.eclipse", "Failed to create project.", ex));
    }
  }

  /**
   * Apparently, Archetype#generateProjectFromArchetype 2.0-alpha-4 does not attempt to resolve archetype
   * from configured remote repositories. To compensate, we populate local repo with archetype pom/jar.
   */
  private Artifact resolveArchetype(Archetype a, IProgressMonitor monitor) throws CoreException {
    ArrayList<ArtifactRepository> repos = new ArrayList<ArtifactRepository>();
    repos.addAll(maven.getArtifactRepositories()); // see org.apache.maven.archetype.downloader.DefaultDownloader#download    

    //MNGECLIPSE-1399 use archetype repository too, not just the default ones
    String artifactRemoteRepository = a.getRepository();

    try {
    
      if (StringUtils.isBlank(artifactRemoteRepository)){
        
        IMavenConfiguration mavenConfiguration = MavenPlugin.getDefault().getMavenConfiguration();
        if (!mavenConfiguration.isOffline()){
          //Try to find the repository from remote catalog if needed
          final ArchetypeManager archetypeManager = MavenPlugin.getDefault().getArchetypeManager();
          RemoteCatalogFactory factory = archetypeManager.findParentCatalogFactory(a, RemoteCatalogFactory.class);
          if (factory != null) {
             //Grab the computed remote repository url
              artifactRemoteRepository = factory.getRepositoryUrl();
              a.setRepository(artifactRemoteRepository);//Hopefully will prevent further lookups for the same archetype
          }
        }
      }
  
      if (StringUtils.isNotBlank(artifactRemoteRepository)) {
        ArtifactRepository archetypeRepository = maven.createArtifactRepository("archetype", a.getRepository().trim());
        repos.add(0,archetypeRepository);//If the archetype doesn't exist locally, this will be the first remote repo to be searched.
      }
    
      maven.resolve(a.getGroupId(), a.getArtifactId(),a.getVersion(), "pom", null, repos, monitor);
      return maven.resolve(a.getGroupId(), a.getArtifactId(),a.getVersion(), "jar", null, repos, monitor);
    } catch (CoreException e) {
      StringBuilder sb = new StringBuilder();
      sb.append("Could not resolve archetype ").append(a.getGroupId()).append(':').append(a.getArtifactId()).append(':').append(a.getVersion());
      sb.append(" from any of the configured repositories.");
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, sb.toString(), e));
    }
  }

  private org.apache.maven.archetype.Archetype getArchetyper() {
    return MavenPlugin.getDefault().getArchetype();
  }

  public Set<MavenProjectInfo> collectProjects(Collection<MavenProjectInfo> projects) {

    // TODO what does this do?
    return new LinkedHashSet<MavenProjectInfo>() {
      private static final long serialVersionUID = 1L;

      public Set<MavenProjectInfo> collectProjects(Collection<MavenProjectInfo> projects) {
        for(MavenProjectInfo projectInfo : projects) {
          console.logMessage("Collecting project info " + projectInfo);
          add(projectInfo);
          collectProjects(projectInfo.getProjects());
        }
        return this;
      }
    }.collectProjects(projects);
  }

  public ISchedulingRule getRule() {
    return ResourcesPlugin.getWorkspace().getRuleFactory().buildRule();
  }

  private IProject create(MavenProjectInfo projectInfo, ProjectImportConfiguration configuration, IProgressMonitor monitor) throws CoreException {
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    IWorkspaceRoot root = workspace.getRoot();
    
    File pomFile = projectInfo.getPomFile(); 
    Model model = projectInfo.getModel();
    if(model == null) {
      model = maven.readModel(pomFile);
      projectInfo.setModel(model);
    }

    String projectName = configuration.getProjectName(model);

    File projectDir = pomFile.getParentFile();
    String projectParent = projectDir.getParentFile().getAbsolutePath();

    if (projectInfo.getBasedirRename() == MavenProjectInfo.RENAME_REQUIRED) {
      File newProject = new File(projectDir.getParent(), projectName);
      if(!projectDir.equals(newProject)) {
        boolean renamed = projectDir.renameTo(newProject);
        if(!renamed) {
          StringBuilder msg = new StringBuilder("Can't rename ");
          msg.append("Can't rename ").append(projectDir.getAbsolutePath()).append('.');
          if (newProject.exists()) {
            msg.append(" Target directory ").append(newProject.getAbsolutePath()).append(" already exists.");
          }
          throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, msg.toString(), null));
        }
        projectInfo.setPomFile(getCanonicalPomFile(newProject));
        projectDir = newProject;
      }
    } else {
      if(projectParent.equals(root.getLocation().toFile().getAbsolutePath())) {
        // immediately under workspace root, project name must match filesystem directory name
        projectName = projectDir.getName();
      }
    }

    monitor.subTask("Importing project " + projectName);

    IProject project = root.getProject(projectName);
    if(project.exists()) {
      console.logError("Project " + projectName + " already exists");
      return null;
    }

    if(projectDir.equals(root.getLocation().toFile())) {
      console.logError("Can't create project " + projectName + " at Workspace folder");
      return null;
    }

    if(projectParent.equals(root.getLocation().toFile().getAbsolutePath())) {
      project.create(monitor);
    } else {
      IProjectDescription description = workspace.newProjectDescription(projectName);
      description.setLocation(new Path(projectDir.getAbsolutePath()));
      project.create(description, monitor);
    }

    if(!project.isOpen()) {
      project.open(monitor);
    }

    ResolverConfiguration resolverConfiguration = configuration.getResolverConfiguration();
    enableBasicMavenNature(project, resolverConfiguration, monitor);

    return project;
  }

  private File getCanonicalPomFile(File projectDir) throws CoreException {
    try {
      return new File(projectDir.getCanonicalFile(), IMavenConstants.POM_FILE_NAME);
    } catch(IOException ex) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, //
          "Can't get canonical file for " + projectDir.getAbsolutePath(), null));
    }
  }

  public void mavenProjectChanged(MavenProjectChangedEvent[] events, IProgressMonitor monitor) {
    for(MavenProjectChangedEvent event : events) {
      try {
        ILifecycleMapping lifecycleMapping = getLifecycleMapping(event.getMavenProject(), monitor);
        if(lifecycleMapping != null) {
          for(AbstractProjectConfigurator configurator : lifecycleMapping.getProjectConfigurators(event.getMavenProject(), monitor)) {
            configurator.mavenProjectChanged(events, monitor);
          }
        }
      } catch (CoreException e) {
        MavenLogger.log(e);
      }
    }
  }

  public ILifecycleMapping getLifecycleMapping(IMavenProjectFacade projectFacade, IProgressMonitor monitor) throws CoreException {
    if (projectFacade==null) {
      return null;
    }

    return projectFacade.getLifecycleMapping(monitor);
  }
}
