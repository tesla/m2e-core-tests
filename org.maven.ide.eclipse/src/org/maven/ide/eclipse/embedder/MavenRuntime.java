/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.embedder;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.osgi.framework.Bundle;

import org.eclipse.core.runtime.FileLocator;

import org.maven.ide.eclipse.MavenPlugin;

/**
 * Maven runtime
 *
 * @author Eugene Kuleshov
 */
public abstract class MavenRuntime {
  
  public static final MavenRuntime EMBEDDED = new MavenEmbeddedRuntime();
  
  public abstract boolean isEditable();
  
  public abstract String getMainTypeName();

  public abstract String getOptions();
  
  public abstract String[] getClasspath();
  
  public abstract String getLocation();
  
  public static MavenRuntime createExternalRuntime(String location) {
    return new MavenExternalRuntime(location);
  }
  
  
  /**
   * Embedded Maven runtime
   */
  private static final class MavenEmbeddedRuntime extends MavenRuntime {
    private static final String MAVEN_EXECUTOR_CLASS = org.apache.maven.cli.MavenCli.class.getName();
    private static String[] CLASSPATH;
    
    MavenEmbeddedRuntime() {
    }
    
    public boolean isEditable() {
      return false;
    }
    
    public String getLocation() {
      return MavenRuntimeManager.EMBEDDED;
    }
    
    public String getMainTypeName() {
      return MAVEN_EXECUTOR_CLASS;
    }
    
    public String getOptions() {
      return "";
    }
    
    public String[] getClasspath() {
      if(CLASSPATH == null) {
        List cp = new ArrayList();
  
        Bundle bundle = findMavenEmbedderBundle();
        
        Enumeration entries = bundle.findEntries("/", "*", true);
        while(entries.hasMoreElements()) {
          URL url = (URL) entries.nextElement();
          String path = url.getPath();
          if(path.endsWith(".jar") || path.endsWith("bin/")) {
            try {
              cp.add(FileLocator.toFileURL(url).getFile());
            } catch(IOException ex) {
              MavenPlugin.log("Error adding classpath entry " + url.toString(), ex);
            }
          }
        }
  
        CLASSPATH = (String[]) cp.toArray(new String[cp.size()]);
      }
      return CLASSPATH;
    }
    
    private Bundle findMavenEmbedderBundle() {
      Bundle[] bundles = MavenPlugin.getDefault().getBundleContext().getBundles();
      for(int i = 0; i < bundles.length; i++ ) {
        Bundle bundle = bundles[i];
        if("org.maven.ide.components.maven_embedder".equals(bundle.getSymbolicName())) {
          return bundle;
        }
      }
  
      return null;
    }
    
    public boolean equals(Object o) {
      return o==this;
    }
    
    public String toString() {
      return "Embedded";
    }
    
  }

  /**
   * Maven external runtime using ClassWorlds launcher
   * 
   * <pre>
   * %MAVEN_JAVA_EXE% %MAVEN_OPTS% 
   *   -classpath %CLASSWORLDS_JAR% 
   *   "-Dclassworlds.conf=%M2_HOME%\bin\m2.conf" 
   *   "-Dmaven.home=%M2_HOME%" 
   *   org.codehaus.classworlds.Launcher 
   *   %MAVEN_CMD_LINE_ARGS%
   * </pre>
   */
  private static final class MavenExternalRuntime extends MavenRuntime {

    private final String location;

    MavenExternalRuntime(String location) {
      this.location = location;
    }
    
    public boolean isEditable() {
      return true;
    }
    
    public String getLocation() {
      return location;
    }
    
    public String getMainTypeName() {
      return "org.codehaus.classworlds.Launcher";
    }
    
    public String getOptions() {
      // TODO add quotes if location contains spaces
      return " -Dclassworlds.conf=" + location + File.separator + "bin" + File.separator + "m2.conf -Dmaven.home=" + location;
    }
    
    public String[] getClasspath() {
      File mavenHome = new File(location);

      File classworlds = new File(mavenHome, "core" + File.separator + "boot" + File.separator + "classworlds-1.1.jar"); // 2.0.4
      if(!classworlds.exists()) {
        classworlds = new File(mavenHome, "boot" + File.separator + "classworlds-1.1.jar");  // 2.0.7
        if(!classworlds.exists()) {
          classworlds = new File(mavenHome, "boot" + File.separator + "plexus-classworlds-1.2-alpha-12.jar"); // 2.1
        }
      }
      
      return new String[] {classworlds.getAbsolutePath()};
    }
    
    public boolean equals(Object o) {
      if(o instanceof MavenExternalRuntime) {
        return location.equals(((MavenExternalRuntime) o).location);
      }
      return false;
    }
    
    public String toString() {
      return "External" + " " + location;
    }
    
  }

}