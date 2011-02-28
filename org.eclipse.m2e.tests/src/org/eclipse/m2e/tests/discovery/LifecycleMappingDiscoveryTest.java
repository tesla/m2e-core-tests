
package org.eclipse.m2e.tests.discovery;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.Model;
import org.codehaus.plexus.util.IOUtil;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogItem;
import org.eclipse.m2e.core.internal.lifecyclemapping.LifecycleMappingFactory;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.ILifecycleMappingElementKey;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.IMavenDiscoveryProposal;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.MojoExecutionMappingConfiguration;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.PackagingTypeMappingConfiguration;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.ProjectLifecycleMappingConfiguration;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.LifecycleMappingMetadataSource;
import org.eclipse.m2e.core.project.MavenProjectInfo;
import org.eclipse.m2e.core.project.ProjectImportConfiguration;
import org.eclipse.m2e.core.ui.internal.lifecyclemapping.LifecycleMappingConfiguration;
import org.eclipse.m2e.internal.discovery.InstallCatalogItemMavenDiscoveryProposal;
import org.eclipse.m2e.internal.discovery.MavenDiscoveryService;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;


@SuppressWarnings("restriction")
public class LifecycleMappingDiscoveryTest extends AbstractMavenProjectTestCase {

  private LifecycleMappingConfiguration loadMappingConfiguration(File pomFile) throws CoreException {
    List<MavenProjectInfo> projects = new ArrayList<MavenProjectInfo>();
    Model model = plugin.getMaven().readModel(pomFile);
    projects.add(new MavenProjectInfo(pomFile.toString(), pomFile, model, null));
    LifecycleMappingConfiguration configuration = LifecycleMappingConfiguration.calculate(projects,
        new ProjectImportConfiguration(), monitor);
    return configuration;
  }

  private LifecycleMappingMetadataSource readLifecycleMappingMetadata(String pathname) throws Exception {
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
    CatalogItem item = new CatalogItem();
    item.setSiteUrl(metadataPath);
    item.setInstallableUnits(Arrays.asList(metadataPath));

    srv.addCatalogItem(item, readLifecycleMappingMetadata(metadataPath));

    return item;
  }

  public void testNoProposals() throws CoreException {

    LifecycleMappingConfiguration configuration = loadMappingConfiguration(new File(
        "projects/discovery/mojoExecutions/pom.xml"));

    // sanity check
    assertEquals(1, configuration.getProjects().size());

    MavenDiscoveryService srv = new MavenDiscoveryService();

    ProjectLifecycleMappingConfiguration project = configuration.getProjects().get(0);
    Map<ILifecycleMappingElementKey, List<IMavenDiscoveryProposal>> proposals = srv.discover(project.getMavenProject(),
        project.getMojoExecutions(), new ArrayList<IMavenDiscoveryProposal>(), monitor);

    assertTrue(proposals.isEmpty());
  }

  public void testProposalMatching() throws Exception {
    LifecycleMappingConfiguration configuration = loadMappingConfiguration(new File(
        "projects/discovery/twoMojoExecutions/pom.xml"));

    MavenDiscoveryService srv = new MavenDiscoveryService();

    List<CatalogItem> items = new ArrayList<CatalogItem>();
    items.add(newCatalogItem(srv, "projects/discovery/match-test-packaging-a.xml"));
    items.add(newCatalogItem(srv, "projects/discovery/match-test-goal-1-and-2.xml"));

    ProjectLifecycleMappingConfiguration project = configuration.getProjects().get(0);
    Map<ILifecycleMappingElementKey, List<IMavenDiscoveryProposal>> proposals = srv.discover(project.getMavenProject(),
        project.getMojoExecutions(), new ArrayList<IMavenDiscoveryProposal>(), monitor);

    assertEquals(3, proposals.size());

    List<ILifecycleMappingElementKey> elementKeys = new ArrayList<ILifecycleMappingElementKey>(proposals.keySet());

    assertEquals("test-packaging-a", ((PackagingTypeMappingConfiguration.Key) elementKeys.get(0)).getPackaging());
    assertInstallCatalogItemProposal(items.get(0), proposals.get(elementKeys.get(0)));

    assertEquals("test-goal-1", ((MojoExecutionMappingConfiguration.Key) elementKeys.get(1)).getExecution().getGoal());
    assertInstallCatalogItemProposal(items.get(1), proposals.get(elementKeys.get(1)));

    assertEquals("test-goal-2", ((MojoExecutionMappingConfiguration.Key) elementKeys.get(2)).getExecution().getGoal());
    assertInstallCatalogItemProposal(items.get(1), proposals.get(elementKeys.get(2)));
  }

