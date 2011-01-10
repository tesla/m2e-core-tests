
package org.eclipse.m2e.discovery.tests;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.discovery.AbstractDiscoveryStrategy;
import org.eclipse.equinox.internal.p2.discovery.Catalog;
import org.eclipse.equinox.internal.p2.discovery.DiscoveryCore;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogCategory;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogItem;
import org.eclipse.equinox.internal.p2.discovery.model.Tag;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.m2e.discovery.MavenDiscovery;
import org.eclipse.m2e.discovery.wizards.MavenCatalogConfiguration;
import org.eclipse.m2e.discovery.wizards.MavenCatalogViewer;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.internal.Workbench;
import org.junit.Test;


@SuppressWarnings("restriction")
public class MavenDiscoveryTest extends TestCase implements IShellProvider {

  private static final String ID_B = "com.example.item.b";

  private static final String ID_A = "com.example.item.a";

  private static final String CAT_ID = "category.1";

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
    catalog.getDiscoveryStrategies().add(new MockDiscoveryStrategy());

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

    shell = new Shell(Workbench.getInstance().getDisplay());
  }

  public void tearDown() throws Exception {
    shell.dispose();
    shell = null;
  }

  @Test
  public void testCatalogUnchecked() {
    configuration.setShowInstalledFilter(false);

    MavenCatalogViewer mcv = new MavenCatalogViewer(catalog, this, new RunnableContext(), configuration);
    mcv.createControl(shell);
    mcv.updateCatalog();
    assertEquals("Catalog entries missing", 2, catalog.getItems().size());
    assertFalse("Catalog entries should not be selected", catalog.getItems().get(0).isSelected());
    assertFalse("CatalogItem B should not have Applicable tag",
        catalog.getItems().get(0).hasTag(MavenDiscovery.APPLICABLE_TAG));

    assertFalse("Catalog entries should not be selected", catalog.getItems().get(1).isSelected());
    assertFalse("CatalogItem B should not have Applicable tag",
        catalog.getItems().get(1).hasTag(MavenDiscovery.APPLICABLE_TAG));
  }

  @Test
  public void testCatalogApplicableById() {
    configuration.setShowInstalledFilter(false);
    configuration.setPreselectedIds(Arrays.asList(new String[] {ID_A}));

    MavenCatalogViewer mcv = new MavenCatalogViewer(catalog, this, new RunnableContext(), configuration);
    mcv.createControl(shell);
    mcv.updateCatalog();
    assertEquals("Catalog entries missing", 2, catalog.getItems().size());
    assertTrue("CatalogItem A should be selected", catalog.getItems().get(0).isSelected());
    assertTrue("CatalogItem A should have Applicable tag",
        catalog.getItems().get(0).hasTag(MavenDiscovery.APPLICABLE_TAG));

    assertFalse("CatalogItem B should not be selected", catalog.getItems().get(1).isSelected());
    assertFalse("CatalogItem B should not have Applicable tag",
        catalog.getItems().get(1).hasTag(MavenDiscovery.APPLICABLE_TAG));
  }

  @Test
  public void testCatalogApplicableByTag() {
    configuration.setShowInstalledFilter(false);
    configuration.setPreselectedTags(Arrays.asList(new String[] {TAG_TWO.getValue()}));

    MavenCatalogViewer mcv = new MavenCatalogViewer(catalog, this, new RunnableContext(), configuration);
    mcv.createControl(shell);
    mcv.updateCatalog();
    assertEquals("Catalog entries missing", 2, catalog.getItems().size());
    assertFalse("CatalogItem A should not be selected", catalog.getItems().get(0).isSelected());
    assertFalse("CatalogItem A should not have Applicable tag",
        catalog.getItems().get(0).hasTag(MavenDiscovery.APPLICABLE_TAG));

    assertTrue("CatalogItem B should be selected", catalog.getItems().get(1).isSelected());
    assertTrue("CatalogItem B should have Applicable tag",
        catalog.getItems().get(1).hasTag(MavenDiscovery.APPLICABLE_TAG));
  }

  @Test
  public void testCatalogApplicableByBoth() {
    configuration.setShowInstalledFilter(false);
    configuration.setPreselectedIds(Arrays.asList(new String[] {ID_A}));
    configuration.setPreselectedTags(Arrays.asList(new String[] {TAG_TWO.getValue()}));

    MavenCatalogViewer mcv = new MavenCatalogViewer(catalog, this, new RunnableContext(), configuration);
    mcv.createControl(shell);
    mcv.updateCatalog();
    assertEquals("Catalog entries missing", 2, catalog.getItems().size());
    assertTrue("CatalogItem A should be selected", catalog.getItems().get(0).isSelected());
    assertTrue("CatalogItem A should have Applicable tag",
        catalog.getItems().get(0).hasTag(MavenDiscovery.APPLICABLE_TAG));

    assertTrue("CatalogItem B should be selected", catalog.getItems().get(1).isSelected());
    assertTrue("CatalogItem B should have Applicable tag",
        catalog.getItems().get(1).hasTag(MavenDiscovery.APPLICABLE_TAG));
  }

  private static class RunnableContext implements IRunnableContext {
    public RunnableContext() {
    }

    public void run(boolean fork, boolean cancelable, IRunnableWithProgress runnable) throws InvocationTargetException,
        InterruptedException {
      runnable.run(new NullProgressMonitor());
    }
  }

  private static class MockDiscoveryStrategy extends AbstractDiscoveryStrategy {

    public MockDiscoveryStrategy() {
      // TODO Auto-generated constructor stub
    }

    @Override
    public void performDiscovery(IProgressMonitor monitor) {
      CatalogCategory category = new CatalogCategory();
      category.setId(CAT_ID);
      category.setName("Category");

      getCategories().add(category);
      getItems().add(createItem("Item A", ID_A, MavenDiscoveryTest.TAG_ONE));
      getItems().add(createItem("Item B", ID_B, MavenDiscoveryTest.TAG_TWO));
      category.getItems().addAll(getItems());
    }

    static CatalogItem createItem(String name, String id, Tag tag) {
      CatalogItem item = new CatalogItem();
      item.setName(name);
      item.setId(id);
      item.addTag(tag);
      item.setDescription("Description");
      item.setCategoryId(CAT_ID);
      return item;
    }
  }

  public Shell getShell() {
    return shell;
  }
}
