/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.index;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import org.apache.maven.artifact.versioning.ArtifactVersion;


public class IndexedArtifact {

  public static final Comparator<IndexedArtifactFile> FILE_INFO_COMPARATOR = new Comparator<IndexedArtifactFile>() {

    @SuppressWarnings("unchecked")
    public int compare(IndexedArtifactFile f1, IndexedArtifactFile f2) {
      ArtifactVersion v1 = f1.getArtifactVersion();
      ArtifactVersion v2 = f2.getArtifactVersion();
      int r = -v1.compareTo(v2);
      if(r!=0) {
        return r;
      }

      String c1 = f1.classifier;
      String c2 = f2.classifier;
      if(c1 == null) {
        return c2 == null ? 0 : -1;
      } 
      if(c2 == null) {
        return 1;
      }
      return c1.compareTo(c2);
    }
    
  };

  public final String group;

  public final String artifact;

  private final String packageName;

  public final String className;

  public final String packaging;
  
  /**
   * Set<IndexedArtifactFile>
   */
  public final Set<IndexedArtifactFile> files = new TreeSet<IndexedArtifactFile>(FILE_INFO_COMPARATOR);

  public IndexedArtifact(String group, String artifact, String packageName, String className, String packaging) {
    this.group = group;
    this.artifact = artifact;
    this.packageName = packageName;
    this.className = className;
    this.packaging = packaging;
  }
  
  public void addFile(IndexedArtifactFile indexedArtifactFile) {
    files.add(indexedArtifactFile);
  }
  public String getPackageName(){
    if(packageName.startsWith(".") && packageName.length()>1){ 
      return packageName.substring(1);
    } else {
      return packageName;
    }
  }
  public String toString() {
    StringBuffer sb = new StringBuffer("\n" + className + "  " + packageName + "  " + group + " : " + artifact /*+ "\n"*/);
//    for(Iterator it = files.iterator(); it.hasNext();) {
//      IndexedArtifactFile f = (IndexedArtifactFile) it.next();
//      sb.append("  " + f.version + "  " + f.fname + "\n");
//    }
    return sb.toString();
  }

}
