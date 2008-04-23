/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.project.configurator;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;

import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.model.Build;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.project.AbstractProjectConfigurator;
import org.maven.ide.eclipse.project.MavenProjectFacade;
import org.maven.ide.eclipse.project.MavenRunnable;
import org.maven.ide.eclipse.project.ResolverConfiguration;


/**
 * Generic project configurator that using Maven plugins
 * 
 * @author Eugene Kuleshov
 * @see AbstractProjectConfigurator
 */
public class MavenProjectConfigurator extends AbstractProjectConfigurator {

  String pluginKey;

  List goals;

  public void configure(MavenEmbedder embedder, MavenProjectFacade facade, IProgressMonitor monitor) {
    if(pluginKey == null || goals == null) {
      return;
    }

    MavenProject mavenProject = facade.getMavenProject();
    Build build = mavenProject.getBuild();
    if(build != null) {
      Map pluginMap = build.getPluginsAsMap();
      Plugin plugin = (Plugin) pluginMap.get(pluginKey);
      if(plugin != null) {

        IFile pomFile = facade.getPom();
        ResolverConfiguration resolverConfiguration = facade.getResolverConfiguration();
        MavenPlugin.getDefault().getMavenProjectManager().execute(embedder, pomFile, resolverConfiguration, //
            new MavenRunnable() {
              public MavenExecutionResult execute(MavenEmbedder embedder, MavenExecutionRequest request) {
                request.setGoals(goals);
                return embedder.execute(request);
              }
            }, monitor);
      }
    }
  }

  public String getPluginKey() {
    return this.pluginKey;
  }

  public List getGoals() {
    return this.goals;
  }

  public void setInitializationData(IConfigurationElement config, String propertyName, Object data) {
    super.setInitializationData(config, propertyName, data);

    Pattern pattern = Pattern.compile("(.+?)\\:(.+?)\\|(.+)");
    String params = (String) data;
    if(params != null) {
      Matcher matcher = pattern.matcher(params);
      if(matcher.find() && matcher.groupCount() == 3) {
        pluginKey = matcher.group(1) + ":" + matcher.group(2);
        goals = Arrays.asList(matcher.group(3).split("\\|"));
        return;
      }
    }
    MavenPlugin.log("Unable to parse configuration for project configurator " + getId() + "; " + data, null);
  }

}
