/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.embedder;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.validation.SettingsValidationResult;
import org.apache.maven.wagon.events.TransferListener;

/**
 * Entry point for all Maven functionality in m2e. Note that this component does not directly support workspace artifact
 * resolution.
 * 
 * @author igor
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IMaven {

  /**
   * Creates new Maven execution request. This method is not long running, but created execution request is configured
   * to report progress to provided progress monitor. Monitor can be null.
   */
  public MavenExecutionRequest createExecutionRequest(IProgressMonitor monitor) throws CoreException;

  // POM Model read/write operations

  public Model readModel(InputStream in) throws CoreException;

  public Model readModel(File pomFile) throws CoreException;

  public void writeModel(Model model, OutputStream out) throws CoreException;

  // artifact resolution

  public Artifact resolve(String groupId, String artifactId, String version, String type, String classifier,
      List<ArtifactRepository> artifactRepositories, IProgressMonitor monitor) throws CoreException;

  /**
   * Returns true if the artifact does NOT exist in the local repository and
   * known to be UNavailable from all specified repositories.
   */
  public boolean isUnavailable(String groupId, String artifactId, String version, String type, String classifier,
      List<ArtifactRepository> repositories) throws CoreException;

  // read MavenProject

  public MavenProject readProject(File pomFile, IProgressMonitor monitor) throws CoreException;

  public MavenExecutionResult readProject(MavenExecutionRequest request, IProgressMonitor monitor);

  // execution

  public MavenExecutionResult execute(MavenExecutionRequest request, IProgressMonitor monitor);

  public MavenSession createSession(MavenExecutionRequest request, MavenProject project);

  public void execute(MavenSession session, MojoExecution execution, IProgressMonitor monitor);

  public MavenExecutionPlan calculateExecutionPlan(MavenExecutionRequest request, MavenProject project,
      IProgressMonitor monitor) throws CoreException;

  public <T> T getMojoParameterValue(MavenSession session, MojoExecution mojoExecution, String parameter,
      Class<T> asType) throws CoreException;

  // configuration

  /**
   * TODO should we expose Settings or provide access to servers and proxies instead?
   */
  public Settings getSettings() throws CoreException;

  public ArtifactRepository getLocalRepository() throws CoreException;

  public void populateDefaults(MavenExecutionRequest request) throws CoreException;

  /**
   * Convenience method, fully equivalent to getArtifactRepositories(true)
   */
  public List<ArtifactRepository> getArtifactRepositories() throws CoreException;

  /**
   * Returns list of remote artifact repositories configured in settings.xml. Only profiles active by default are
   * considered when calculating the list.
   * 
   * If injectSettings=true, mirrors, authentication and proxy info will be injected.
   * 
   * If injectSettings=false, raw repository definition will be used. 
   */
  public List<ArtifactRepository> getArtifactRepositories(boolean injectSettings) throws CoreException;

  public List<ArtifactRepository> getPluginArtifactRepositories() throws CoreException;

  public List<ArtifactRepository> getPluginArtifactRepositories(boolean injectSettings) throws CoreException;

  public Settings buildSettings(String globalSettings, String userSettings) throws CoreException;

  public SettingsValidationResult validateSettings(String settings);

  public List<Mirror> getMirrors() throws CoreException;

  public Mirror getMirror(ArtifactRepository repo) throws CoreException;

  public void addSettingsChangeListener(ISettingsChangeListener listener);

  public void removeSettingsChangeListener(ISettingsChangeListener listener);

  public void reloadSettings() throws CoreException;

  /**
   * Temporary solution/workaround for http://jira.codehaus.org/browse/MNG-4194. Extensions realm is created each time
   * MavenProject instance is built, so we have to remove unused extensions realms to avoid OOME.
   */
  @Deprecated
  public void xxxRemoveExtensionsRealm(MavenProject project);

  /** @deprecated IMaven API should not expose wagon.TransferListener */
  public void addTransferListener(TransferListener listener);

  /** @deprecated IMaven API should not expose wagon.TransferListener */
  public void removeTransferListener(TransferListener listener);

  /** @deprecated IMaven API should not expose wagon.TransferListener */
  public TransferListener createTransferListener(IProgressMonitor monitor);
}
