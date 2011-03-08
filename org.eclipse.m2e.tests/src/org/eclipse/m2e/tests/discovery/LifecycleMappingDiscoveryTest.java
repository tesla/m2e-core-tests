
package org.eclipse.m2e.tests.discovery;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.Model;
import org.codehaus.plexus.util.IOUtil;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogItem;
import org.eclipse.m2e.core.internal.lifecyclemapping.LifecycleMappingFactory;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.ILifecycleMappingRequirement;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.IMavenDiscoveryProposal;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.LifecycleMappingConfiguration;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.MojoExecutionMappingConfiguration;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.MojoExecutionMappingConfiguration.MojoExecutionMappingRequirement;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.MojoExecutionMappingConfiguration.ProjectConfiguratorMappingRequirement;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.PackagingTypeMappingConfiguration;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.PackagingTypeMappingConfiguration.LifecycleStrategyMappingRequirement;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.PackagingTypeMappingConfiguration.PackagingTypeMappingRequirement;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.ProjectLifecycleMappingConfiguration;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.LifecycleMappingMetadataSource;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.PluginExecutionAction;
import org.eclipse.m2e.core.project.MavenProjectInfo;
import org.eclipse.m2e.core.project.ProjectImportConfiguration;
import org.eclipse.m2e.internal.discovery.InstallCatalogItemMavenDiscoveryProposal;
import org.eclipse.m2e.internal.discovery.MavenDiscovery;
import org.eclipse.m2e.internal.discovery.MavenDiscoveryService;
import org.eclipse.m2e.tests.common.AbstractLifecycleMappingTest;


@SuppressWarnings("restriction")
public class LifecycleMappingDiscoveryTest extends AbstractLifecycleMappingTest {

  private LifecycleMappingConfiguration loadMappingConfiguration(File pomFile) throws CoreException {
    List<MavenProjectInfo> projects = new ArrayList<MavenProjectInfo>();
    Model model = plugin.getMaven().readModel(pomFile);
    projects.add(new MavenProjectInfo(pomFile.toString(), pomFile, model, null));
    LifecycleMappingConfiguration configuration = LifecycleMappingConfiguration.calculate(projects,
        new ProjectImportConfiguration(), monitor);
    return configuration;
  }

