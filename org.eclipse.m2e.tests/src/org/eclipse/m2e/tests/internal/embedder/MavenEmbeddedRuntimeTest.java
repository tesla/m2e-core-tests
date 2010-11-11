package org.eclipse.m2e.tests.internal.embedder;

import junit.framework.TestCase;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.MavenRuntime;
import org.eclipse.m2e.core.embedder.MavenRuntimeManager;
import org.eclipse.m2e.core.internal.Messages;


@SuppressWarnings("restriction")
public class MavenEmbeddedRuntimeTest extends TestCase {
  public void testGetVersion() throws Exception {
    MavenRuntimeManager runtimeManager = MavenPlugin.getDefault().getMavenRuntimeManager();
    MavenRuntime embeddedRuntime = runtimeManager.getRuntime(MavenRuntimeManager.EMBEDDED);
    String mavenVersion = embeddedRuntime.getVersion();
    assertNotNull(mavenVersion);
    assertNotSame("", mavenVersion);
    assertNotSame(Messages.MavenEmbeddedRuntime_unknown, mavenVersion);
  }
}
