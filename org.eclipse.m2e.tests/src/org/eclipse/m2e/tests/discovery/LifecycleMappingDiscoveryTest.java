
package org.eclipse.m2e.tests.discovery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogItem;

import org.eclipse.m2e.core.internal.lifecyclemapping.LifecycleMappingFactory;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.ILifecycleMappingRequirement;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.IMavenDiscoveryProposal;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.LifecycleMappingDiscoveryRequest;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.MojoExecutionMappingConfiguration.MojoExecutionMappingRequirement;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.MojoExecutionMappingConfiguration.ProjectConfiguratorMappingRequirement;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.PackagingTypeMappingConfiguration.LifecycleStrategyMappingRequirement;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.LifecycleMappingMetadataSource;
import org.eclipse.m2e.core.ui.internal.wizards.LifecycleMappingDiscoveryHelper;
import org.eclipse.m2e.internal.discovery.InstallCatalogItemMavenDiscoveryProposal;
import org.eclipse.m2e.internal.discovery.MavenDiscovery;
import org.eclipse.m2e.internal.discovery.MavenDiscoveryService;
import org.eclipse.m2e.tests.common.AbstractLifecycleMappingTest;


/**
 * This class tests Lifecycle Mapping proposals discovery
 * 
 * @author Fred Bricon
 */
public class LifecycleMappingDiscoveryTest extends AbstractLifecycleMappingTest {

  private LifecycleMappingMetadataSource readLifecycleMappingMetadata(String pathname) throws Exception {
    if(pathname == null) {
      return new LifecycleMappingMetadataSource();
    }
    try (InputStream is = new FileInputStream(new File(pathname))) {
      return LifecycleMappingFactory.createLifecycleMappingMetadataSource(is);
    }
  }

  private void assertInstallCatalogItemProposal(CatalogItem catalogItem, List<IMavenDiscoveryProposal> proposals) {
    assertEquals(1, proposals.size());
    assertSame(catalogItem, ((InstallCatalogItemMavenDiscoveryProposal) proposals.get(0)).getCatalogItem());
  }

  private CatalogItem newCatalogItem(MavenDiscoveryService srv, String metadataPath) throws Exception {
    return newCatalogItem(srv, metadataPath, null);
  }

  private CatalogItem newCatalogItem(MavenDiscoveryService srv, String metadataPath, String pluginxmlPath)
      throws Exception {
    CatalogItem item = new CatalogItem();
    item.setSiteUrl(Integer.toString(item.hashCode()));
    item.setInstallableUnits(Arrays.asList(Integer.toString(item.hashCode())));

    List<String> mappingStrategies = new ArrayList<>();
    List<String> configurators = new ArrayList<>();
    if(pluginxmlPath != null) {
      try (FileInputStream is = new FileInputStream(pluginxmlPath)) {
        MavenDiscovery.parsePluginXml(is, configurators, mappingStrategies);
      }
    }

    srv.addCatalogItem(item, readLifecycleMappingMetadata(metadataPath), configurators, mappingStrategies);

    return item;
  }

  @Test
  public void testPackagingPom() throws Exception {
    LifecycleMappingDiscoveryRequest request = loadLifecycleMappingDiscoveryRequest("projects/discovery/packagingPom/pom.xml");

    // sanity check
    assertEquals(0, request.getProjects().size());
    assertTrue("Expected a complete mapping", request.isMappingComplete());
  }

  @Test
  public void testNoProposals() throws Exception {
    LifecycleMappingDiscoveryRequest request = loadLifecycleMappingDiscoveryRequest("projects/discovery/mojoExecutions/pom.xml");

    // sanity check
    assertEquals(1, request.getProjects().size());
    assertFalse("Expected an incomplete mapping", request.isMappingComplete());

    MavenDiscoveryService srv = new MavenDiscoveryService();

    Map<ILifecycleMappingRequirement, List<IMavenDiscoveryProposal>> proposals = srv.discover(
        request.getRequirements(), null, monitor);
    assertTrue("Didn't expect discovery proposals", proposals.isEmpty());
  }