  public void testPreselectedProposals() throws Exception {
    LifecycleMappingConfiguration configuration = loadMappingConfiguration(new File(
        "projects/discovery/mojoExecutions/pom.xml"));

    MavenDiscoveryService srv = new MavenDiscoveryService();
    CatalogItem item = new CatalogItem();
    LifecycleMappingMetadataSource metadataSource = readLifecycleMappingMetadata("projects/discovery/match-test-goal-1-and-2.xml");
    srv.addCatalogItem(item, metadataSource);

    ProjectLifecycleMappingConfiguration project = configuration.getProjects().get(0);
    Map<ILifecycleMappingElementKey, List<IMavenDiscoveryProposal>> proposalsmap = srv.discover(
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

    ProjectLifecycleMappingConfiguration project = configuration.getProjects().get(0);
    Map<ILifecycleMappingElementKey, List<IMavenDiscoveryProposal>> allproposals = new LinkedHashMap<ILifecycleMappingElementKey, List<IMavenDiscoveryProposal>>();
    allproposals.put(project.getPackagingTypeMappingConfiguration().getLifecycleMappingElementKey(),
        Arrays.asList(proposal));
    for(MojoExecutionMappingConfiguration mojoExecution : project.getMojoExecutionConfigurations()) {
      allproposals.put(mojoExecution.getLifecycleMappingElementKey(), Arrays.asList(proposal));
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

    ProjectLifecycleMappingConfiguration project = configuration.getProjects().get(0);
    Map<ILifecycleMappingElementKey, List<IMavenDiscoveryProposal>> proposals = srv.discover(project.getMavenProject(),
        project.getMojoExecutions(), new ArrayList<IMavenDiscoveryProposal>(), monitor);

    assertEquals(3, proposals.size());

    configuration.setProposals(proposals);

    // sanity check
    assertEquals(0, configuration.getSelectedProposals().size());

    assertTrue(configuration.autoCompleteMapping());

    List<ILifecycleMappingElementKey> elementKeys = new ArrayList<ILifecycleMappingElementKey>(proposals.keySet());

    PackagingTypeMappingConfiguration.Key packagingType = (PackagingTypeMappingConfiguration.Key) elementKeys.get(0);
    MojoExecutionMappingConfiguration.Key goal1 = (MojoExecutionMappingConfiguration.Key) elementKeys.get(1);
    MojoExecutionMappingConfiguration.Key goal2 = (MojoExecutionMappingConfiguration.Key) elementKeys.get(2);

    assertEquals("test-packaging-a", packagingType.getPackaging());
    assertEquals("test-goal-1", goal1.getExecution().getGoal());
    assertEquals("test-goal-2", goal2.getExecution().getGoal());

    assertNull(configuration.getSelectedProposal(packagingType)); // packaging type is mapped by default, make sure we don't override the mapping
    assertSame(items.get(1),
        ((InstallCatalogItemMavenDiscoveryProposal) configuration.getSelectedProposal(goal1)).getCatalogItem());
    assertSame(items.get(1),
        ((InstallCatalogItemMavenDiscoveryProposal) configuration.getSelectedProposal(goal2)).getCatalogItem());

  }

  public void testAutomaticSelectionWithAmbiguousProposals() throws Exception {
    LifecycleMappingConfiguration configuration = loadMappingConfiguration(new File(
        "projects/discovery/twoMojoExecutions/pom.xml"));

    MavenDiscoveryService srv = new MavenDiscoveryService();

    // can't choose between match and another-match automatically
    List<CatalogItem> items = new ArrayList<CatalogItem>();
    items.add(newCatalogItem(srv, "projects/discovery/match-test-goal-1.xml"));
    items.add(newCatalogItem(srv, "projects/discovery/another-match-test-goal-1.xml"));

    ProjectLifecycleMappingConfiguration project = configuration.getProjects().get(0);
    Map<ILifecycleMappingElementKey, List<IMavenDiscoveryProposal>> proposals = srv.discover(project.getMavenProject(),
        project.getMojoExecutions(), new ArrayList<IMavenDiscoveryProposal>(), monitor);

    assertEquals(1, proposals.size());

    configuration.setProposals(proposals);

    // sanity check
    assertFalse(configuration.isMappingComplete());
    assertEquals(0, configuration.getSelectedProposals().size());

    configuration.autoCompleteMapping();

    List<ILifecycleMappingElementKey> elementKeys = new ArrayList<ILifecycleMappingElementKey>(proposals.keySet());

    MojoExecutionMappingConfiguration.Key goal1 = (MojoExecutionMappingConfiguration.Key) elementKeys.get(0);

    assertEquals("test-goal-1", goal1.getExecution().getGoal());

    assertNull(configuration.getSelectedProposal(goal1));
  }
}
