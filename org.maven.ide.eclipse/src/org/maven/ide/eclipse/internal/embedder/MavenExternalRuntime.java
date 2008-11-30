/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.embedder;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.Properties;

import copy.org.codehaus.plexus.classworlds.launcher.ConfigurationException;
import copy.org.codehaus.plexus.classworlds.launcher.ConfigurationHandler;
import copy.org.codehaus.plexus.classworlds.launcher.ConfigurationParser;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.codehaus.plexus.util.DirectoryScanner;

import org.maven.ide.eclipse.core.IMavenConstants;
import org.maven.ide.eclipse.embedder.IMavenLauncherConfiguration;
import org.maven.ide.eclipse.embedder.MavenRuntime;

/**
 * Maven external runtime using ClassWorlds launcher
 * 
 * @author Eugene Kuleshov
 * @author Igor Fedorenko
 * 
 */
public class MavenExternalRuntime implements MavenRuntime {

  private static final String PROPERTY_MAVEN_HOME = "maven.home";

  private final String location;
  

  public MavenExternalRuntime(String location) {
    this.location = location;
  }
  
  public boolean isEditable() {
    return true;
  }

  public boolean isAvailable() {
    return new File(location, "bin").exists() && getLauncherClasspath() != null;
  }
  
  public String getLocation() {
    return location;
  }
  
  public String getSettings() {
    return location + File.separator + "conf" + File.separator + "settings.xml";
  }
  
  public String getMainTypeName() {
    return "org.codehaus.classworlds.Launcher";
  }

  private File getLauncherConfigurationFile() {
    return new File(location, "bin/m2.conf");
  }

  public void createLauncherConfiguration(final IMavenLauncherConfiguration collector, IProgressMonitor monitor) throws CoreException {
    
    collector.addRealm(IMavenLauncherConfiguration.LAUNCHER_REALM);
    collector.addArchiveEntry(getLauncherClasspath());
    
    ConfigurationHandler handler = new ConfigurationHandler() {
      public void addImportFrom(String relamName, String importSpec) throws ConfigurationException {
        throw new ConfigurationException("Unsupported m2.conf element");
      }
      public void addLoadFile(File file) {
        try {
          collector.addArchiveEntry(file.getAbsolutePath());
        } catch(CoreException ex) {
          throw new ExceptionWrapper(ex);
        }
      }
      public void addLoadURL(URL url) {
        try {
          collector.addArchiveEntry(url.toExternalForm());
        } catch(CoreException ex) {
          throw new ExceptionWrapper(ex);
        }
      }
      public void addRealm(String realmName) {
        collector.addRealm(realmName);
      }
      public void setAppMain(String mainClassName, String mainRealmName) {
        collector.setMainType(mainClassName, mainRealmName);
      }
    };

    Properties properties = new Properties();
    properties.put(PROPERTY_MAVEN_HOME, location);

    ConfigurationParser parser = new ConfigurationParser(handler, properties);
    
    try {
      FileInputStream is = new FileInputStream(getLauncherConfigurationFile());
      try {
        parser.parse(is);
      } finally {
        is.close();
      }
    } catch (Exception e) {
      if (e instanceof ExceptionWrapper && e.getCause() instanceof CoreException) {
        throw (CoreException) e.getCause();
      }
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, "Can't parse m2.conf", e));
    }

    // XXX show error dialog and fail launch
  }

  public boolean equals(Object o) {
    if(o instanceof MavenExternalRuntime) {
      return location.equals(((MavenExternalRuntime) o).location);
    }
    return false;
  }
  
  public int hashCode() {
    return location.hashCode();
  }

  public String toString() {
    return "External" + " " + location;
  }

  private static class ExceptionWrapper extends RuntimeException {
    private static final long serialVersionUID = 8815818826909815028L;
    public ExceptionWrapper(Exception cause) {
      super(cause);
    }
  }

  private String getLauncherClasspath() {
    File mavenHome = new File(location);
    DirectoryScanner ds = new DirectoryScanner();
    ds.setBasedir(mavenHome);
    ds.setIncludes(new String[] {
        "core/boot/classworlds*.jar", // 2.0.4
        "boot/classworlds*.jar", // 2.0.7
        "boot/plexus-classworlds*.jar", // 2.1 as of 2008-03-27
    });
    ds.scan();
    String[] includedFiles = ds.getIncludedFiles();

    if (includedFiles.length == 1) {
      return new File(mavenHome, includedFiles[0]).getAbsolutePath();
    }

    return null;
  }
}