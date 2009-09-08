/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.embedder;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;

import org.codehaus.plexus.MutablePlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.classworlds.realm.NoSuchRealmException;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.converters.ConfigurationConverter;
import org.codehaus.plexus.component.configurator.converters.lookup.ConverterLookup;
import org.codehaus.plexus.component.configurator.converters.lookup.DefaultConverterLookup;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import org.apache.maven.Maven;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.embedder.MavenEmbedderException;
import org.apache.maven.embedder.execution.MavenExecutionRequestPopulator;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.apache.maven.model.Repository;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.io.ModelReader;
import org.apache.maven.model.io.ModelWriter;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.PluginManagerException;
import org.apache.maven.plugin.PluginParameterExpressionEvaluator;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingResult;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.settings.MavenSettingsBuilder;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.SettingsUtils;
import org.apache.maven.settings.validation.SettingsValidationResult;
import org.apache.maven.wagon.events.TransferListener;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.core.IMavenConstants;
import org.maven.ide.eclipse.core.MavenConsole;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.embedder.IMaven;
import org.maven.ide.eclipse.embedder.IMavenConfiguration;
import org.maven.ide.eclipse.embedder.IMavenConfigurationChangeListener;
import org.maven.ide.eclipse.embedder.ISettingsChangeListener;
import org.maven.ide.eclipse.embedder.MavenConfigurationChangeEvent;
import org.maven.ide.eclipse.internal.preferences.MavenPreferenceConstants;


public class MavenImpl implements IMaven, IMavenConfigurationChangeListener {

  private final PlexusContainer plexus;

  private final Maven maven;

  private final ProjectBuilder projectBuilder;

  private final ModelReader modelReader;

  private final ModelWriter modelWriter;

  private final RepositorySystem repositorySystem;

  private final MavenSettingsBuilder settingsBuilder;

  private final IMavenConfiguration mavenConfiguration;

  private final MavenExecutionRequestPopulator populator;

  private final BuildPluginManager pluginManager;

  private final LifecycleExecutor lifecycleExecutor;

  private final ConverterLookup converterLookup = new DefaultConverterLookup();
  
  private final MavenConsole console;

  private final ArrayList<ISettingsChangeListener> settingsListeners = new ArrayList<ISettingsChangeListener>();

  private final ArrayList<TransferListener> transferListeners = new ArrayList<TransferListener>();

