/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.tests.discovery;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import junit.framework.TestCase;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.discovery.AbstractCatalogSource;
import org.eclipse.equinox.internal.p2.discovery.Catalog;
import org.eclipse.equinox.internal.p2.discovery.DiscoveryCore;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogItem;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.internal.Workbench;

import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;
import org.eclipse.m2e.internal.discovery.MavenDiscovery;
import org.eclipse.m2e.internal.discovery.wizards.MavenCatalogConfiguration;
import org.eclipse.m2e.internal.discovery.wizards.MavenCatalogViewer;


public class MavenDiscoveryTest extends TestCase implements IShellProvider {
  private static final String TYCHO_PACKAGE_TYPE = "eclipse-plugin";

  private static final String ONE_ID = "com.sonatype.m2e.lifecycle.one";

  private static final String TWO_ID = "com.sonatype.m2e.lifecycle.two";

  private static final String THREE_ID = "com.sonatype.m2e.lifecycle.three";

  private static final String FOUR_ID = "com.sonatype.m2e.lifecycle.four";

  private Catalog catalog;

  private Shell shell;

  private MavenCatalogConfiguration configuration;

  @Override
  public void setUp() throws Exception {
    super.setUp();

    catalog = new Catalog();
    catalog.setEnvironment(DiscoveryCore.createEnvironment());
    catalog.setVerifyUpdateSiteAvailability(false);
    catalog.getDiscoveryStrategies().add(new TestM2EBundleStrategy());

    // Build the list of tags to show in the Wizard header
    catalog.setTags(Collections.singletonList(MavenDiscovery.APPLICABLE_TAG));

    // Create configuration for the catalog
    configuration = new MavenCatalogConfiguration();
    configuration.setShowTagFilter(true);
    configuration.setSelectedTags(Collections.singletonList(MavenDiscovery.APPLICABLE_TAG));
    configuration.setShowInstalledFilter(false);
    configuration.setSelectedPackagingTypes(Collections.<String> emptyList());
    configuration.setSelectedMojos(Collections.<MojoExecutionKey> emptyList());
    configuration.setSelectedLifecycleIds(Collections.<String> emptyList());
    configuration.setSelectedConfigurators(Collections.<String> emptyList());

    shell = new Shell(Workbench.getInstance().getDisplay());
  }

  public void tearDown() throws Exception {
    try {
      shell.dispose();
      shell = null;
    } finally {
      super.tearDown();
    }
  }

  @Test
  public void testCatalogUnselected() {
    updateMavenCatalog();

    for(CatalogItem ci : catalog.getItems()) {
      assertFalse("CatalogItem not selected", ci.isSelected());
      assertFalse("CatalogItem not tagged Applicable", ci.hasTag(MavenDiscovery.APPLICABLE_TAG));
    }
  }

  @Test
  public void testCatalogSelectedByPackaging() {
    configuration.setSelectedPackagingTypes(Collections.singleton(TYCHO_PACKAGE_TYPE));
    updateMavenCatalog();

    assertSelected(ONE_ID);
    assertNotSelected(TWO_ID);
    assertNotSelected(THREE_ID);
    assertNotSelected(FOUR_ID);
  }

  @Test
  public void testCatalogSelectedByMojoExecution() {
    configuration.setSelectedMojos(Collections.singleton(getMojoExecution("maven-osgi-compiler-plugin",
        "org.sonatype.tycho", "0.9.0", "compile", "")));
    updateMavenCatalog();

    assertSelected(ONE_ID);
    assertNotSelected(TWO_ID);
    assertNotSelected(THREE_ID);
    assertNotSelected(FOUR_ID);
  }

  @Test
  public void testCatalogSelectedByConfiguratorId() {
    configuration.setSelectedConfigurators(Collections.singleton("a.configurator-id"));
    updateMavenCatalog();

    assertNotSelected(ONE_ID);
    assertSelected(TWO_ID);
    assertNotSelected(THREE_ID);
    assertNotSelected(FOUR_ID);
  }

  @Test
  public void testCatalogSelectedByLifecycleId() {
    configuration.setSelectedLifecycleIds(Collections.singleton("unknown-or-missing"));
    updateMavenCatalog();

    assertNotSelected(ONE_ID);
    assertNotSelected(TWO_ID);
    assertNotSelected(THREE_ID);
    assertSelected(FOUR_ID);
  }

