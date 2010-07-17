/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.perftests;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.plexus.util.FileUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.test.internal.performance.InternalDimensions;
import org.eclipse.test.internal.performance.InternalPerformanceMeter;
import org.eclipse.test.internal.performance.PerformanceTestPlugin;
import org.eclipse.test.internal.performance.db.DB;
import org.eclipse.test.internal.performance.eval.AssertChecker;
import org.eclipse.test.internal.performance.eval.Evaluator;
import org.eclipse.test.internal.performance.eval.RelativeBandChecker;
import org.eclipse.test.performance.Performance;
import org.eclipse.test.performance.PerformanceMeter;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.core.MavenConsole;
import org.maven.ide.eclipse.embedder.MavenModelManager;
import org.maven.ide.eclipse.project.IMavenProjectChangedListener;
import org.maven.ide.eclipse.project.IMavenProjectFacade;
import org.maven.ide.eclipse.project.IMavenProjectImportResult;
import org.maven.ide.eclipse.project.IProjectConfigurationManager;
import org.maven.ide.eclipse.project.LocalProjectScanner;
import org.maven.ide.eclipse.project.MavenProjectChangedEvent;
import org.maven.ide.eclipse.project.MavenProjectInfo;
import org.maven.ide.eclipse.project.MavenProjectManager;
import org.maven.ide.eclipse.project.MavenUpdateRequest;
import org.maven.ide.eclipse.project.ProjectImportConfiguration;
import org.maven.ide.eclipse.tests.common.AbstractMavenProjectTestCase;
import org.maven.ide.eclipse.tests.common.WorkspaceHelpers;


/**
 * @author igor
 */
public class ImportPerformanceTest extends AbstractMavenProjectTestCase {

  private static final int EXECUTION_COUNT = 5;

  protected PerformanceMeter fPerformanceMeter;

  private IProjectConfigurationManager configurationManager;

  private MavenModelManager modelManager;

  private MavenConsole console;

  private MavenProjectManager projectManager;

  private List<MavenProjectChangedEvent> events = new ArrayList<MavenProjectChangedEvent>();

  private IMavenProjectChangedListener mavenProjectChangeListener = new IMavenProjectChangedListener() {
    public void mavenProjectChanged(MavenProjectChangedEvent[] events, IProgressMonitor monitor) {
      ImportPerformanceTest.this.events.addAll(Arrays.asList(events));
    }
  };

  protected void setUp() throws Exception {
    super.setUp();

    MavenPlugin mavenPlugin = MavenPlugin.getDefault();
    configurationManager = mavenPlugin.getProjectConfigurationManager();
    modelManager = mavenPlugin.getMavenModelManager();
    console = mavenPlugin.getConsole();
    projectManager = mavenPlugin.getMavenProjectManager();

    Performance performance = Performance.getDefault();
    fPerformanceMeter = performance.createPerformanceMeter(performance.getDefaultScenarioId(this));

    projectManager.addMavenProjectChangedListener(mavenProjectChangeListener);
  }

  protected void tearDown() throws Exception {
    projectManager.removeMavenProjectChangedListener(mavenProjectChangeListener);
    fPerformanceMeter.dispose();
    super.tearDown();
  }

  protected void startMeasuring() {
    fPerformanceMeter.start();
  }

  protected void stopMeasuring() {
    fPerformanceMeter.stop();
  }

  protected void commitMeasurements() {
    fPerformanceMeter.commit();
  }

  protected void assertPerformance(AssertChecker... assertCheckers) {
    Evaluator e = new Evaluator();
    e.setAssertCheckers(assertCheckers);
    e.evaluate(fPerformanceMeter);

    String scenarioName = ((InternalPerformanceMeter) fPerformanceMeter).getScenarioName();

    @SuppressWarnings("rawtypes")
    Map failures = DB.queryFailure(scenarioName, PerformanceTestPlugin.getVariations());

    if(failures != null) {
      assertTrue(failures.values().toString(), failures.isEmpty());
    }
  }

  protected void assertPerformance() {
    assertPerformance(//
        new RelativeBandChecker(InternalDimensions.CPU_TIME, 0.8f, 1.1f), //
        new RelativeBandChecker(InternalDimensions.RCHAR, 0.8f, 1.1f), //
        new RelativeBandChecker(InternalDimensions.WCHAR, 0.8f, 1.1f)//
    );
  }