  private LifecycleMappingMetadataSource readLifecycleMappingMetadata(String pathname) throws Exception {
    if(pathname == null) {
      return new LifecycleMappingMetadataSource();
    }

    InputStream is = new FileInputStream(new File(pathname));
    try {
      return LifecycleMappingFactory.createLifecycleMappingMetadataSource(is);
    } finally {
      IOUtil.close(is);
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

    List<String> mappingStrategies = new ArrayList<String>();
    List<String> configurators = new ArrayList<String>();
    if(pluginxmlPath != null) {
      FileInputStream is = new FileInputStream(pluginxmlPath);
      try {
        MavenDiscovery.parsePluginXml(is, configurators, mappingStrategies);
      } finally {
        IOUtil.close(is);
      }
    }

    srv.addCatalogItem(item, readLifecycleMappingMetadata(metadataPath), configurators, mappingStrategies);

    return item;
  }

  private ProjectLifecycleMappingConfiguration getProjectMappingConfiguration(
      LifecycleMappingConfiguration configuration, int idx) {
    Iterator<ProjectLifecycleMappingConfiguration> iter = configuration.getProjects().iterator();
    for(int i = 0; i < idx; i++ ) {
      iter.next();
    }
    return iter.next();
  }

  public void testNoProposals() throws CoreException {

    LifecycleMappingConfiguration configuration = loadMappingConfiguration(new File(
        "projects/discovery/mojoExecutions/pom.xml"));

    // sanity check
    assertEquals(1, configuration.getProjects().size());

    MavenDiscoveryService srv = new MavenDiscoveryService();

    ProjectLifecycleMappingConfiguration project = getProjectMappingConfiguration(configuration, 0);
    Map<ILifecycleMappingRequirement, List<IMavenDiscoveryProposal>> proposals = srv.discover(
        project.getMavenProject(), project.getMojoExecutions(), new ArrayList<IMavenDiscoveryProposal>(), monitor);

    assertTrue(proposals.isEmpty());
  }

  public void testProposalMatching() throws Exception {
    LifecycleMappingConfiguration configuration = loadMappingConfiguration(new File(
        "projects/discovery/twoMojoExecutions/pom.xml"));

    MavenDiscoveryService srv = new MavenDiscoveryService();

    List<CatalogItem> items = new ArrayList<CatalogItem>();
    items.add(newCatalogItem(srv, "projects/discovery/match-test-packaging-a.xml"));
    items.add(newCatalogItem(srv, "projects/discovery/match-test-goal-1-and-2.xml"));

    ProjectLifecycleMappingConfiguration project = getProjectMappingConfiguration(configuration, 0);
    Map<ILifecycleMappingRequirement, List<IMavenDiscoveryProposal>> proposals = srv.discover(
        project.getMavenProject(), project.getMojoExecutions(), new ArrayList<IMavenDiscoveryProposal>(), monitor);

    assertEquals(3, proposals.size());

    List<ILifecycleMappingRequirement> elementKeys = new ArrayList<ILifecycleMappingRequirement>(proposals.keySet());

    assertEquals("test-packaging-a", ((PackagingTypeMappingRequirement) elementKeys.get(0)).getPackaging());
    assertInstallCatalogItemProposal(items.get(0), proposals.get(elementKeys.get(0)));

    assertEquals("test-goal-1", ((MojoExecutionMappingRequirement) elementKeys.get(1)).getExecution().getGoal());
    assertInstallCatalogItemProposal(items.get(1), proposals.get(elementKeys.get(1)));

    assertEquals("test-goal-2", ((MojoExecutionMappingRequirement) elementKeys.get(2)).getExecution().getGoal());
    assertInstallCatalogItemProposal(items.get(1), proposals.get(elementKeys.get(2)));
  }

  public void testPreselectedProposals() throws Exception {
    LifecycleMappingConfiguration configuration = loadMappingConfiguration(new File(
        "projects/discovery/mojoExecutions/pom.xml"));

    MavenDiscoveryService srv = new MavenDiscoveryService();
    CatalogItem item = new CatalogItem();
    LifecycleMappingMetadataSource metadataSource = readLifecycleMappingMetadata("projects/discovery/match-test-goal-1-and-2.xml");
    srv.addCatalogItem(item, metadataSource, Collections.EMPTY_LIST, Collections.EMPTY_LIST);

    ProjectLifecycleMappingConfiguration project = getProjectMappingConfiguration(configuration, 0);
    Map<ILifecycleMappingRequirement, List<IMavenDiscoveryProposal>> proposalsmap = srv.discover(
        project.getMavenProject(), project.getMojoExecutions(), new ArrayList<IMavenDiscoveryProposal>(), monitor);

    assertEquals(2, proposalsmap.size());
  }

  public void testIsMappingComplete() throws Exception {
    LifecycleMappingConfiguration configuration = loadMappingConfiguration(new File(
        "projects/discovery/twoMojoExecutions/pom.xml"));

    assertFalse(configuration.isMappingComplete());

    CatalogItem item = new CatalogItem();
    item.setSiteUrl("url");
    item.setInstallableUnits(Arrays.asList("iu"));
    IMavenDiscoveryProposal proposal = new InstallCatalogItemMavenDiscoveryProposal(item);

    ProjectLifecycleMappingConfiguration project = getProjectMappingConfiguration(configuration, 0);
    Map<ILifecycleMappingRequirement, List<IMavenDiscoveryProposal>> allproposals = new LinkedHashMap<ILifecycleMappingRequirement, List<IMavenDiscoveryProposal>>();
    allproposals.put(project.getPackagingTypeMappingConfiguration().getLifecycleMappingRequirement(),
        Arrays.asList(proposal));
    for(MojoExecutionMappingConfiguration mojoExecution : project.getMojoExecutionConfigurations()) {
      allproposals.put(mojoExecution.getLifecycleMappingRequirement(), Arrays.asList(proposal));
    }
    configuration.setProposals(allproposals);
    assertFalse(configuration.isMappingComplete());

    configuration.addSelectedProposal(proposal);

    assertTrue(configuration.isMappingComplete());
  }

  public void testAutomaticSelectionOfSingleProposal() throws Exception {
    LifecycleMappingConfiguration configuration = loadMappingConfiguration(new File(
        "projects/discovery/twoMojoExecutions/pom.xml"));

    MavenDiscoveryService srv = new MavenDiscoveryService();

    List<CatalogItem> items = new ArrayList<CatalogItem>();
    items.add(newCatalogItem(srv, "projects/discovery/match-test-packaging-a.xml"));
    items.add(newCatalogItem(srv, "projects/discovery/match-test-goal-1-and-2.xml"));

    ProjectLifecycleMappingConfiguration project = getProjectMappingConfiguration(configuration, 0);
    Map<ILifecycleMappingRequirement, List<IMavenDiscoveryProposal>> proposals = srv.discover(
        project.getMavenProject(), project.getMojoExecutions(), new ArrayList<IMavenDiscoveryProposal>(), monitor);

    assertEquals(3, proposals.size());

    configuration.setProposals(proposals);

    // sanity check
    assertEquals(0, configuration.getSelectedProposals().size());

    configuration.autoCompleteMapping();

    List<ILifecycleMappingRequirement> elementKeys = new ArrayList<ILifecycleMappingRequirement>(proposals.keySet());

    PackagingTypeMappingRequirement packagingType = (PackagingTypeMappingRequirement) elementKeys.get(0);
    MojoExecutionMappingRequirement goal1 = (MojoExecutionMappingRequirement) elementKeys.get(1);
    MojoExecutionMappingRequirement goal2 = (MojoExecutionMappingRequirement) elementKeys.get(2);

    assertEquals("test-packaging-a", packagingType.getPackaging());
    assertEquals("test-goal-1", goal1.getExecution().getGoal());
    assertEquals("test-goal-2", goal2.getExecution().getGoal());

    assertSame(items.get(0),
        ((InstallCatalogItemMavenDiscoveryProposal) configuration.getSelectedProposal(packagingType)).getCatalogItem());
    assertSame(items.get(1),
        ((InstallCatalogItemMavenDiscoveryProposal) configuration.getSelectedProposal(goal1)).getCatalogItem());
    assertSame(items.get(1),
        ((InstallCatalogItemMavenDiscoveryProposal) configuration.getSelectedProposal(goal2)).getCatalogItem());

    assertTrue(configuration.isMappingComplete());
  }

  public void testAutomaticSelectionWithAmbiguousProposals() throws Exception {
    LifecycleMappingConfiguration configuration = loadMappingConfiguration(new File(
        "projects/discovery/twoMojoExecutions/pom.xml"));

    MavenDiscoveryService srv = new MavenDiscoveryService();

    // can't choose between match and another-match automatically
    List<CatalogItem> items = new ArrayList<CatalogItem>();
    items.add(newCatalogItem(srv, "projects/discovery/match-test-goal-1.xml"));
    items.add(newCatalogItem(srv, "projects/discovery/another-match-test-goal-1.xml"));

    ProjectLifecycleMappingConfiguration project = configuration.getProjects().iterator().next();
    Map<ILifecycleMappingRequirement, List<IMavenDiscoveryProposal>> proposals = srv.discover(
        project.getMavenProject(), project.getMojoExecutions(), new ArrayList<IMavenDiscoveryProposal>(), monitor);

    assertEquals(1, proposals.size());

    configuration.setProposals(proposals);

    // sanity check
    assertFalse(configuration.isMappingComplete());
    assertEquals(0, configuration.getSelectedProposals().size());

    configuration.autoCompleteMapping();

    List<ILifecycleMappingRequirement> elementKeys = new ArrayList<ILifecycleMappingRequirement>(proposals.keySet());

    MojoExecutionMappingRequirement goal1 = (MojoExecutionMappingRequirement) elementKeys.get(0);

    assertEquals("test-goal-1", goal1.getExecution().getGoal());

    assertNull(configuration.getSelectedProposal(goal1));
  }

  public void testDiscoverProjectConfigurators() throws Exception {
    LifecycleMappingConfiguration configuration = loadMappingConfiguration(new File(
        "projects/discovery/projectConfigurator/pom.xml"));

    MavenDiscoveryService srv = new MavenDiscoveryService();

    List<CatalogItem> items = new ArrayList<CatalogItem>();
    items.add(newCatalogItem(srv, null, "projects/discovery/projectConfigurator.pluginxml"));

    ProjectLifecycleMappingConfiguration project = getProjectMappingConfiguration(configuration, 0);

    MojoExecutionMappingConfiguration goal1 = project.getMojoExecutionConfigurations().get(1);
    ILifecycleMappingRequirement goal1Requirement = goal1.getLifecycleMappingRequirement();

    // sanity checks
    assertEquals("test-goal-1", goal1.getGoal());
    assertNotNull(goal1.getMapping());
    assertEquals(PluginExecutionAction.configurator, goal1.getMapping().getAction());
    assertEquals("LifecycleMappingTest.projectConfigurator",
        LifecycleMappingFactory.getProjectConfiguratorId(goal1.getMapping()));
    assertTrue(goal1Requirement instanceof ProjectConfiguratorMappingRequirement);

    Map<ILifecycleMappingRequirement, List<IMavenDiscoveryProposal>> proposals = srv.discover(
        project.getMavenProject(), project.getMojoExecutions(), new ArrayList<IMavenDiscoveryProposal>(), monitor);

    assertEquals(1, proposals.size());

    List<ILifecycleMappingRequirement> elementKeys = new ArrayList<ILifecycleMappingRequirement>(proposals.keySet());

    ProjectConfiguratorMappingRequirement configurator = (ProjectConfiguratorMappingRequirement) elementKeys.get(0);

    assertEquals("LifecycleMappingTest.projectConfigurator", configurator.getProjectConfiguratorId());

    assertSame(items.get(0),
        ((InstallCatalogItemMavenDiscoveryProposal) proposals.get(configurator).get(0)).getCatalogItem());
  }

  public void testDiscoverLifecycleMappingStrategy() throws Exception {
    LifecycleMappingConfiguration configuration = loadMappingConfiguration(new File(
        "projects/discovery/lifecycleId/pom.xml"));

    MavenDiscoveryService srv = new MavenDiscoveryService();

    List<CatalogItem> items = new ArrayList<CatalogItem>();
    items.add(newCatalogItem(srv, null, "projects/discovery/lifecycleId.pluginxml"));

    ProjectLifecycleMappingConfiguration project = getProjectMappingConfiguration(configuration, 0);
    Map<ILifecycleMappingRequirement, List<IMavenDiscoveryProposal>> proposals = srv.discover(
        project.getMavenProject(), project.getMojoExecutions(), new ArrayList<IMavenDiscoveryProposal>(), monitor);

    assertEquals(1, proposals.size());

    List<ILifecycleMappingRequirement> elementKeys = new ArrayList<ILifecycleMappingRequirement>(proposals.keySet());

    LifecycleStrategyMappingRequirement packagingType = (LifecycleStrategyMappingRequirement) elementKeys.get(0);

    assertEquals("lifecycleId", packagingType.getLifecycleMappingId());

    assertSame(items.get(0),
        ((InstallCatalogItemMavenDiscoveryProposal) proposals.get(packagingType).get(0)).getCatalogItem());
  }

  public void testProposalToReplaceDefaultMapping() throws Exception {
    LifecycleMappingMetadataSource defaultMetadata = loadLifecycleMappingMetadataSource("projects/discovery/default-test-packaging-a.xml");
    LifecycleMappingFactory.setDefaultLifecycleMappingMetadataSource(defaultMetadata);

    LifecycleMappingConfiguration configuration = loadMappingConfiguration(new File(
        "projects/discovery/twoMojoExecutions/pom.xml"));

    MavenDiscoveryService srv = new MavenDiscoveryService();

    List<CatalogItem> items = new ArrayList<CatalogItem>();
    items.add(newCatalogItem(srv, "projects/discovery/match-test-packaging-a.xml"));
    items.add(newCatalogItem(srv, "projects/discovery/match-test-goal-1.xml"));

    ProjectLifecycleMappingConfiguration project = getProjectMappingConfiguration(configuration, 0);
    configuration.setProposals(srv.discover(project.getMavenProject(), project.getMojoExecutions(),
        new ArrayList<IMavenDiscoveryProposal>(), monitor));

    PackagingTypeMappingConfiguration pkgConfig = project.getPackagingTypeMappingConfiguration();
    ILifecycleMappingRequirement pkgRequirement = pkgConfig.getLifecycleMappingRequirement();

    // sanity checks
    assertEquals("test-packaging-a", pkgConfig.getPackaging());
    assertNull(pkgConfig.getLifecycleMappingId());
    assertTrue(pkgRequirement instanceof PackagingTypeMappingRequirement);
    assertTrue(configuration.isRequirementSatisfied(pkgRequirement, true));

    List<IMavenDiscoveryProposal> proposals = configuration.getProposals(pkgRequirement);
    assertEquals(1, proposals.size());
    assertSame(items.get(0), ((InstallCatalogItemMavenDiscoveryProposal) proposals.get(0)).getCatalogItem());

    MojoExecutionMappingConfiguration goal1 = project.getMojoExecutionConfigurations().get(1);
    ILifecycleMappingRequirement goal1Requirement = goal1.getLifecycleMappingRequirement();

    // sanity checks
    assertEquals("test-goal-1", goal1.getGoal());
    assertNull(goal1.getMapping());
    assertTrue(goal1Requirement instanceof MojoExecutionMappingRequirement);

    proposals = configuration.getProposals(goal1Requirement);
    assertEquals(1, proposals.size());
    assertSame(items.get(1), ((InstallCatalogItemMavenDiscoveryProposal) proposals.get(0)).getCatalogItem());
    assertTrue(configuration.isRequirementSatisfied(goal1Requirement, true));

    MojoExecutionMappingConfiguration goal2 = project.getMojoExecutionConfigurations().get(2);
    ILifecycleMappingRequirement goal2Requirement = goal2.getLifecycleMappingRequirement();

    // sanity checks
    assertEquals("test-goal-2", goal2.getGoal());
    assertNull(goal2.getMapping());
    assertTrue(goal2Requirement instanceof MojoExecutionMappingRequirement);

    proposals = configuration.getProposals(goal2Requirement);
    assertEquals(0, proposals.size());
    assertTrue(configuration.isRequirementSatisfied(goal2Requirement, true));
  }

  public void testDefaultNonConfiguratorMapping() throws Exception {
    LifecycleMappingMetadataSource defaultMetadata = loadLifecycleMappingMetadataSource("projects/discovery/match-test-goal-1-and-2.xml");
    LifecycleMappingFactory.setDefaultLifecycleMappingMetadataSource(defaultMetadata);

    LifecycleMappingConfiguration configuration = loadMappingConfiguration(new File(
        "projects/discovery/twoMojoExecutions/pom.xml"));

    ProjectLifecycleMappingConfiguration project = getProjectMappingConfiguration(configuration, 0);

    MojoExecutionMappingConfiguration goal1 = project.getMojoExecutionConfigurations().get(1);
    assertEquals("test-goal-1", goal1.getGoal());
    assertTrue(configuration.isRequirementSatisfied(goal1.getLifecycleMappingRequirement(), true));

    MojoExecutionMappingConfiguration goal2 = project.getMojoExecutionConfigurations().get(2);
    assertEquals("test-goal-2", goal2.getGoal());
    assertTrue(configuration.isRequirementSatisfied(goal2.getLifecycleMappingRequirement(), true));
  }
}
