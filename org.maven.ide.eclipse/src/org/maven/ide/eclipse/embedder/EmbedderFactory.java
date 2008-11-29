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

import org.apache.maven.embedder.Configuration;
import org.apache.maven.embedder.ConfigurationValidationResult;
import org.apache.maven.embedder.ContainerCustomizer;
import org.apache.maven.embedder.DefaultConfiguration;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.embedder.MavenEmbedderException;
import org.apache.maven.embedder.MavenEmbedderLogger;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;



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
    Configuration configuration = new DefaultConfiguration();
    
    if(globalSettings != null) {
      configuration.setGlobalSettingsFile(new File(globalSettings));
    }
    
    if(userSettings != null) {
      configuration.setUserSettingsFile(new File(userSettings));
    }
    
    configuration.setMavenEmbedderLogger(logger);
    
    configuration.setConfigurationCustomizer(customizer);
    
    return createMavenEmbedder(configuration, null);
  }

  public static MavenEmbedder createMavenEmbedder(Configuration configuration, ClassLoader classLoader)
      throws MavenEmbedderException {
    ClassLoader orig = Thread.currentThread().getContextClassLoader();
    try {
      if(classLoader == null) {
        classLoader = MavenEmbedder.class.getClassLoader();
      }
      configuration.setClassLoader(classLoader);
      verifySettings(configuration);
      return new MavenEmbedder(configuration);
    } finally {
      Thread.currentThread().setContextClassLoader(orig);
    }
  }

  private static void verifySettings(Configuration configuration) {
    MavenEmbedderLogger logger = configuration.getMavenEmbedderLogger();
 
    File userSettingsFile = configuration.getUserSettingsFile();
    if(userSettingsFile==null) {
      userSettingsFile = MavenEmbedder.DEFAULT_USER_SETTINGS_FILE;
      configuration.setUserSettingsFile(userSettingsFile);
    }
    ConfigurationValidationResult userResult = validateConfiguration(userSettingsFile, logger);
    if(userSettingsFile.exists()) {
      if(!userResult.isValid()) {
        logger.error("Invalid user settings " + userSettingsFile.getAbsolutePath());
        if(userResult.getUserSettingsException() != null) {
          logger.error(userResult.getUserSettingsException().getMessage());
          configuration.setUserSettingsFile(null);
        }
      }
    } else {
      logger.info("User settings file does not exist " + userSettingsFile.getAbsolutePath());
      configuration.setUserSettingsFile(null);
    }
 
    File globalSettingsFile = configuration.getUserSettingsFile();
    if(globalSettingsFile != null) {
      if(globalSettingsFile.exists()) {
        ConfigurationValidationResult globalResult = validateConfiguration(globalSettingsFile, logger);
        if(!globalResult.isValid()) {
          logger.error("Invalid global settings " + globalSettingsFile.getAbsolutePath());
          if(globalResult.getUserSettingsException() != null) {
            logger.error(globalResult.getUserSettingsException().getMessage());
          }
          configuration.setGlobalSettingsFile(null);
        }
      } else {
        logger.info("Global settings file does not exist " + globalSettingsFile.getAbsolutePath());
        configuration.setGlobalSettingsFile(null);
      }
    }
  }

  
  public static ConfigurationValidationResult validateConfiguration(File file, MavenEmbedderLogger logger) {
    Configuration configuration = new DefaultConfiguration();
    configuration.setMavenEmbedderLogger(logger);
    configuration.setUserSettingsFile(file);
    return MavenEmbedder.validateConfiguration(configuration);
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

  public static MavenExecutionRequest createMavenExecutionRequest(boolean offline, boolean debug) {
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
