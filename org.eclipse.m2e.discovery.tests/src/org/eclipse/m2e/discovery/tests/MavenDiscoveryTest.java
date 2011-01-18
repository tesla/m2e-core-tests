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

package org.eclipse.m2e.discovery.tests;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collections;

import junit.framework.TestCase;

import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecution;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.discovery.Catalog;
import org.eclipse.equinox.internal.p2.discovery.DiscoveryCore;
import org.eclipse.equinox.internal.p2.discovery.compatibility.BundleDiscoveryStrategy;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogItem;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.m2e.internal.discovery.MavenDiscovery;
import org.eclipse.m2e.internal.discovery.wizards.MavenCatalogConfiguration;
import org.eclipse.m2e.internal.discovery.wizards.MavenCatalogViewer;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.internal.Workbench;
import org.junit.Test;


@SuppressWarnings("restriction")
public class MavenDiscoveryTest extends TestCase implements IShellProvider {

  private static final String TYCHO_PACKAGE_TYPE = "eclipse-plugin";

  private static final String TYCHO_ID = "com.sonatype.m2e.discovery-directories.lifecycles.tycho_feature";

  private Catalog catalog;

  private Shell shell;

  private MavenCatalogConfiguration configuration;

  @Override
  public void setUp() throws Exception {
    catalog = new Catalog();
    catalog.setEnvironment(DiscoveryCore.createEnvironment());
    catalog.setVerifyUpdateSiteAvailability(false);
    catalog.getDiscoveryStrategies().add(new BundleDiscoveryStrategy());

    // Build the list of tags to show in the Wizard header
    catalog.setTags(Collections.singletonList(MavenDiscovery.APPLICABLE_TAG));

    // Create configuration for the catalog
    configuration = new MavenCatalogConfiguration();
    configuration.setShowTagFilter(true);
    configuration.setSelectedTags(Collections.singletonList(MavenDiscovery.APPLICABLE_TAG));
    configuration.setShowInstalledFilter(false);
    configuration.setSelectedPackagingTypes(Collections.EMPTY_LIST);
    configuration.setSelectedMojos(Collections.EMPTY_LIST);
    configuration.setSelectedLifecycleIds(Collections.EMPTY_LIST);

    shell = new Shell(Workbench.getInstance().getDisplay());
  }

  public void tearDown() throws Exception {
    shell.dispose();
    shell = null;
  }

  @Test
  public void testCatalogUnselected() {
    updateMavenCatalog();


    assertFalse("CatalogItem should not be selected", getCatalogItem(TYCHO_ID).isSelected());
    assertFalse("CatalogItem should not be tagged Applicable",
        getCatalogItem(TYCHO_ID).hasTag(MavenDiscovery.APPLICABLE_TAG));
  }

  @Test
  public void testCatalogSelectedByPackaging() {
    configuration.setSelectedPackagingTypes(Arrays.asList(new String[] {TYCHO_PACKAGE_TYPE}));

    updateMavenCatalog();

    assertTrue("CatalogItem should be selected", getCatalogItem(TYCHO_ID).isSelected());
  }

  @Test
  public void testCatalogTaggedApplicableByPackaging() {
    configuration.setSelectedPackagingTypes(Arrays.asList(new String[] {TYCHO_PACKAGE_TYPE}));

    updateMavenCatalog();

    assertTrue("CatalogItem should be tagged Applicable", getCatalogItem(TYCHO_ID)
        .hasTag(MavenDiscovery.APPLICABLE_TAG));
  }

  @Test
  public void testCatalogSelectedByMojoExecution() {
    configuration.setSelectedMojos(Arrays.asList(new MojoExecution[] {getTychoMojoExecution()}));

    updateMavenCatalog();

    assertTrue("CatalogItem should be selected", getCatalogItem(TYCHO_ID).isSelected());
  }

  @Test
  public void testCatalogTaggedApplicableByMojoExecution() {
    configuration.setSelectedMojos(Arrays.asList(new MojoExecution[] {getTychoMojoExecution()}));

    updateMavenCatalog();

    assertTrue("CatalogItem should be tagged Applicable", getCatalogItem(TYCHO_ID)
        .hasTag(MavenDiscovery.APPLICABLE_TAG));
  }

  @Test
  public void testCatalogSelectedByBoth() {
    configuration.setSelectedMojos(Arrays.asList(new MojoExecution[] {getTychoMojoExecution()}));
    configuration.setSelectedPackagingTypes(Arrays.asList(new String[] {TYCHO_PACKAGE_TYPE}));

    updateMavenCatalog();

    assertTrue("CatalogItem should be selected", getCatalogItem(TYCHO_ID).isSelected());
  }

  @Test
  public void testCatalogTaggedApplicableByBoth() {
    configuration.setSelectedMojos(Arrays.asList(new MojoExecution[] {getTychoMojoExecution()}));
    configuration.setSelectedPackagingTypes(Arrays.asList(new String[] {TYCHO_PACKAGE_TYPE}));

    updateMavenCatalog();

    assertTrue("CatalogItem should be tagged Applicable", getCatalogItem(TYCHO_ID)
        .hasTag(MavenDiscovery.APPLICABLE_TAG));
  }

  @Test
  public void testCatalogSelectedByLifecycleId() {
    configuration.setSelectedLifecycleIds(Collections.singleton("unknown-or-missing"));

    updateMavenCatalog();

    assertTrue("CatalogItem should be selected", getCatalogItem(TYCHO_ID).isSelected());
  }

  @Test
  public void testCatalogSelectedByLifecycleIdWithPeriods() {
    configuration.setSelectedLifecycleIds(Collections.singleton("my.lifecycle.id"));

    updateMavenCatalog();

    assertTrue("CatalogItem should be selected", getCatalogItem(TYCHO_ID).isSelected());
  }

  @Test
  public void testCatalogTaggedApplicableByLifecycleId() {
    configuration.setSelectedLifecycleIds(Collections.singleton("unknown-or-missing"));

    updateMavenCatalog();

    assertTrue("CatalogItem should be selected", getCatalogItem(TYCHO_ID).isSelected());
  }

  private void updateMavenCatalog() {
    MavenCatalogViewer mcv = new MavenCatalogViewer(catalog, this, new RunnableContext(), configuration);
    mcv.createControl(shell);
    mcv.updateCatalog();
  }

  private CatalogItem getCatalogItem(String id) {
    for (CatalogItem ci : catalog.getItems()) {
      if (ci.getId().equals(id))
        return ci;
    }
    fail("CatalogItem " + id + " not found in catalog");
    return null;
  }

  private static MojoExecution getTychoMojoExecution() {
    Plugin plugin = new Plugin();
    plugin.setArtifactId("maven-osgi-compiler-plugin");
    plugin.setGroupId("org.sonatype.tycho");
    plugin.setVersion("0.9.0");
    return new MojoExecution(plugin, "compile", "");
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
}
