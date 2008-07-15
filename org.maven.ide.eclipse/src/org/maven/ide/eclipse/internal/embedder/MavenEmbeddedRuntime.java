/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.embedder;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import org.eclipse.core.runtime.FileLocator;

import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.embedder.MavenRuntime;
import org.maven.ide.eclipse.embedder.MavenRuntimeManager;

/**
 * Embedded Maven runtime
 */
public class MavenEmbeddedRuntime extends MavenRuntime {

  private static final String MAVEN_MAVEN_EMBEDDER_BUNDLE_ID = "org.maven.ide.components.maven_embedder";

  private static final String MAVEN_EXECUTOR_CLASS = org.apache.maven.cli.MavenCli.class.getName();
  
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
            MavenLogger.log("Error adding classpath entry " + url.toString(), ex);
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
  
  public boolean equals(Object o) {
    return o==this;
  }
  
  public int hashCode() {
    return 1568475786;  // "EMBEDDED".hashCode() 
  }
  
  public String toString() {
    return "Embedded";
  }
  
}