/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.embedder;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;

import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.embedder.IMavenLauncherConfiguration;
import org.maven.ide.eclipse.embedder.MavenRuntime;
import org.maven.ide.eclipse.embedder.MavenRuntimeManager;

/**
 * Embedded Maven runtime
 * 
 * @author Eugene Kuleshov
 * @author Igor Fedorenko
 */
public class MavenEmbeddedRuntime implements MavenRuntime {

  private static final String MAVEN_MAVEN_EMBEDDER_BUNDLE_ID = "org.maven.ide.components.maven_embedder";

  private static final String MAVEN_EXECUTOR_CLASS = org.apache.maven.cli.MavenCli.class.getName();

  private static final String PLEXUS_CLASSWORLD_NAME = "plexus.core";

  private static String[] LAUNCHER_CLASSPATH;
  private static String[] CLASSPATH;

  private BundleContext bundleContext;
  
  public MavenEmbeddedRuntime(BundleContext bundleContext) {
    this.bundleContext = bundleContext;
  }
  
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

  public void createLauncherConfiguration(IMavenLauncherConfiguration collector, IProgressMonitor monitor) throws CoreException {
    collector.setMainType(MAVEN_EXECUTOR_CLASS, PLEXUS_CLASSWORLD_NAME);
    
    initClasspath(findMavenEmbedderBundle());
    
    collector.addRealm(IMavenLauncherConfiguration.LAUNCHER_REALM);
    for(String entry : LAUNCHER_CLASSPATH) {
      collector.addArchiveEntry(entry);
    }

    collector.addRealm(PLEXUS_CLASSWORLD_NAME);
    for(String entry : CLASSPATH) {
      collector.addArchiveEntry(entry);
    }
 }

  private static synchronized void initClasspath(Bundle bundle) {
    if(CLASSPATH == null) {
      List<String> cp = new ArrayList<String>();
      List<String> lcp = new ArrayList<String>();

      @SuppressWarnings("unchecked")
      Enumeration<URL> entries = bundle.findEntries("/", "*", true);
      while(entries.hasMoreElements()) {
        URL url = entries.nextElement();
        String path = url.getPath();
        if(path.endsWith(".jar") || path.endsWith("bin/")) {
          try {
            String file = FileLocator.toFileURL(url).getFile();
            if (file.contains("plexus-classworlds")) {
              lcp.add(file);
            } else {
              cp.add(file);
            }
          } catch(IOException ex) {
            MavenLogger.log("Error adding classpath entry " + url.toString(), ex);
          }
        }
      }

      CLASSPATH = cp.toArray(new String[cp.size()]);
      LAUNCHER_CLASSPATH = lcp.toArray(new String[lcp.size()]);
    }
  }

  private Bundle findMavenEmbedderBundle() {
    Bundle bundle = null;
    Bundle[] bundles = bundleContext.getBundles();
    for(int i = 0; i < bundles.length; i++ ) {
      if(MAVEN_MAVEN_EMBEDDER_BUNDLE_ID.equals(bundles[i].getSymbolicName())) {
        bundle = bundles[i];
        break;
      }
    }
    return bundle;
  }

  public String toString() {
    Bundle embedder = Platform.getBundle("org.maven.ide.components.maven_embedder");

    StringBuilder sb = new StringBuilder();
    sb.append("Embedded (").append(getVersion());
    if (embedder != null) {
      String version = (String) embedder.getHeaders().get(Constants.BUNDLE_VERSION);
      sb.append('/').append(version);
    }
    sb.append(')');

    return  sb.toString();
  }

  public String getVersion() {
    return "3.0-SNAPSHOT"; // TODO may as well discover
  }

}