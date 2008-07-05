/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.embedder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;


import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.codehaus.plexus.util.DirectoryScanner;


import org.maven.ide.eclipse.MavenPlugin;

/**
 * Maven runtime
 *
 * @author Eugene Kuleshov
 */
public abstract class MavenRuntime {
  
  public abstract boolean isEditable();
  
  public abstract String getMainTypeName();

  public abstract String getOptions(File tpmfolder, String[] forcedComponents) throws CoreException;

  public abstract String[] getClasspath(String[] forcedComponents);
  
  public abstract String getLocation();

  public abstract String getSettings();
  
  public abstract boolean isAvailable();
  
  public static MavenRuntime createExternalRuntime(String location) {
    return new MavenExternalRuntime(location);
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

    public boolean isAvailable() {
      return new File(location, "bin").exists();
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

    public String getOptions(File tpmfolder, String[] forcedComponents) throws CoreException {
      String m2conf = location + File.separator + "bin" + File.separator + "m2.conf";
      if (forcedComponents != null && forcedComponents.length > 0) {
        try {
          boolean created = tpmfolder.mkdirs();
          if(!created) {
            throw new CoreException(new Status(IStatus.ERROR, MavenPlugin.PLUGIN_ID, -1,
                "Can't create temporary folder " + tpmfolder, null));
          }
          m2conf = new File(tpmfolder, "m2.conf").getCanonicalPath();
          BufferedWriter out = new BufferedWriter(new FileWriter(m2conf));
          try {
            out.write("main is org.apache.maven.cli.MavenCli from plexus.core\n");
            // we always set maven.home
  //          out.write("set maven.home default ${user.home}/m2\n");
            out.write("[plexus.core]\n");
            for (int i = 0; i < forcedComponents.length; i++) {
              out.write("load " + forcedComponents[i] + "\n");
            }
            out.write("load ${maven.home}/lib/*.jar\n");
          } finally {
            out.close();
          }
        } catch (IOException ex) {
          throw new CoreException(new Status(IStatus.ERROR, MavenPlugin.PLUGIN_ID, -1, ex.getMessage(), ex));
        }
      }
      StringBuffer sb = new StringBuffer();
      sb.append(" ").append(quote("-Dclassworlds.conf=" + m2conf));
      sb.append(" ").append(quote("-Dmaven.home=" + location));
      return sb.toString();
    }

    private String quote(String string) {
      return string.indexOf(' ')>-1 ? "\"" + string + "\"" : string;
    }

    public String[] getClasspath(String[] forcedComponents) {
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

      if (includedFiles.length != 1) {
        // XXX show error dialog and fail launch
        return new String[0];
      }

      return new String[] {new File(mavenHome, includedFiles[0]).getAbsolutePath()};
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
    
  }

}