  public void testImportEmptyWorkspace() throws Exception {
    // warm up
    System.out.println(getName() + "#warmup");
    List<IMavenProjectImportResult> results = new ArrayList<IMavenProjectImportResult>();
    results.addAll(importProjects(getBasedir("p001/libs", "libs")));
    results.addAll(importProjects(getBasedir("p001/core", "core")));

    // sanity check
    assertEquals(34 + 128, results.size());
    for(IMavenProjectImportResult result : results) {
      assertNoErrors(result.getProject());
    }

    for(int i = 0; i < EXECUTION_COUNT; i++ ) {
      System.out.println(getName() + "#" + i);

      // workspace cleanup
      WorkspaceHelpers.cleanWorkspace();
      waitForJobsToComplete();
      events.clear();

      File libs = getBasedir("p001/libs", "libs");
      File core = getBasedir("p001/core", "core");

      startMeasuring();
      results = new ArrayList<IMavenProjectImportResult>();
      results.addAll(importProjects(libs));
      results.addAll(importProjects(core));
      stopMeasuring();

      // sanity check
      assertEquals(34 + 128, results.size());
      for(IMavenProjectImportResult result : results) {
        assertNoErrors(result.getProject());
      }
      assertEquals(34 + 128, events.size());
    }

    commitMeasurements();
    assertPerformance();
  }

  public void testUpdateDependencies() throws Exception {
    System.out.println(getName() + "#setup");
    importProjects(getBasedir("p001/libs", "libs"));
    importProjects(getBasedir("p001/core", "core"));

    IMavenProjectFacade testee = projectManager.getMavenProject("p001.core", "module107", "0.0.1-SNAPSHOT");

    System.out.println(getName() + "#warmup");
    MavenUpdateRequest request = new MavenUpdateRequest(false, false);
    request.addPomFile(testee.getPom());
    projectManager.refresh(request, monitor);

    // sanity check
    IMavenProjectFacade[] projects = projectManager.getProjects();
    assertEquals(34 + 128, projects.length);
    for(IMavenProjectFacade facade : projects) {
      assertNoErrors(facade.getProject());
    }

    for(int i = 0; i < EXECUTION_COUNT; i++ ) {
      System.out.println(getName() + "#" + i);

      events.clear();

      startMeasuring();
      workspace.run(new IWorkspaceRunnable() {
        public void run(IProgressMonitor monitor) throws CoreException {
          IMavenProjectFacade testee = projectManager.getMavenProject("p001.core", "module107", "0.0.1-SNAPSHOT");
          IProject project = testee.getProject();
          try {
            InputStream contents = project.getFile("pom_with_new_dependency.xml").getContents();
            try {
              IFile file = project.getFile("pom.xml");
              if(!file.exists()) {
                file.create(contents, IResource.FORCE, monitor);
              } else {
                file.setContents(contents, IResource.FORCE, monitor);
              }
            } finally {
              contents.close();
            }
          } catch(IOException e) {
            throw new CoreException(new Status(IStatus.ERROR, "pluginId", "Interrupted", e));
          }
          MavenUpdateRequest request = new MavenUpdateRequest(false, false);
          request.addPomFile(testee.getPom());
          projectManager.refresh(request, monitor);
        }
      }, monitor);
      stopMeasuring();

      // sanity check
      projects = projectManager.getProjects();
      assertEquals(34 + 128, projects.length);
      for(IMavenProjectFacade facade : projects) {
        assertNoErrors(facade.getProject());
      }

      assertEquals(62, events.size());
    }

    commitMeasurements();
    assertPerformance();
  }

  private List<IMavenProjectImportResult> importProjects(final File dir) throws CoreException {
    final List<IMavenProjectImportResult> results = new ArrayList<IMavenProjectImportResult>();
    workspace.run(new IWorkspaceRunnable() {
      public void run(IProgressMonitor monitor) throws CoreException {
        ProjectImportConfiguration configuration = new ProjectImportConfiguration();

        LocalProjectScanner scanner = new LocalProjectScanner(workspace.getRoot().getLocation().toFile(), dir
            .getAbsolutePath(), false, modelManager, console);
        try {
          scanner.run(monitor);
        } catch(InterruptedException e) {
          throw new CoreException(new Status(IStatus.ERROR, "pluginId", "Interrupted", e));
        }

        Set<MavenProjectInfo> projectInfos = configurationManager.collectProjects(scanner.getProjects(), false);

        results.addAll(configurationManager.importProjects(projectInfos, configuration, monitor));
      }
    }, plugin.getProjectConfigurationManager().getRule(), IWorkspace.AVOID_UPDATE, monitor);
    return results;
  }

  private File getBasedir(String src, String name) throws IOException {
    File dir = new File("target", name).getCanonicalFile();
    FileUtils.deleteDirectory(dir);
    FileUtils.copyDirectoryStructure(new File("projects/", src), dir);
    return dir;
  }
}
