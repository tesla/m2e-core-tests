
package org.eclipse.m2e.tests.discovery;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
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

    CatalogItem[] items = new CatalogItem[] {new CatalogItem(), new CatalogItem()};

    items[0].setSiteUrl("0");
    items[0].setInstallableUnits(Arrays.asList("0"));
    items[1].setSiteUrl("1");
    items[1].setInstallableUnits(Arrays.asList("1"));

    srv.addCatalogItem(items[0], readLifecycleMappingMetadata("projects/discovery/match-test-packaging-a.xml"));
    srv.addCatalogItem(items[1], readLifecycleMappingMetadata("projects/discovery/match-test-goal-1-and-2.xml"));

    ProjectLifecycleMappingConfiguration project = configuration.getProjects().get(0);
    Map<ILifecycleMappingElementKey, List<IMavenDiscoveryProposal>> proposals = srv.discover(project.getMavenProject(),
        project.getMojoExecutions(), new ArrayList<IMavenDiscoveryProposal>(), monitor);

    assertEquals(3, proposals.size());

    List<ILifecycleMappingElementKey> elementKeys = new ArrayList<ILifecycleMappingElementKey>(proposals.keySet());

    assertEquals("test-packaging-a", ((PackagingTypeMappingConfiguration.Key) elementKeys.get(0)).getPackaging());
    assertInstallCatalogItemProposal(items[0], proposals.get(elementKeys.get(0)));

    assertEquals("test-goal-1", ((MojoExecutionMappingConfiguration.Key) elementKeys.get(1)).getExecution().getGoal());
    assertInstallCatalogItemProposal(items[1], proposals.get(elementKeys.get(1)));

    assertEquals("test-goal-2", ((MojoExecutionMappingConfiguration.Key) elementKeys.get(2)).getExecution().getGoal());
    assertInstallCatalogItemProposal(items[1], proposals.get(elementKeys.get(2)));
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
}