  @Test
  public void testProposalMatching() throws Exception {
    LifecycleMappingDiscoveryRequest request = loadLifecycleMappingDiscoveryRequest("projects/discovery/twoMojoExecutions/pom.xml");

    // sanity check
    assertEquals(1, request.getProjects().size());
    assertFalse("Expected an incomplete mapping", request.isMappingComplete());
    assertEquals("Unexpected requirements number", 2, request.getRequirements().size());

    MojoExecutionMappingRequirement goal1 = null;
    MojoExecutionMappingRequirement goal2 = null;

    for(ILifecycleMappingRequirement req : request.getRequirements()) {
      if(req instanceof MojoExecutionMappingRequirement) {
        MojoExecutionMappingRequirement goal = (MojoExecutionMappingRequirement) req;
        if("test-goal-1".equals(goal.getExecution().goal())) {
          goal1 = goal;
        } else if("test-goal-2".equals(goal.getExecution().goal())) {
          goal2 = goal;
        }
      }
    }

    assertNotNull("test-goal-1 requirement is missing", goal1);
    assertNotNull("test-goal-2 requirement is missing", goal2);

    MavenDiscoveryService srv = new MavenDiscoveryService();

    List<CatalogItem> items = new ArrayList<>();
    items.add(newCatalogItem(srv, "projects/discovery/match-test-packaging-a.xml"));
    items.add(newCatalogItem(srv, "projects/discovery/match-test-goal-1-and-2.xml"));

    Map<ILifecycleMappingRequirement, List<IMavenDiscoveryProposal>> proposals = srv.discover(
        request.getRequirements(), null, monitor);

    assertEquals(2, proposals.size());

    assertInstallCatalogItemProposal(items.get(1), proposals.get(goal1));
    assertInstallCatalogItemProposal(items.get(1), proposals.get(goal2));
  }

  @Test
  public void testPreselectedProposals() throws Exception {
    LifecycleMappingDiscoveryRequest request = loadLifecycleMappingDiscoveryRequest("projects/discovery/mojoExecutions/pom.xml");

    // sanity check
    assertEquals(1, request.getProjects().size());
    assertFalse("Expected an incomplete mapping", request.isMappingComplete());
    assertEquals("Unexpected requirements number", 2, request.getRequirements().size());

    MavenDiscoveryService srv = new MavenDiscoveryService();
    CatalogItem item = new CatalogItem();
    LifecycleMappingMetadataSource metadataSource = readLifecycleMappingMetadata("projects/discovery/match-test-goal-1-and-2.xml");
    srv.addCatalogItem(item, metadataSource, Collections.emptyList(), Collections.emptyList());

    Map<ILifecycleMappingRequirement, List<IMavenDiscoveryProposal>> proposals = srv.discover(
        request.getRequirements(), null, monitor);

    assertEquals(2, proposals.size());
  }

  @Test
  public void testIsMappingComplete() throws Exception {
    LifecycleMappingDiscoveryRequest request = loadLifecycleMappingDiscoveryRequest("projects/discovery/twoMojoExecutions/pom.xml");

    // sanity check
    assertEquals(1, request.getProjects().size());
    assertFalse("Expected an incomplete mapping", request.isMappingComplete());
    assertEquals("Unexpected requirements number", 2, request.getRequirements().size());

    CatalogItem item = new CatalogItem();
    item.setSiteUrl("url");
    item.setInstallableUnits(Arrays.asList("iu"));
    IMavenDiscoveryProposal proposal = new InstallCatalogItemMavenDiscoveryProposal(item);

    Map<ILifecycleMappingRequirement, List<IMavenDiscoveryProposal>> allproposals = new LinkedHashMap<>();
    for(ILifecycleMappingRequirement requirement : request.getRequirements()) {
      allproposals.put(requirement, Arrays.asList(proposal));
    }
    request.setProposals(allproposals);
    assertFalse(request.isMappingComplete());

    request.addSelectedProposal(proposal);

    assertTrue(request.isMappingComplete());
  }

