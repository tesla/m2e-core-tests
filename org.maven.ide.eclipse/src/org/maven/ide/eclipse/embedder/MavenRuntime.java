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
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;

import org.osgi.framework.Bundle;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.codehaus.plexus.util.DirectoryScanner;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.embedder.MavenEmbedder;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.project.MavenProjectFacade;
import org.maven.ide.eclipse.project.MavenProjectManager;

/**
 * Maven runtime
 *
 * @author Eugene Kuleshov
 */
public abstract class MavenRuntime {
  
  public static final MavenRuntime EMBEDDED = new MavenEmbeddedRuntime();
  
  public static final MavenRuntime WORKSPACE = new MavenWorkspaceRuntime();
  
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
   * Embedded Maven runtime
   */
  static final class MavenEmbeddedRuntime extends MavenRuntime {
    private static final String MAVEN_EXECUTOR_CLASS = org.apache.maven.cli.MavenCli.class.getName();
    private static String[] CLASSPATH;
    
    public boolean isEditable() {
      return false;
    }
    
    public String getLocation() {
      return MavenRuntimeManager.EMBEDDED;
    }
    
    public String getSettings() {
      return null;
    }
    
    public boolean isAvailable() {
      return true;
    }
    
    public String getMainTypeName() {
      return MAVEN_EXECUTOR_CLASS;
    }
    
    public String getOptions(File tpmfolder, String[] forcedComponents) {
      return "";
    }
    
    public String[] getClasspath(String[] forcedComponents) {
      if(CLASSPATH == null) {
        List<String> cp = new ArrayList<String>();
  
        Bundle bundle = findMavenEmbedderBundle();
        
        @SuppressWarnings("unchecked")
        Enumeration<URL> entries = bundle.findEntries("/", "*", true);
        while(entries.hasMoreElements()) {
          URL url = entries.nextElement();
          String path = url.getPath();
          if(path.endsWith(".jar") || path.endsWith("bin/")) {
            try {
              cp.add(FileLocator.toFileURL(url).getFile());
            } catch(IOException ex) {
              MavenPlugin.log("Error adding classpath entry " + url.toString(), ex);
            }
          }
        }
  
        CLASSPATH = cp.toArray(new String[cp.size()]);
      }
      if (forcedComponents != null && forcedComponents.length > 0) {
        String[] cp = new String[CLASSPATH.length + forcedComponents.length];
        System.arraycopy(forcedComponents, 0, cp, 0, forcedComponents.length);
        System.arraycopy(CLASSPATH, 0, cp, forcedComponents.length, CLASSPATH.length);
        return cp;
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
   * Maven 2.1-SNAPSHOT runtime loaded from the Eclipse Workspace
   */
  static class MavenWorkspaceRuntime extends MavenRuntime {

    public String getLocation() {
      return MavenRuntimeManager.WORKSPACE;
    }
    
    public String getSettings() {
      return null;
    }

    public String getMainTypeName() {
      return "org.apache.maven.cli.MavenCli";
    }

    public String getOptions(File tpmfolder, String[] forcedComponents) {
      return "";
    }

    public boolean isEditable() {
      return false;
    }

    public boolean isAvailable() {
      MavenProjectManager projectManager = MavenPlugin.getDefault().getMavenProjectManager();
      return projectManager.getMavenProject("org.apache.maven", "maven-distribution", "2.1-SNAPSHOT") != null;
    }

    public String[] getClasspath(String[] forcedComponents) {
      List<String> cp = new ArrayList<String>();

      MavenPlugin mavenPlugin = MavenPlugin.getDefault();
      MavenProjectManager projectManager = mavenPlugin.getMavenProjectManager();
      MavenProjectFacade maven = projectManager.getMavenProject("org.apache.maven", "maven-distribution", "2.1-SNAPSHOT");
      if (maven != null) {
        if (forcedComponents != null) {
          for (int i = 0; i < forcedComponents.length; i++) {
            cp.add(forcedComponents[i]);
          }
        }

        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        MavenEmbedderManager embedderManager = mavenPlugin.getMavenEmbedderManager();
        MavenEmbedder embedder = embedderManager.getWorkspaceEmbedder();

        @SuppressWarnings("unchecked")
        Set<Artifact> artifacts = maven.getMavenProject().getArtifacts();
        for (Artifact artifact : artifacts) {
          if (Artifact.SCOPE_TEST.equals(artifact.getScope())) {
            continue;
          }

          MavenProjectFacade facade = projectManager.getMavenProject(artifact);

          File file = null;
          if (facade != null) {
            IFolder output = root.getFolder(facade.getOutputLocation());
            if (output.isAccessible()) {
              file = output.getLocation().toFile();
            }
          } else {
            try {
              embedder.resolve(artifact, Collections.EMPTY_LIST, embedder.getLocalRepository());
            } catch(ArtifactResolutionException ex) {
            } catch(ArtifactNotFoundException ex) {
            }
            file = artifact.getFile();
          }
          
          if (file != null) {
            cp.add(file.getAbsolutePath());
          }
        }
      }
      
      return cp.toArray(new String[cp.size()]);
    }

    public String toString() {
      return "Workspace";
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
          tpmfolder.mkdirs();
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
          throw new CoreException(new Status(IStatus.ERROR, MavenPlugin.PLUGIN_ID, ex.getMessage(), ex));
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
          "core/boot/classworlds-*.jar", // 2.0.4
          "boot/classworlds-*.jar", // 2.0.7
          "boot/plexus-classworlds-*.jar", // 2.1 as of 2008-03-27
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
    
    public String toString() {
      return "External" + " " + location;
    }
    
  }

}
