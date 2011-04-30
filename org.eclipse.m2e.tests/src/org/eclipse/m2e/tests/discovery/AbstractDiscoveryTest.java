package org.eclipse.m2e.tests.discovery;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;

import junit.framework.TestCase;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.discovery.Catalog;
import org.eclipse.equinox.internal.p2.discovery.DiscoveryCore;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.IShellProvider;

import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;
import org.eclipse.m2e.internal.discovery.MavenDiscovery;
import org.eclipse.m2e.internal.discovery.wizards.MavenCatalogConfiguration;
import org.eclipse.m2e.internal.discovery.wizards.MavenCatalogViewer;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.internal.Workbench;

public abstract class AbstractDiscoveryTest extends TestCase implements IShellProvider {

	protected Catalog catalog;
	protected MavenCatalogConfiguration configuration;
	private Shell shell;

	@Override
	public void setUp() throws Exception {
		catalog = new Catalog();
		catalog.setEnvironment(DiscoveryCore.createEnvironment());
		catalog.setVerifyUpdateSiteAvailability(false);
		catalog.getDiscoveryStrategies().add(new TestM2EBundleStrategy());

		// Build the list of tags to show in the Wizard header
		catalog.setTags(Collections.singletonList(MavenDiscovery.APPLICABLE_TAG));

		// Create configuration for the catalog
		configuration = new MavenCatalogConfiguration();
		configuration.setShowTagFilter(true);
		configuration.setSelectedTags(Collections.singletonList(MavenDiscovery.APPLICABLE_TAG));
		configuration.setShowInstalledFilter(false);
    configuration.setSelectedPackagingTypes(Collections.<String>emptyList());
    configuration.setSelectedMojos(Collections.<MojoExecutionKey>emptyList());
    configuration.setSelectedLifecycleIds(Collections.<String>emptyList());
    configuration.setSelectedConfigurators(Collections.<String>emptyList());

		shell = new Shell(Workbench.getInstance().getDisplay());
	}

	public void tearDown() throws Exception {
		shell.dispose();
		shell = null;
	}

	protected void updateMavenCatalog() {
		MavenCatalogViewer mcv = new MavenCatalogViewer(catalog, this, new RunnableContext(), configuration);
		mcv.createControl(shell);
		mcv.updateCatalog();
	}

	private static class RunnableContext implements IRunnableContext {
		public RunnableContext() {
		}

		public void run(boolean fork, boolean cancelable, IRunnableWithProgress runnable) throws InvocationTargetException, InterruptedException {
			runnable.run(new NullProgressMonitor());
		}
	}

	public Shell getShell() {
		return shell;
	}
}