  @Test
  public void testAutomaticSelectionOfSingleProposal() throws Exception {
    LifecycleMappingDiscoveryRequest request = loadLifecycleMappingDiscoveryRequest("projects/discovery/twoMojoExecutions/pom.xml");

    // sanity check
    assertEquals(1, request.getProjects().size());
    assertFalse("Expected an incomplete mapping", request.isMappingComplete());
    assertEquals("Unexpected requirements number", 2, request.getRequirements().size());

    MojoExecutionMappingRequirement goal1 = null;
    MojoExecutionMappingRequirement goal2 = null;

    for(ILifecycleMappingRequirement req : request.getRequirements()) {
      if(req instanceof MojoExecutionMappingRequirement) {
        MojoExecutionMappingRequirement goal = (MojoExecutionMappingRequirement) req;
        if("test-goal-1".equals(goal.getExecution().goal())) {
          goal1 = goal;
        } else if("test-goal-2".equals(goal.getExecution().goal())) {
          goal2 = goal;
        }
      }
    }

    assertNotNull("test-goal-1 requirement is missing", goal1);
    assertNotNull("test-goal-2 requirement is missing", goal2);

    MavenDiscoveryService srv = new MavenDiscoveryService();

    List<CatalogItem> items = new ArrayList<>();
    items.add(newCatalogItem(srv, "projects/discovery/match-test-packaging-a.xml"));
    items.add(newCatalogItem(srv, "projects/discovery/match-test-goal-1-and-2.xml"));

    Map<ILifecycleMappingRequirement, List<IMavenDiscoveryProposal>> proposals = srv.discover(
        request.getRequirements(), null, monitor);

    assertEquals(2, proposals.size());

    request.setProposals(proposals);

    // sanity check
    assertEquals(0, request.getSelectedProposals().size());

    request.autoCompleteMapping();

    assertInstallCatalogItemProposal(items.get(1), proposals.get(goal1));
    assertInstallCatalogItemProposal(items.get(1), proposals.get(goal2));

    assertTrue(request.isMappingComplete());
  }

  @Test
  public void testAutomaticSelectionWithAmbiguousProposals() throws Exception {
    LifecycleMappingDiscoveryRequest request = loadLifecycleMappingDiscoveryRequest("projects/discovery/twoMojoExecutions/pom.xml");

    // sanity check
    assertEquals(1, request.getProjects().size());
    assertFalse("Expected an incomplete mapping", request.isMappingComplete());
    assertEquals("Unexpected requirements number", 2, request.getRequirements().size());

    MavenDiscoveryService srv = new MavenDiscoveryService();

    // can't choose between match and another-match automatically
    List<CatalogItem> items = new ArrayList<>();
    items.add(newCatalogItem(srv, "projects/discovery/match-test-goal-1.xml"));
    items.add(newCatalogItem(srv, "projects/discovery/another-match-test-goal-1.xml"));
    Map<ILifecycleMappingRequirement, List<IMavenDiscoveryProposal>> proposals = srv.discover(
        request.getRequirements(), null, monitor);

    assertEquals(1, proposals.size());
    assertEquals(2, proposals.values().iterator().next().size());

    request.setProposals(proposals);

    // sanity check
    assertFalse(request.isMappingComplete());
    assertEquals(0, request.getSelectedProposals().size());

    request.autoCompleteMapping();
    assertFalse(request.isMappingComplete());

    List<ILifecycleMappingRequirement> elementKeys = new ArrayList<>(proposals.keySet());

    MojoExecutionMappingRequirement goal1 = (MojoExecutionMappingRequirement) elementKeys.get(0);

    assertEquals("test-goal-1", goal1.getExecution().goal());

    assertNull(request.getSelectedProposal(goal1));
  }

  @Test
  public void testDiscoverProjectConfigurators() throws Exception {
    LifecycleMappingDiscoveryRequest request = loadLifecycleMappingDiscoveryRequest("projects/discovery/projectConfigurator/pom.xml");

    // sanity check
    assertEquals(1, request.getProjects().size());
    assertFalse("Expected an incomplete mapping", request.isMappingComplete());
    assertEquals("Unexpected requirements number", 2, request.getRequirements().size());

    MavenDiscoveryService srv = new MavenDiscoveryService();

    List<CatalogItem> items = new ArrayList<>();
    items.add(newCatalogItem(srv, null, "projects/discovery/projectConfigurator.pluginxml"));
    ProjectConfiguratorMappingRequirement configurator = null;
    for(ILifecycleMappingRequirement req : request.getRequirements()) {
      if(req instanceof ProjectConfiguratorMappingRequirement) {
        configurator = (ProjectConfiguratorMappingRequirement) req;
      }
    }
    assertNotNull("projectConfiguratorRequirement not found", configurator);
    assertEquals("LifecycleMappingTest.projectConfigurator", configurator.getProjectConfiguratorId());
    // sanity checks
    /*
    assertEquals("test-goal-1", goal1.getGoal());
    assertNotNull(goal1.getMapping());
    assertEquals(PluginExecutionAction.configurator, goal1.getMapping().getAction());
    assertEquals("LifecycleMappingTest.projectConfigurator",
        LifecycleMappingFactory.getProjectConfiguratorId(goal1.getMapping()));
    */

    Map<ILifecycleMappingRequirement, List<IMavenDiscoveryProposal>> proposals = srv.discover(
        request.getRequirements(), null, monitor);

    assertEquals(2, proposals.size()); // two different mojo executions

    assertSame(items.get(0),
        ((InstallCatalogItemMavenDiscoveryProposal) proposals.get(configurator).get(0)).getCatalogItem());
  }

