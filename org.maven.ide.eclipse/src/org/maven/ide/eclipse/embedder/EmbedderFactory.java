/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.embedder;

import java.io.File;

import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.ComponentDescriptor;

import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.embedder.Configuration;
import org.apache.maven.embedder.ConfigurationValidationResult;
import org.apache.maven.embedder.ContainerCustomizer;
import org.apache.maven.embedder.DefaultConfiguration;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.embedder.MavenEmbedderException;
import org.apache.maven.embedder.MavenEmbedderLogger;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;

import org.maven.ide.eclipse.internal.project.EclipseArtifactResolver;


/**
 * Embedder factory responsible for creating MavenEmbedder instances. 
 * <br><br>
 * 
 * <i>This class should NOT have non-Maven dependencies.</i>
 * 
 * @author Eugene Kuleshov
 */
public class EmbedderFactory {

  public static MavenEmbedder createMavenEmbedder(ContainerCustomizer customizer, MavenEmbedderLogger logger,
      String globalSettings, String userSettings) throws MavenEmbedderException {
    ClassLoader orig = Thread.currentThread().getContextClassLoader();
    ClassLoader loader = MavenEmbedder.class.getClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(loader);
      return createMavenEmbedder(customizer, logger, globalSettings, userSettings, loader);
    } finally {
      Thread.currentThread().setContextClassLoader(orig);
    }
  }

  public static MavenEmbedder createMavenEmbedder(ContainerCustomizer customizer, MavenEmbedderLogger logger,
      String globalSettings, String userSettings, ClassLoader loader) throws MavenEmbedderException {
    Configuration configuration = new DefaultConfiguration();

    configuration.setMavenEmbedderLogger(logger);
    configuration.setClassLoader(loader);
    configuration.setConfigurationCustomizer(customizer);

    File userSettingsFile;
    if(userSettings!=null && userSettings.length()>0) {
      userSettingsFile = new File(userSettings);
    } else {
      userSettingsFile = MavenEmbedder.DEFAULT_USER_SETTINGS_FILE;
    }
    ConfigurationValidationResult userResult = validateConfiguration(userSettingsFile, logger);
    if(userSettingsFile.exists()) {
      if(userResult.isValid()) {
        configuration.setUserSettingsFile(userSettingsFile);
      } else {
        logger.error("Invalid user settings " + userSettingsFile);
        if(userResult.getUserSettingsException() != null) {
          logger.error(userResult.getUserSettingsException().getMessage());
        }
      }
    } else {
      logger.info("User settings file does not exist " + userSettingsFile);
    }

    if(globalSettings != null && globalSettings.length() > 0) {
      File globalSettingsFile = new File(globalSettings);
      if(globalSettingsFile.exists()) {
        ConfigurationValidationResult globalResult = validateConfiguration(globalSettingsFile, logger);
        if(globalResult.isValid()) {
          configuration.setGlobalSettingsFile(globalSettingsFile);
        } else {
          logger.error("Invalid global settings " + globalSettings);
          if(globalResult.getUserSettingsException() != null) {
            logger.error(globalResult.getUserSettingsException().getMessage());
          }
        }
      } else {
        logger.info("Global settings file does not exist " + userSettingsFile);
      }
    }

    return new MavenEmbedder(configuration);
  }

  public static ConfigurationValidationResult validateConfiguration(File file, MavenEmbedderLogger logger) {
    Configuration configuration = new DefaultConfiguration();
    configuration.setMavenEmbedderLogger(logger);
    configuration.setUserSettingsFile(file);
    return MavenEmbedder.validateConfiguration(configuration);
  }

  public static ContainerCustomizer createWorkspaceCustomizer() {
    return new ContainerCustomizer() {
      public void customize(PlexusContainer container) {
        ComponentDescriptor resolverDescriptor = container.getComponentDescriptor(ArtifactResolver.ROLE);
        resolverDescriptor.setImplementation(EclipseArtifactResolver.class.getName());

        // desc = plexusContainer.getComponentDescriptor(ArtifactFactory.ROLE);
        // desc.setImplementation(org.maven.ide.eclipse.embedder.EclipseArtifactFactory.class.getName());

        // Used for building hierarchy of dependencies
        // desc = container.getComponentDescriptor(ResolutionListener.ROLE);
        // if(desc == null) {
        //   desc = new ComponentDescriptor();
        //   desc.setRole(ResolutionListener.ROLE);
        //   container.addComponentDescriptor(desc);
        // }
        // desc.setImplementation(EclipseResolutionListener.class.getName());

        // Custom artifact resolver for resolving artifacts from Eclipse Worspace
//        if(resolveWorkspaceProjects) {
//          ComponentDescriptor resolverDescriptor = container.getComponentDescriptor(ArtifactResolver.ROLE);
//          // ComponentRequirement requirement = new ComponentRequirement();
//          // requirement.setRole(ResolutionListener.ROLE);
//          // desc.addRequirement(requirement);
//          resolverDescriptor.setImplementation(EclipseArtifactResolver.class.getName());
//        }
        
//          desc = container.getComponentDescriptor(WagonManager.ROLE);
//          desc.setImplementation(EclipseWagonManager.class.getName());
      }
    };
  }

  public static ContainerCustomizer createExecutionCustomizer() {
    return new ContainerCustomizer() {
      public void customize(PlexusContainer plexusContainer) {
//          ComponentDescriptor desc = plexusContainer.getComponentDescriptor(LifecycleExecutor.ROLE);
//          desc.setImplementation(org.maven.ide.eclipse.embedder.EclipseLifecycleExecutor.class.getName());
//          try {
//            PlexusConfiguration oldConf = desc.getConfiguration();
//            XmlPlexusConfiguration conf = new XmlPlexusConfiguration(oldConf.getName());
//            copyConfig(oldConf, conf);
//            desc.setConfiguration(conf);
//          } catch(PlexusConfigurationException ex) {
//            // XXX log error
//          }
      }

//        private void copyConfig(PlexusConfiguration old, XmlPlexusConfiguration conf)
//            throws PlexusConfigurationException {
//          conf.setValue(old.getValue());
//          String[] attrNames = old.getAttributeNames();
//          if(attrNames != null && attrNames.length > 0) {
//            for(int i = 0; i < attrNames.length; i++ ) {
//              conf.setAttribute(attrNames[i], old.getAttribute(attrNames[i]));
//            }
//          }
//          if("lifecycle".equals(conf.getName())) {
//            conf.setAttribute("implementation", "org.apache.maven.lifecycle.Lifecycle");
//          }
//          for(int i = 0; i < old.getChildCount(); i++ ) {
//            PlexusConfiguration oldChild = old.getChild(i);
//            XmlPlexusConfiguration newChild = new XmlPlexusConfiguration(oldChild.getName());
//            conf.addChild(newChild);
//            copyConfig(oldChild, newChild);
//          }
//        }
    };
  }

  public static MavenExecutionRequest createMavenExecutionRequest(MavenEmbedder embedder, boolean offline, boolean debug) {
    DefaultMavenExecutionRequest request = new DefaultMavenExecutionRequest();

    request.setOffline(offline);
    request.setUseReactor(false);
    request.setRecursive(true);

    if(debug) {
      request.setShowErrors(true);
      request.setLoggingLevel(MavenExecutionRequest.LOGGING_LEVEL_DEBUG);
    } else {
      request.setShowErrors(false);
      request.setLoggingLevel(MavenExecutionRequest.LOGGING_LEVEL_INFO);
    }

    return request;
  }

}