  @Test
  public void testMultipleSelections() {
    configuration.setSelectedPackagingTypes(Collections.singleton(TYCHO_PACKAGE_TYPE));
    configuration.setSelectedMojos(Collections.singleton(getMojoExecution("maven-osgi-compiler-plugin",
        "org.sonatype.tycho", "0.9.0", "compile", "")));
    configuration.setSelectedConfigurators(Collections.singleton("a.configurator-id"));
    configuration.setSelectedLifecycleIds(Collections.singleton("unknown-or-missing"));
    updateMavenCatalog();

    assertSelected(ONE_ID);
    assertSelected(TWO_ID);
    assertNotSelected(THREE_ID);
    assertSelected(FOUR_ID);
  }

  @Test
  public void testNotMatchPluginSelection() {
    configuration.setSelectedMojos(Collections.singleton(getMojoExecution("maven-compiler-plugin",
        "org.apache.maven.plugins", "2.0", "compile", "")));
    updateMavenCatalog();

    assertNotSelected(ONE_ID);
    assertNotSelected(TWO_ID);
    assertNotSelected(THREE_ID);
    assertNotSelected(FOUR_ID);
  }

  private void updateMavenCatalog() {
    MavenCatalogViewer mcv = new MavenCatalogViewer(catalog, this, new RunnableContext(), configuration);
    mcv.createControl(shell);
    mcv.updateCatalog();
  }

  private CatalogItem getCatalogItem(String id) {
    for(CatalogItem ci : catalog.getItems()) {
      if(ci.getId().equals(id))
        return ci;
    }
    fail("CatalogItem " + id + " not found in catalog");
    return null;
  }

  private static class RunnableContext implements IRunnableContext {
    public RunnableContext() {
    }

    public void run(boolean fork, boolean cancelable, IRunnableWithProgress runnable) throws InvocationTargetException,
        InterruptedException {
      runnable.run(new NullProgressMonitor());
    }
  }

  public Shell getShell() {
    return shell;
  }

  private void assertSelected(String catalogId) {
    assertTrue("CatalogItem " + catalogId + "should be selected", getCatalogItem(catalogId).isSelected());
    assertTrue("CatalogItem " + catalogId + "should be tagged Applicable",
        getCatalogItem(catalogId).hasTag(MavenDiscovery.APPLICABLE_TAG));
  }

  private void assertNotSelected(String catalogId) {
    assertFalse("CatalogItem " + catalogId + " should not be selected", getCatalogItem(catalogId).isSelected());
    assertFalse("CatalogItem " + catalogId + "  should not be tagged Applicable",
        getCatalogItem(catalogId).hasTag(MavenDiscovery.APPLICABLE_TAG));
  }

  private static MojoExecutionKey getMojoExecution(String artifactId, String groupId, String version, String goal,
      String executionId) {
    return new MojoExecutionKey(groupId, artifactId, version, goal, "compile", executionId);
  }

  public void testCatalogItemWithoutMappingMetadata() throws Exception {
    final File emptydir = new File("target/emptydir").getCanonicalFile();
    emptydir.mkdirs();
    assertEquals(0, emptydir.list().length);

    CatalogItem item = new CatalogItem();
    item.setSource(new AbstractCatalogSource() {

      @Override
      public URL getResource(String resourceName) {
        try {
          return new File(emptydir, resourceName).toURL();
        } catch(MalformedURLException e) {
          throw new RuntimeException(e);
        }
      }

      @Override
      public Object getId() {
        return emptydir;
      }
    });

    assertNull(MavenDiscovery.getLifecycleMappingMetadataSource(item));
  }

  public void testCatalogItemExtensionsMetadata() throws Exception {
    final File pluginxml = new File("projects/discovery/projectConfigurator.pluginxml").getCanonicalFile();

    CatalogItem item = new CatalogItem();
    item.setSource(new AbstractCatalogSource() {

      @Override
      public URL getResource(String resourceName) {
        try {
          return pluginxml.toURL();
        } catch(MalformedURLException e) {
          throw new RuntimeException(e);
        }
      }

      @Override
      public Object getId() {
        return pluginxml;
      }
    });

    item.setName("name");
    item.setId("id");

    List<String> configurators = new ArrayList<String>();
    MavenDiscovery.getProvidedProjectConfigurators(item, configurators, new ArrayList<String>());

    assertEquals(1, configurators.size());
    assertEquals("LifecycleMappingTest.projectConfigurator", configurators.get(0));
  }
}
