/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.embedder;

import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.classworlds.realm.ClassRealm;

import org.apache.maven.classrealm.ClassRealmManagerDelegate;


/**
 * EclipseArtifactFilterManager
 * 
 * @author igor
 */
public class EclipseClassRealmManagerDelegate implements ClassRealmManagerDelegate {

  private PlexusContainer plexus;

  public void setupRealm(ClassRealm realm) {
    ClassRealm coreRealm = plexus.getContainerRealm();

    realm.importFrom( coreRealm, "org.codehaus.plexus.util.AbstractScanner" );
    realm.importFrom( coreRealm, "org.codehaus.plexus.util.Scanner" );

    realm.importFrom( coreRealm, "org.sonatype.plexus.build.incremental" );
  }

}
