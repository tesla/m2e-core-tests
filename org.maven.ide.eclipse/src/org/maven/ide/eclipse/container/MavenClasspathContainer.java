/*******************************************************************************
 * Copyright (c) 2007, 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.container;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;

import org.maven.ide.eclipse.MavenPlugin;


/**
 * MavenClasspathContainer
 * 
 * @author Eugene Kuleshov
 */
public class MavenClasspathContainer implements IClasspathContainer {
  private final IClasspathEntry[] entries;
  private final IPath path;

  
  public MavenClasspathContainer() {
    this.path = new Path(MavenPlugin.CONTAINER_ID);
    this.entries = new IClasspathEntry[0];
  }
  
  public MavenClasspathContainer(IPath path, IClasspathEntry[] entries) {
    this.path = path;
    this.entries = entries;
  }
  
  public MavenClasspathContainer(IPath path, Set entrySet) {
    this(path, (IClasspathEntry[]) entrySet.toArray(new IClasspathEntry[entrySet.size()]));
  }

  public synchronized IClasspathEntry[] getClasspathEntries() {
    return entries;
  }

  public String getDescription() {
    return "Maven Dependencies";  // TODO move to properties
  }

  public int getKind() {
    return IClasspathContainer.K_APPLICATION;
  }

  public IPath getPath() {
    return path; 
  }

  public static String getJavaDocUrl(String fileName) {
    try {
      URL fileUrl = new File(fileName).toURL();
      return "jar:"+fileUrl.toExternalForm()+"!/"+MavenClasspathContainer.getJavaDocPathInArchive(fileName);
    } catch(MalformedURLException ex) {
      return null;
    }
  }
  
  private static String getJavaDocPathInArchive(String name) {
    long l1 = System.currentTimeMillis();
    ZipFile jarFile = null;
    try {
      jarFile = new ZipFile(name);
      String marker = "package-list";
      for(Enumeration en = jarFile.entries(); en.hasMoreElements();) {
        ZipEntry entry = (ZipEntry) en.nextElement();
        String entryName = entry.getName();
        if(entryName.endsWith(marker)) {
          return entry.getName().substring(0, entryName.length()-marker.length());
        }
      }
    } catch(IOException ex) {
      // ignore
    } finally {
      long l2 = System.currentTimeMillis();
      MavenPlugin.getDefault().getConsole().logMessage("Scanned javadoc " + name + " " + (l2-l1)/1000f);
      try {
        if(jarFile!=null) jarFile.close();
      } catch(IOException ex) {
        //
      }
    }
    
    return "";
  }
  
}

