/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.tests.internal.embedder;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import org.codehaus.plexus.classworlds.realm.ClassRealm;

import org.apache.maven.classrealm.ClassRealmConstituent;
import org.apache.maven.classrealm.ClassRealmManagerDelegate;
import org.apache.maven.classrealm.ClassRealmRequest;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.embedder.EclipseClassRealmManagerDelegate;
import org.eclipse.m2e.core.internal.embedder.MavenImpl;


public class EclipseClassRealmManagerDelegateTest {

  private EclipseClassRealmManagerDelegate delegate;

  @Before
  public void setUp() throws Exception {
    delegate = (EclipseClassRealmManagerDelegate) ((MavenImpl) MavenPlugin.getMaven()).getPlexusContainer().lookup(
        ClassRealmManagerDelegate.class, EclipseClassRealmManagerDelegate.ROLE_HINT);
  }

  @Test
  public void testRealmSetup() throws Exception {

    List<String> realmImports = new ArrayList<>();
    ClassRealm realm = new ClassRealm(null, "test-" + System.currentTimeMillis(), null) {
      @Override
      public void importFrom(ClassLoader classLoader, String packageName) {
        realmImports.add(packageName);
      }
    };

    List<ClassRealmConstituent> constituents = new ArrayList<>();
    ClassRealmRequest request = Mockito.mock(ClassRealmRequest.class);
    Mockito.when(request.getConstituents()).thenReturn(constituents);

    delegate.setupRealm(realm, request);

    assertFalse(realmImports.toString(), realmImports.contains("org.sonatype.plexus.build.incremental"));

    constituents.add(new DummyConstituent("0.0.3"));

    delegate.setupRealm(realm, request);

    assertTrue(realmImports.toString(), realmImports.contains("org.sonatype.plexus.build.incremental"));

    realmImports.clear();
    constituents.clear();
    constituents.add(new DummyConstituent("999.0"));

    delegate.setupRealm(realm, request);

    assertFalse(realmImports.toString(), realmImports.contains("org.sonatype.plexus.build.incremental"));
  }


  private static class DummyConstituent implements ClassRealmConstituent {

    private String version;

    public DummyConstituent(String version) {
      this.version = version;
    }

    @Override
    public String getGroupId() {
      return "org.sonatype.plexus";
    }

    @Override
    public String getArtifactId() {
      return "plexus-build-api";
    }

    @Override
    public String getType() {
      return "jar";
    }

    @Override
    public String getClassifier() {
      return "";
    }

    @Override
    public String getVersion() {
      return version;
    }

    @Override
    public File getFile() {
      return new File("");
    }
  }
}
