/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.embedder;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.classworlds.realm.ClassRealm;

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.classrealm.ClassRealmConstituent;
import org.apache.maven.classrealm.ClassRealmManagerDelegate;
import org.apache.maven.classrealm.ClassRealmRequest;


/**
 * EclipseArtifactFilterManager
 * 
 * @author igor
 */
public class EclipseClassRealmManagerDelegate implements ClassRealmManagerDelegate {

  private PlexusContainer plexus;

  private final ArtifactVersion currentBuildApiVersion;

  public EclipseClassRealmManagerDelegate() {
    Properties props = new Properties();
    InputStream is = getClass().getResourceAsStream("/org/sonatype/plexus/build/incremental/version.properties");
    if(is != null) {
      try {
        props.load(is);
      } catch(IOException e) {
        e.printStackTrace();
      }
    }
    currentBuildApiVersion = new DefaultArtifactVersion(props.getProperty("api.version", "0.0.5"));
  }

  public void setupRealm(ClassRealm realm, ClassRealmRequest request) {
    if(supportsBuildApi(request.getConstituents())) {
      ClassRealm coreRealm = plexus.getContainerRealm();

      realm.importFrom(coreRealm, "org.codehaus.plexus.util.AbstractScanner");
      realm.importFrom(coreRealm, "org.codehaus.plexus.util.Scanner");

      realm.importFrom(coreRealm, "org.sonatype.plexus.build.incremental");
    }
  }

  private boolean supportsBuildApi(List<ClassRealmConstituent> constituents) {
    for(ClassRealmConstituent constituent : constituents) {
      if("org.sonatype.plexus".equals(constituent.getGroupId())
          && "plexus-build-api".equals(constituent.getArtifactId())) {
        ArtifactVersion version = new DefaultArtifactVersion(constituent.getVersion());
        return currentBuildApiVersion.compareTo(version) >= 0;
      }
    }
    return false;
  }

}
