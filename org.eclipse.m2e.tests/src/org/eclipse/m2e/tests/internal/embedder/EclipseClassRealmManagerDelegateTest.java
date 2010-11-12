/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.tests.internal.embedder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.apache.maven.classrealm.ClassRealmConstituent;
import org.apache.maven.classrealm.ClassRealmManagerDelegate;
import org.apache.maven.classrealm.ClassRealmRequest;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.embedder.EclipseClassRealmManagerDelegate;
import org.eclipse.m2e.core.internal.embedder.MavenImpl;


public class EclipseClassRealmManagerDelegateTest extends TestCase {

  private EclipseClassRealmManagerDelegate delegate;

  protected void setUp() throws Exception {
    super.setUp();

    delegate = (EclipseClassRealmManagerDelegate) ((MavenImpl) MavenPlugin.getDefault().getMaven())
        .getPlexusContainer().lookup(ClassRealmManagerDelegate.class);
  }

  public void testRealmSetup() throws Exception {
    DummyRealm realm = new DummyRealm();

    final List<ClassRealmConstituent> constituents = new ArrayList<ClassRealmConstituent>();

    ClassRealmRequest request = new ClassRealmRequest() {

      public List<ClassRealmConstituent> getConstituents() {
        return constituents;
      }

      public List<String> getImports() {
        return new ArrayList<String>();
      }

      public ClassLoader getParent() {
        return null;
      }

      public RealmType getType() {
        return RealmType.Plugin;
      }

    };

    delegate.setupRealm(realm, request);

    assertFalse(realm.imports.toString(), realm.imports.contains("org.sonatype.plexus.build.incremental"));

    constituents.add(new DummyConstituent("0.0.3"));

    delegate.setupRealm(realm, request);

    assertTrue(realm.imports.toString(), realm.imports.contains("org.sonatype.plexus.build.incremental"));

    realm.imports.clear();
    constituents.clear();
    constituents.add(new DummyConstituent("999.0"));

    delegate.setupRealm(realm, request);

    assertFalse(realm.imports.toString(), realm.imports.contains("org.sonatype.plexus.build.incremental"));
  }

  static class DummyRealm extends ClassRealm {

    public List<String> imports = new ArrayList<String>();

    public DummyRealm() {
      super(null, "test-" + System.currentTimeMillis(), null);
    }

    public void importFrom(ClassLoader classLoader, String packageName) {
      imports.add(packageName);
    }

  }

  static class DummyConstituent implements ClassRealmConstituent {

    private String version;

    public DummyConstituent(String version) {
      this.version = version;
    }

    public String getGroupId() {
      return "org.sonatype.plexus";
    }

    public String getArtifactId() {
      return "plexus-build-api";
    }

    public String getType() {
      return "jar";
    }

    public String getClassifier() {
      return "";
    }

    public String getVersion() {
      return version;
    }

    public File getFile() {
      return new File("");
    }

  }

}
