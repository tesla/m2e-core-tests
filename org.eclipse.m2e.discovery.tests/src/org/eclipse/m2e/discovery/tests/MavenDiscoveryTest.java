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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecution;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.discovery.Catalog;
import org.eclipse.equinox.internal.p2.discovery.DiscoveryCore;
import org.eclipse.equinox.internal.p2.discovery.compatibility.BundleDiscoveryStrategy;
import org.eclipse.equinox.internal.p2.discovery.model.Tag;
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

  static final Tag TAG_ONE = new Tag("one", "One");

  static final Tag TAG_TWO = new Tag("two", "Two");

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
    List<Tag> tags = new ArrayList<Tag>(3);
    tags.add(MavenDiscovery.APPLICABLE_TAG);
    tags.add(TAG_ONE);
    tags.add(TAG_TWO);
    catalog.setTags(tags);

    // Create configuration for the catalog
    configuration = new MavenCatalogConfiguration();
    configuration.setShowTagFilter(true);
    configuration.setSelectedTags(tags);
    configuration.setShowInstalledFilter(false);
    configuration.setSelectedPackagingTypes(Collections.EMPTY_LIST);
    configuration.setSelectedMojos(Collections.EMPTY_LIST);

    shell = new Shell(Workbench.getInstance().getDisplay());
  }

  public void tearDown() throws Exception {
    shell.dispose();
    shell = null;
  }

  @Test
  public void testCatalogUnselected() {
    MavenCatalogViewer mcv = new MavenCatalogViewer(catalog, this, new RunnableContext(), configuration);
    mcv.createControl(shell);
    mcv.updateCatalog();

    assertEquals("Unexpected number of catalog items", 1, catalog.getItems().size());
    assertFalse("CatalogItem should not be selected", catalog.getItems().get(0).isSelected());
    assertFalse("CatalogItem should not be tagged Applicable",
        catalog.getItems().get(0).hasTag(MavenDiscovery.APPLICABLE_TAG));
  }

  @Test
  public void testCatalogSelectedByPackaging() {
    configuration.setSelectedPackagingTypes(Arrays.asList(new String[] {"eclipse-plugin"}));

    MavenCatalogViewer mcv = new MavenCatalogViewer(catalog, this, new RunnableContext(), configuration);
    mcv.createControl(shell);
    mcv.updateCatalog();

    assertEquals("Unexpected number of catalog items", 1, catalog.getItems().size());
    assertTrue("CatalogItem should be selected", catalog.getItems().get(0).isSelected());
  }

  @Test
  public void testCatalogTaggedApplicableByPackaging() {
    configuration.setSelectedPackagingTypes(Arrays.asList(new String[] {"eclipse-plugin"}));

    MavenCatalogViewer mcv = new MavenCatalogViewer(catalog, this, new RunnableContext(), configuration);
    mcv.createControl(shell);
    mcv.updateCatalog();

    assertEquals("Unexpected number of catalog items", 1, catalog.getItems().size());
    assertTrue("CatalogItem should be tagged Applicable",
        catalog.getItems().get(0).hasTag(MavenDiscovery.APPLICABLE_TAG));
  }

  @Test
  public void testCatalogSelectedByMojoExecution() {
    configuration.setSelectedMojos(Arrays.asList(new MojoExecution[] {getTychoMojoExecution()}));

    MavenCatalogViewer mcv = new MavenCatalogViewer(catalog, this, new RunnableContext(), configuration);
    mcv.createControl(shell);
    mcv.updateCatalog();
    assertEquals("Catalog entries missing", 1, catalog.getItems().size());
    assertTrue("CatalogItem should be selected", catalog.getItems().get(0).isSelected());
  }

  @Test
  public void testCatalogTaggedApplicableByMojoExecution() {
    configuration.setSelectedMojos(Arrays.asList(new MojoExecution[] {getTychoMojoExecution()}));

    MavenCatalogViewer mcv = new MavenCatalogViewer(catalog, this, new RunnableContext(), configuration);
    mcv.createControl(shell);
    mcv.updateCatalog();
    assertEquals("Catalog entries missing", 1, catalog.getItems().size());
    assertTrue("CatalogItem should be tagged Applicable",
        catalog.getItems().get(0).hasTag(MavenDiscovery.APPLICABLE_TAG));
  }

  @Test
  public void testCatalogSelectedByBoth() {
    configuration.setSelectedMojos(Arrays.asList(new MojoExecution[] {getTychoMojoExecution()}));
    configuration.setSelectedPackagingTypes(Arrays.asList(new String[] {"eclipse-plugin"}));

    MavenCatalogViewer mcv = new MavenCatalogViewer(catalog, this, new RunnableContext(), configuration);
    mcv.createControl(shell);
    mcv.updateCatalog();
    assertEquals("Catalog entries missing", 1, catalog.getItems().size());
    assertTrue("CatalogItem should be selected", catalog.getItems().get(0).isSelected());
  }

  @Test
  public void testCatalogTaggedApplicableByBoth() {
    configuration.setSelectedMojos(Arrays.asList(new MojoExecution[] {getTychoMojoExecution()}));
    configuration.setSelectedPackagingTypes(Arrays.asList(new String[] {"eclipse-plugin"}));

    MavenCatalogViewer mcv = new MavenCatalogViewer(catalog, this, new RunnableContext(), configuration);
    mcv.createControl(shell);
    mcv.updateCatalog();
    assertEquals("Catalog entries missing", 1, catalog.getItems().size());
    assertTrue("CatalogItem should be tagged Applicable",
        catalog.getItems().get(0).hasTag(MavenDiscovery.APPLICABLE_TAG));
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
