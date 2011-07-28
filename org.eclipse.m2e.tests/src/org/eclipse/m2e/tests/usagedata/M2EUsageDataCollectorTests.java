
package org.eclipse.m2e.tests.usagedata;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.epp.usagedata.internal.gathering.events.UsageDataEvent;
import org.eclipse.epp.usagedata.internal.gathering.services.UsageDataService;

import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.internal.udc.MavenUsageDataCollectorActivator;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;


public class M2EUsageDataCollectorTests extends AbstractMavenProjectTestCase {

  private TestUsageDataService usageDataService;

  public void setUp() throws Exception {
    super.setUp();
    usageDataService = new TestUsageDataService();

    Thread.sleep(1000); // TODO this sucks, need a better way to plug test recorder
    MavenUsageDataCollectorActivator.getDefault().setUsageDataService(usageDataService);
  }

  public void tearDown() throws Exception {
    usageDataService = null;
    super.tearDown();
  }

  public void testPackagingEvent() throws Exception {
    importProject("projects/usagedatacollection/simple/pom.xml");
    waitForJobsToComplete();
    assertContains("Missing packaging type event", "pom", "m2e.packaging", "pom", "org.eclipse.m2e.core",
        MavenPluginActivator.getVersion());
  }

  public void testPluginEvent() throws Exception {
    importProject("projects/usagedatacollection/withplugins/pom.xml");
    waitForJobsToComplete();

    assertContains("Plugin Event", "clean:default-clean:clean", "m2e.plugins",
        "org.apache.maven.plugins:maven-clean-plugin:2.4.1", "org.eclipse.m2e.core", MavenPluginActivator.getVersion());

  }

  private void assertContains(String message, String what, String kind, String description, String bundleId,
      String bundleVersion) {
    StringBuilder e = new StringBuilder();
    for(UsageDataEvent event : usageDataService.events) {
      if(event.what.equals(what) && event.kind.equals(kind) && event.description.equals(description)
          && event.bundleId.equals(bundleId) && event.bundleVersion.equals(bundleVersion)) {
        return;
      }
      e.append(toString(event)).append("\n");
    }
    fail(message + ": " + e.toString());
  }

  private static String toString(UsageDataEvent event) {
    return new StringBuilder().append(event.what).append(',').append(event.kind).append(',').append(event.description)
        .append(',').append(event.bundleId).append(',').append(event.bundleVersion).toString();
  }

  protected static class TestUsageDataService extends UsageDataService {
    List<UsageDataEvent> events = new ArrayList<UsageDataEvent>();

    public void recordEvent(String what, String kind, String description, String bundleId) {
      recordEvent(what, kind, description, bundleId, null);
    }

    public void recordEvent(String what, String kind, String description, String bundleId, String bundleVersion) {
      UsageDataEvent event = new UsageDataEvent(what, kind, description, bundleId, bundleVersion,
          System.currentTimeMillis());
      events.add(event);
    }
  }
}
