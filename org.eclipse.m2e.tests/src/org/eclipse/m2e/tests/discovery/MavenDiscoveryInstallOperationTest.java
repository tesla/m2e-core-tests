
package org.eclipse.m2e.tests.discovery;

import java.util.Collections;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogItem;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;

import org.eclipse.m2e.internal.discovery.operation.MavenDiscoveryInstallOperation;
import org.eclipse.m2e.tests.common.HttpServer;


@SuppressWarnings("restriction")
public class MavenDiscoveryInstallOperationTest extends AbstractDiscoveryTest {

  private HttpServer httpServer;

  private static final IProgressMonitor monitor = new NullProgressMonitor();

  public void setUp() throws Exception {
    super.setUp();
    httpServer = new HttpServer();
    httpServer.addResources("/", "resources/p2discoveryRepo");
    httpServer.setHttpPort(10123);
    httpServer.start();
    updateMavenCatalog();
  }

  public void tearDown() throws Exception {
    try {
      if(httpServer != null) {
        httpServer.stop();
      }
    } finally {
      super.tearDown();
    }
  }

  public void testMatchNoVersion() throws Exception {
    MavenDiscoveryInstallOperation operation = new MavenDiscoveryInstallOperation(
        Collections.singletonList(getCatalogItem("iu.with.no.version")), null, false, true, null);
    IInstallableUnit[] ius = operation.computeInstallableUnits(monitor);
    assertEquals("# IUs", 1, ius.length);
    assertEquals("IU id", "test.iu", ius[0].getId());
    assertEquals("IU version", Version.parseVersion("2.0.0.201102231450"), ius[0].getVersion());
  }

  public void testMatchHighestVersion() throws Exception {
    MavenDiscoveryInstallOperation operation = new MavenDiscoveryInstallOperation(
        Collections.singletonList(getCatalogItem("iu.with.highest.version")), null, false, true, null);
    IInstallableUnit[] ius = operation.computeInstallableUnits(monitor);
    assertEquals("# IUs", 1, ius.length);
    assertEquals("IU id", "test.iu", ius[0].getId());
    assertEquals("IU version", Version.parseVersion("2.0.0.201102231450"), ius[0].getVersion());
  }

  public void testMatchLowestVersion() throws Exception {
    MavenDiscoveryInstallOperation operation = new MavenDiscoveryInstallOperation(
        Collections.singletonList(getCatalogItem("iu.with.lowest.version")), null, false, true, null);
    IInstallableUnit[] ius = operation.computeInstallableUnits(monitor);
    assertEquals("# IUs", 1, ius.length);
    assertEquals("IU id", "test.iu", ius[0].getId());
    assertEquals("IU version", Version.parseVersion("1.0.0.201102231450"), ius[0].getVersion());
  }

  public void testMissingRepository() throws Exception {
    MavenDiscoveryInstallOperation operation = new MavenDiscoveryInstallOperation(
        Collections.singletonList(getCatalogItem("missing.repository")), null, false, true, null);
    try {
      operation.computeInstallableUnits(monitor);
    } catch(CoreException e) {
      IStatus status = e.getStatus();
      assertEquals("Status Message", "Error(s) occurred gathering items for installation", status.getMessage());
      assertEquals("Children Status", 2, status.getChildren().length);
      assertEquals("Child Status Message", "No repository found at http://localhost:10123/missingrepository/.",
          status.getChildren()[0].getMessage());
      assertEquals("Child Status Message",
          "Error installing MissingRepository contacting repository http://localhost:10123/missingrepository/",
          status.getChildren()[1].getMessage());
      return;
    }
    fail("Expected CoreException");
  }

  public void testMissingIU() throws Exception {
    MavenDiscoveryInstallOperation operation = new MavenDiscoveryInstallOperation(
        Collections.singletonList(getCatalogItem("missing.iu")), null, false, true, null);
    try {
      operation.computeInstallableUnits(monitor);
    } catch(CoreException e) {
      IStatus status = e.getStatus();
      assertEquals("Status Message", "Error(s) occurred gathering items for installation", status.getMessage());
      assertEquals("Children Status", 1, status.getChildren().length);
      assertEquals("Child Status Message",
          "Error installing MissingIU unable to locate installable unit missing.iu/1.0.0.201102231450",
          status.getChildren()[0].getMessage());
      return;
    }
    fail("Expected CoreException");
  }

  private CatalogItem getCatalogItem(String id) {
    for(CatalogItem item : catalog.getItems()) {
      if(item.getId().equals(id)) {
        return item;
      }
    }
    throw new IllegalArgumentException("Failed to find CatalogItem with id: " + id);
  }
}