  public MavenImpl(PlexusContainer plexus, IMavenConfiguration mavenConfiguration, MavenConsole console) throws CoreException {
    this.plexus = plexus;
    try {
      this.maven = plexus.lookup(Maven.class);
      this.projectBuilder = plexus.lookup(ProjectBuilder.class);
      this.modelReader = plexus.lookup(ModelReader.class);
      this.modelWriter = plexus.lookup(ModelWriter.class);
      this.repositorySystem = plexus.lookup(RepositorySystem.class);
      this.settingsBuilder = plexus.lookup(MavenSettingsBuilder.class);
      this.populator = plexus.lookup(MavenExecutionRequestPopulator.class);
      this.pluginManager = plexus.lookup(BuildPluginManager.class);
      this.lifecycleExecutor = plexus.lookup(LifecycleExecutor.class);
    } catch(ComponentLookupException ex) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1,
          "Could not lookup required component", ex));
    }

    this.console = console;
    this.mavenConfiguration = mavenConfiguration;
    mavenConfiguration.addConfigurationChangeListener(this);
  }

  public MavenExecutionRequest createExecutionRequest(IProgressMonitor monitor) throws CoreException {
    MavenExecutionRequest request = new DefaultMavenExecutionRequest();
    if(mavenConfiguration.getGlobalSettingsFile() != null) {
      request.setGlobalSettingsFile(new File(mavenConfiguration.getGlobalSettingsFile()));
    }
    if(mavenConfiguration.getUserSettingsFile() != null) {
      request.setUserSettingsFile(new File(mavenConfiguration.getUserSettingsFile()));
    }

    try {
      populator.populateFromSettings(request, getSettings());
    } catch(MavenEmbedderException ex) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1,
          "Could not create maven execution request", ex));
    }

    ArtifactRepository localRepository = getLocalRepository();
    request.setLocalRepository(localRepository);
    request.setLocalRepositoryPath(localRepository.getBasedir());
    request.setOffline(mavenConfiguration.isOffline());

    // logging
    request.setTransferListener(createTransferListener(monitor));

    request.getUserProperties().put("m2e.version", MavenPlugin.getVersion());

    // the right way to disable snapshot update
    // request.setUpdateSnapshots(false);
    return request;
  }

  private String getLocalRepositoryPath() throws CoreException {
    return getSettings().getLocalRepository();
  }

  public MavenExecutionResult execute(MavenExecutionRequest request, IProgressMonitor monitor) {
    // XXX is there a way to set per-request log level?

    MavenExecutionResult result;
    try {
      populator.populateDefaults(request);
      result = maven.execute(request);
    } catch(MavenEmbedderException ex) {
      ex.printStackTrace();
      result = new DefaultMavenExecutionResult();
      result.addException(ex);
    } catch (Exception e){
      e.printStackTrace();
      result = new DefaultMavenExecutionResult();
      result.addException(e);
    }
    if(result.getExceptions() != null && result.getExceptions().size() > 0){
      for(Exception e :result.getExceptions()){
        e.printStackTrace();
      }
    }
    return result;
  }

  public MavenSession createSession(MavenExecutionRequest request, MavenProject project) {
    MavenExecutionResult result = new DefaultMavenExecutionResult();
    MavenSession mavenSession = new MavenSession(plexus, request, result);
    mavenSession.setProjects(Collections.singletonList(project));
    return mavenSession;
  }

  public void execute(MavenSession session, MojoExecution execution, IProgressMonitor monitor) {
    try {
      pluginManager.executeMojo(session, execution);
    } catch(Exception ex) {
      session.getResult().addException(ex);
    }
  }

  public MavenExecutionPlan calculateExecutionPlan(MavenExecutionRequest request, MavenProject project,
      IProgressMonitor monitor) throws CoreException {
    MavenSession session = createSession(request, project);
    try {
      List<String> goals = request.getGoals();
      return lifecycleExecutor.calculateExecutionPlan(session, goals.toArray(new String[goals.size()]));
    } catch(Exception ex) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1,
          "Could not calculate build plan", ex));
    }
  }

  public ArtifactRepository getLocalRepository() throws CoreException {
    try {
      String localRepositoryPath = getLocalRepositoryPath();
      if(localRepositoryPath != null) {
        return repositorySystem.createLocalRepository(new File(localRepositoryPath));
      }
      return repositorySystem.createLocalRepository(RepositorySystem.defaultUserLocalRepository);
    } catch(InvalidRepositoryException ex) {
      // can't happen
      throw new IllegalStateException(ex);
    }
  }

  public Settings getSettings() throws CoreException {
    // MUST NOT use createRequest!

    MavenExecutionRequest request = new DefaultMavenExecutionRequest();
    if(mavenConfiguration.getGlobalSettingsFile() != null) {
      request.setGlobalSettingsFile(new File(mavenConfiguration.getGlobalSettingsFile()));
    }
    if(mavenConfiguration.getUserSettingsFile() != null) {
      request.setUserSettingsFile(new File(mavenConfiguration.getUserSettingsFile()));
    }
    try {
      return settingsBuilder.buildSettings(request);
    } catch(IOException ex) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, "Could not read settings.xml",
          ex));
    } catch(XmlPullParserException ex) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, "Could not read settings.xml",
          ex));
    }
  }

  public Settings buildSettings(String globalSettings, String userSettings) throws CoreException {
    MavenExecutionRequest request = createExecutionRequest(null);
    request.setGlobalSettingsFile(globalSettings != null ? new File(globalSettings) : null);
    request.setUserSettingsFile(userSettings != null ? new File(userSettings) : null);
    try {
      return settingsBuilder.buildSettings(request);
    } catch(IOException ex) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, "Could not read settings.xml",
          ex));
    } catch(XmlPullParserException ex) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, "Could not read settings.xml",
          ex));
    }
  }

  public SettingsValidationResult validateSettings(String settings) {
    SettingsValidationResult result = new SettingsValidationResult();
    if(settings != null) {
      File settingsFile = new File(settings);
      if(settingsFile.canRead()) {
        @SuppressWarnings("unchecked")
        List<String> messages = settingsBuilder.validateSettings(settingsFile).getMessages();
        for(String message : messages) {
          result.addMessage(message);
        }
      } else {
        result.addMessage("Can not read settings file " + settings);
      }
    }

    return result;
  }

  public void reloadSettings() throws CoreException {
    // TODO do something more meaningful
    Settings settings = getSettings();
    for (ISettingsChangeListener listener : settingsListeners) {
      try {
        listener.settingsChanged(settings);
      } catch (CoreException e) {
        MavenLogger.log(e);
      }
    }
  }

  public void mavenConfigutationChange(MavenConfigurationChangeEvent event) throws CoreException {
    if(MavenConfigurationChangeEvent.P_USER_SETTINGS_FILE.equals(event.getKey())
        || MavenPreferenceConstants.P_GLOBAL_SETTINGS_FILE.equals(event.getKey())) {
      reloadSettings();
    }
  }

  public Model readModel(InputStream in) throws CoreException {
    try {
      return modelReader.read(in, null);
    } catch(IOException e) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, "Could not read pom.xml", e));
    }
  }

  public Model readModel(File pomFile) throws CoreException {
    try {
      BufferedInputStream is = new BufferedInputStream(new FileInputStream(pomFile));
      try {
        return readModel(is);
      } finally {
        IOUtil.close(is);
      }
    } catch(IOException e) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, "Could not read pom.xml", e));
    }
  }

  public void writeModel(Model model, OutputStream out) throws CoreException {
    try {
      modelWriter.write(out, null, model);
    } catch(IOException ex) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, "Could not write pom.xml", ex));
    }
  }

  public MavenProject readProject(File pomFile, IProgressMonitor monitor) throws CoreException {
    try {
      MavenExecutionRequest request = createExecutionRequest(monitor);
      populator.populateDefaults(request);
      ProjectBuildingRequest configuration = request.getProjectBuildingRequest();
      configuration.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
      return projectBuilder.build(pomFile, configuration).getProject();
    } catch(ProjectBuildingException ex) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, "Could not read maven project",
          ex));
    } catch(MavenEmbedderException ex) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, "Could not read maven project",
          ex));
    }
  }

  public MavenExecutionResult readProject(MavenExecutionRequest request, IProgressMonitor monitor) {
    File pomFile = request.getPom();
    MavenExecutionResult result = new DefaultMavenExecutionResult();
    try {
      populator.populateDefaults(request);
      ProjectBuildingRequest configuration = request.getProjectBuildingRequest();
      configuration.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
      ProjectBuildingResult projectBuildingResult = projectBuilder.build(pomFile, configuration);
      result.setProject(projectBuildingResult.getProject());
      result.setArtifactResolutionResult(projectBuildingResult.getArtifactResolutionResult());
    } catch(ProjectBuildingException ex) {
      return result.addException(ex);
    } catch(MavenEmbedderException ex) {
      return result.addException(ex);
    }
    return result;
  }

  public Artifact resolve(String groupId, String artifactId, String version, String type, String classifier,
      List<ArtifactRepository> remoteRepositories, IProgressMonitor monitor) throws CoreException {
    Artifact artifact = repositorySystem.createArtifactWithClassifier(groupId, artifactId, version, type, classifier);

    ArtifactResolutionRequest request = new ArtifactResolutionRequest();
    ArtifactRepository localRepository = getLocalRepository();
    request.setLocalRepository(localRepository);
    if(remoteRepositories != null) {
      request.setRemoteRepositories(remoteRepositories);
    } else {
      try {
        MavenExecutionRequest er = new DefaultMavenExecutionRequest();
        populator.populateDefaults(er);
        request.setRemoteRepositories(er.getRemoteRepositories());
      } catch(MavenEmbedderException e) {
        // we've tried
        request.setRemoteRepositories(new ArrayList<ArtifactRepository>());
      }
    }
    request.setArtifact(artifact);

    ArtifactResolutionResult result = repositorySystem.resolve(request);

    setLastUpdated(localRepository, remoteRepositories, artifact);

    if(result.hasExceptions()) {
      ArrayList<IStatus> members = new ArrayList<IStatus>();
      for(Exception e : result.getExceptions()) {
        members.add(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, e.getMessage(), e));
      }
      IStatus[] newMembers = members.toArray(new IStatus[members.size()]);
      throw new CoreException(new MultiStatus(IMavenConstants.PLUGIN_ID, -1, newMembers, "Could not resolve artifact",
          null));
    }

    return artifact;
  }

  private void setLastUpdated(ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories,
      Artifact artifact) throws CoreException {

    Properties lastUpdated = loadLastUpdated(localRepository, artifact);

    String timestamp = Long.toString(System.currentTimeMillis());

    for (ArtifactRepository repository : remoteRepositories) {
      lastUpdated.setProperty(getLastUpdatedKey(repository, artifact), timestamp);
    }

    File lastUpdatedFile = getLastUpdatedFile(localRepository, artifact);
    try {
      lastUpdatedFile.getParentFile().mkdirs();
      BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(lastUpdatedFile));
      try {
        lastUpdated.store(os, null);
      } finally {
        IOUtil.close(os);
      }
    } catch (IOException ex) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, "Could not write artifact lastUpdated status",
          ex));
    }
  }

  /**
   * This is a temporary implementation that only works for artifacts resolved
   * using #resolve.
   */
  public boolean isUnavailable(String groupId, String artifactId, String version, String type, String classifier,
      List<ArtifactRepository> remoteRepositories) throws CoreException {
    Artifact artifact = repositorySystem.createArtifactWithClassifier(groupId, artifactId, version, type, classifier);

    ArtifactRepository localRepository = getLocalRepository();

    if (localRepository.find(artifact).isResolved()) {
      // artifact is available locally
      return false;
    }

    if (remoteRepositories == null || remoteRepositories.isEmpty()) {
      // no remote repositories
      return true;
    }

    // now is the hard part
    Properties lastUpdated = loadLastUpdated(localRepository, artifact);

    for (ArtifactRepository repository : remoteRepositories) {
      String timestamp = lastUpdated.getProperty(getLastUpdatedKey(repository, artifact));
      if (timestamp == null) {
        // availability of the artifact from this repository has not been checked yet 
        return false;
      }
    }

    // artifact is not available locally and all remote repositories have been checked in the past
    return true;
  }

  private String getLastUpdatedKey(ArtifactRepository repository, Artifact artifact) {
    StringBuilder key = new StringBuilder();

    // repository part
    key.append(repository.getId());
    if (repository.getAuthentication() != null) {
      key.append('|').append(repository.getAuthentication().getUsername());
    }
    key.append('|').append(repository.getUrl());
    
    // artifact part
    key.append('|').append(artifact.getClassifier());

    return key.toString();
  }

  private Properties loadLastUpdated(ArtifactRepository localRepository, Artifact artifact) throws CoreException {
    Properties lastUpdated = new Properties();
    File lastUpdatedFile = getLastUpdatedFile(localRepository, artifact);
    try {
      BufferedInputStream is = new BufferedInputStream(new FileInputStream(lastUpdatedFile));
      try {
        lastUpdated.load(is);
      } finally {
        IOUtil.close(is);
      }
    } catch (FileNotFoundException ex) {
      // that's okay
    } catch (IOException ex) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, "Could not read artifact lastUpdated status",
          ex));
    }
    return lastUpdated;
  }

  private File getLastUpdatedFile(ArtifactRepository localRepository, Artifact artifact) {
    return new File(localRepository.getBasedir(), basePathOf(localRepository, artifact) + "/" + "m2e-lastUpdated.properties");
  }

  private static final char PATH_SEPARATOR = '/';

  private static final char GROUP_SEPARATOR = '.';

  private String basePathOf(ArtifactRepository repository, Artifact artifact) {
    StringBuilder path = new StringBuilder(128);

    path.append(formatAsDirectory(artifact.getGroupId())).append(PATH_SEPARATOR);
    path.append(artifact.getArtifactId()).append(PATH_SEPARATOR);
    path.append(artifact.getBaseVersion()).append(PATH_SEPARATOR);

    return path.toString();
  }  

  private String formatAsDirectory(String directory) {
    return directory.replace(GROUP_SEPARATOR, PATH_SEPARATOR);
  }
  
  public <T> T getMojoParameterValue(MavenSession session, MojoExecution mojoExecution, String parameter,
      Class<T> asType) throws CoreException {

    try {
      MojoDescriptor mojoDescriptor = mojoExecution.getMojoDescriptor();

      ClassRealm pluginRealm = pluginManager.getPluginRealm(session, mojoDescriptor.getPluginDescriptor());

      ExpressionEvaluator expressionEvaluator = new PluginParameterExpressionEvaluator(session, mojoExecution);

      ConfigurationConverter typeConverter = converterLookup.lookupConverterForType(asType);

      Xpp3Dom dom = mojoExecution.getConfiguration();

      if(dom == null) {
        return null;
      }

      PlexusConfiguration pomConfiguration = new XmlPlexusConfiguration(dom);

      PlexusConfiguration configuration = pomConfiguration.getChild(parameter);

      if(configuration == null) {
        return null;
      }

      Object value = typeConverter.fromConfiguration(converterLookup, configuration, asType, mojoDescriptor
          .getImplementationClass(), pluginRealm, expressionEvaluator, null);
      return asType.cast(value);
    } catch(ComponentConfigurationException ex) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1,
          "Could not get mojo execution paramater value", ex));
    } catch(PluginManagerException ex) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1,
          "Could not get mojo execution paramater value", ex));
    }
  }

  @SuppressWarnings("deprecation")
  public void xxxRemoveExtensionsRealm(MavenProject project) {
    ClassRealm realm = project.getClassRealm();
    if(realm != null && realm != plexus.getContainerRealm()) {
      ClassWorld world = ((MutablePlexusContainer) plexus).getClassWorld();
      try {
        world.disposeRealm(realm.getId());
      } catch(NoSuchRealmException ex) {
        MavenLogger.log("Could not dispose of project extensions class realm", ex);
      }
    }
  }

  public List<ArtifactRepository> getArtifactRepositories() throws CoreException {
    ArrayList<ArtifactRepository> repositories = new ArrayList<ArtifactRepository>();
    for(Profile profile : getActiveProfiles()) {
      addArtifactRepositories(repositories, profile.getRepositories());
    }

    addDefaultRepository(repositories);

    return repositories;
  }

  private void addDefaultRepository(ArrayList<ArtifactRepository> repositories) {
    for (ArtifactRepository repository : repositories) {
      if (RepositorySystem.DEFAULT_REMOTE_REPO_ID.equals(repository.getId())) {
        return;
      }
    }
    try {
      repositories.add(0, repositorySystem.createDefaultRemoteRepository());
    } catch(InvalidRepositoryException ex) {
      MavenLogger.log("Unexpected exception", ex);
    }
  }

  private void addArtifactRepositories(ArrayList<ArtifactRepository> artifactRepositories, List<Repository> repositories) throws CoreException {
    for(Repository repository : repositories) {
      try {
        ArtifactRepository artifactRepository = repositorySystem.buildArtifactRepository(repository);
        if(!artifactRepositories.contains(artifactRepository)) {
          artifactRepositories.add(artifactRepository);
        }
      } catch(InvalidRepositoryException ex) {
        throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, "Could not read settings.xml",
            ex));
      }
    }
  }

  private List<Profile> getActiveProfiles() throws CoreException {
    Settings settings = getSettings();
    @SuppressWarnings("unchecked")
    Map<String,org.apache.maven.settings.Profile> profiles = settings.getProfilesAsMap();
    ArrayList<Profile> activeProfiles = new ArrayList<Profile>();
    for (String profileId : settings.getActiveProfiles()) {
      org.apache.maven.settings.Profile settingsProfile = profiles.get(profileId);
      if (settingsProfile != null) {
        Profile profile = SettingsUtils.convertFromSettingsProfile(settingsProfile);
        activeProfiles.add(profile);
      }
    }
    return activeProfiles;
  }

  public List<ArtifactRepository> getPluginArtifactRepository() throws CoreException {
    ArrayList<ArtifactRepository> repositories = new ArrayList<ArtifactRepository>();
    for(Profile profile : getActiveProfiles()) {
      addArtifactRepositories(repositories, profile.getPluginRepositories());
    }
    addDefaultRepository(repositories);

    return repositories;
  }

  public Mirror getMirror(ArtifactRepository repo) throws CoreException {
    MavenExecutionRequest request = createExecutionRequest(new NullProgressMonitor());
    populateDefaults(request);
    return repositorySystem.getMirror(repo, request.getMirrors());
  };

  public void populateDefaults(MavenExecutionRequest request) throws CoreException {
    try {
      populator.populateDefaults(request);
    } catch(MavenEmbedderException ex) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1,
          "Could not read Maven configuration", ex));
    }
  }

  public List<Mirror> getMirrors() throws CoreException {
    MavenExecutionRequest request = createExecutionRequest(null);
    populateDefaults(request);
    return request.getMirrors();
  }

  public void addSettingsChangeListener(ISettingsChangeListener listener) {
    settingsListeners.add(listener);
  }
  
  public void removeSettingsChangeListener(ISettingsChangeListener listener) {
    settingsListeners.remove(listener);
  }

  @SuppressWarnings("deprecation")
  public void addTransferListener(TransferListener listener) {
    transferListeners.add(listener);
  }

  @SuppressWarnings("deprecation")
  public void removeTransferListener(TransferListener listener) {
    transferListeners.remove(listener);
  }

  @SuppressWarnings("deprecation")
  public CompoundTransferListener createTransferListener(IProgressMonitor monitor) {
    ArrayList<TransferListener> listeners = new ArrayList<TransferListener>();
    listeners.add(new TransferListenerAdapter(monitor, console));
    listeners.addAll(transferListeners);
    return new CompoundTransferListener(listeners);
  }

  /** for testing purposes */
  public PlexusContainer getPlexusContainer() {
    return plexus;
  }
}