  @Test
  public void testDiscoverLifecycleMappingStrategy() throws Exception {
    LifecycleMappingDiscoveryRequest request = loadLifecycleMappingDiscoveryRequest("projects/discovery/lifecycleId/pom.xml");

    // sanity check
    assertEquals(1, request.getProjects().size());
    assertFalse("Expected an incomplete mapping", request.isMappingComplete());
    assertEquals("Unexpected requirements number", 1, request.getRequirements().size());

    ILifecycleMappingRequirement req = request.getRequirements().iterator().next();
    assertTrue(req instanceof LifecycleStrategyMappingRequirement);
    LifecycleStrategyMappingRequirement packagingType = (LifecycleStrategyMappingRequirement) req;
    assertEquals("lifecycleId", packagingType.getLifecycleMappingId());

    MavenDiscoveryService srv = new MavenDiscoveryService();

    List<CatalogItem> items = new ArrayList<>();
    items.add(newCatalogItem(srv, null, "projects/discovery/lifecycleId.pluginxml"));

    Map<ILifecycleMappingRequirement, List<IMavenDiscoveryProposal>> proposals = srv.discover(
        request.getRequirements(), null, monitor);

    assertEquals(1, proposals.get(packagingType).size());

    assertSame(items.get(0),
        ((InstallCatalogItemMavenDiscoveryProposal) proposals.get(packagingType).get(0)).getCatalogItem());
  }

  /*
   * Different behavior from {@link LifecycleMappingDiscoveryTest#testProposalToReplaceDefaultMapping()}, as no Lifecycle Mapping errors are found on the project.
   */
  @Test
  public void testProposalToReplaceDefaultMapping() throws Exception {
    LifecycleMappingMetadataSource defaultMetadata = loadLifecycleMappingMetadataSource("projects/discovery/default-test-packaging-a.xml");
    LifecycleMappingFactory.setDefaultLifecycleMappingMetadataSource(defaultMetadata);

    LifecycleMappingDiscoveryRequest request = loadLifecycleMappingDiscoveryRequest("projects/discovery/twoMojoExecutions/pom.xml");

    // sanity check
    assertEquals(0, request.getProjects().size());
    assertTrue("Expected a complete mapping", request.isMappingComplete());
    assertEquals("Unexpected requirements number", 0, request.getRequirements().size());
  }

  /*
   * Different behavior from {@link LifecycleMappingDiscoveryTest#testDefaultNonConfiguratorMapping()}, as no Lifecycle Mapping errors are found on the project.
   */
  @Test
  public void testDefaultNonConfiguratorMapping() throws Exception {
    LifecycleMappingMetadataSource defaultMetadata = loadLifecycleMappingMetadataSource("projects/discovery/match-test-goal-1-and-2.xml");
    LifecycleMappingFactory.setDefaultLifecycleMappingMetadataSource(defaultMetadata);

    LifecycleMappingDiscoveryRequest request = loadLifecycleMappingDiscoveryRequest("projects/discovery/twoMojoExecutions/pom.xml");

    //No errors so no need for discovery
    assertEquals(0, request.getProjects().size());
    assertTrue("Expected a complete mapping", request.isMappingComplete());
    assertEquals("Unexpected requirements number", 0, request.getRequirements().size());
  }

  private LifecycleMappingDiscoveryRequest loadLifecycleMappingDiscoveryRequest(String pomLocation) throws IOException,
      CoreException {
    IProject project = importProject(pomLocation);
    return LifecycleMappingDiscoveryHelper.createLifecycleMappingDiscoveryRequest(project, monitor);
  }

}
