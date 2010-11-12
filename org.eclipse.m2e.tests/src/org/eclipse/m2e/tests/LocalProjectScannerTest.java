/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/


package org.eclipse.m2e.tests;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.core.MavenConsole;
import org.eclipse.m2e.core.embedder.MavenModelManager;
import org.eclipse.m2e.core.project.LocalProjectScanner;
import org.eclipse.m2e.core.project.MavenProjectInfo;


public class LocalProjectScannerTest extends TestCase {

  private MavenModelManager modelManager;

  private MavenConsole console;

  protected void setUp() throws Exception {
    super.setUp();

    MavenPlugin mavenPlugin = MavenPlugin.getDefault();
    modelManager = mavenPlugin.getMavenModelManager();
    console = mavenPlugin.getConsole();
  }

  public void testDeepNesting() throws Exception {
    File baseDir = new File("projects/localprojectscanner/deepnesting/parent").getCanonicalFile();

    LocalProjectScanner scanner = new LocalProjectScanner(baseDir, baseDir.getAbsolutePath(), false, modelManager,
        console);
    scanner.run(new NullProgressMonitor());

    List<MavenProjectInfo> projects = scanner.getProjects();
    assertEquals(1, projects.size());

    MavenProjectInfo parent = projects.get(0);
    assertEquals("/pom.xml", parent.getLabel());
    assertEquals(new File(baseDir, "pom.xml"), parent.getPomFile());

    List<MavenProjectInfo> modules = new ArrayList<MavenProjectInfo>(parent.getProjects());
    assertEquals(1, modules.size());

    MavenProjectInfo module = modules.get(0);
    assertEquals("module/pom.xml", module.getLabel());
    assertEquals(new File(baseDir, "module/pom.xml"), module.getPomFile());

    List<MavenProjectInfo> submodules = new ArrayList<MavenProjectInfo>(module.getProjects());
    assertEquals(1, submodules.size());

    MavenProjectInfo submodule = submodules.get(0);
    assertEquals("submodule/pom.xml", submodule.getLabel());
    assertEquals(new File(baseDir, "module/submodule/pom.xml"), submodule.getPomFile());
  }

  public void testDeepNesting002() throws Exception {
    File baseDir = new File("projects/localprojectscanner/deepnesting/parent").getCanonicalFile();

    LocalProjectScanner scanner = new LocalProjectScanner(baseDir, baseDir.getParentFile().getAbsolutePath(), false,
        modelManager, console);
    scanner.run(new NullProgressMonitor());

    List<MavenProjectInfo> projects = scanner.getProjects();
    assertEquals(1, projects.size());

    MavenProjectInfo parent = projects.get(0);
    assertEquals("/pom.xml", parent.getLabel());
    assertEquals(new File(baseDir, "pom.xml"), parent.getPomFile());

    List<MavenProjectInfo> modules = new ArrayList<MavenProjectInfo>(parent.getProjects());
    assertEquals(1, modules.size());

    MavenProjectInfo module = modules.get(0);
    assertEquals("module/pom.xml", module.getLabel());
    assertEquals(new File(baseDir, "module/pom.xml"), module.getPomFile());

    List<MavenProjectInfo> submodules = new ArrayList<MavenProjectInfo>(module.getProjects());
    assertEquals(1, submodules.size());

    MavenProjectInfo submodule = submodules.get(0);
    assertEquals("submodule/pom.xml", submodule.getLabel());
    assertEquals(new File(baseDir, "module/submodule/pom.xml"), submodule.getPomFile());
  }

  public void testModuleCorrelationInverse() throws Exception {
    /*
     * Currently, we do NOT correlate modules to "top-level" project.
     * This is not a desired behaviour, but a limitation of the implementation.
     */
    File baseDir = new File("projects/localprojectscanner/modulecorrelation/parent").getCanonicalFile();

    List<String> folders = new ArrayList<String>();
    folders.add(new File(baseDir, "submodule").getAbsolutePath());
    folders.add(new File(baseDir, "module").getAbsolutePath());

    LocalProjectScanner scanner = new LocalProjectScanner(baseDir, folders, false, modelManager, console);
    scanner.run(new NullProgressMonitor());

    List<MavenProjectInfo> projects = scanner.getProjects();
    assertEquals(2, projects.size());

    MavenProjectInfo submodule = projects.get(0);
    assertEquals("/pom.xml", submodule.getLabel());
    assertEquals(new File(baseDir, "submodule/pom.xml").getCanonicalFile(), submodule.getPomFile().getCanonicalFile());

    MavenProjectInfo module = projects.get(1);
    assertEquals("/pom.xml", module.getLabel());
    assertEquals(new File(baseDir, "module/pom.xml").getCanonicalFile(), module.getPomFile().getCanonicalFile());

    // inverse order gives better result
    folders = new ArrayList<String>();
    folders.add(new File(baseDir, "module").getAbsolutePath());
    folders.add(new File(baseDir, "submodule").getAbsolutePath());

    scanner = new LocalProjectScanner(baseDir, folders, false, modelManager, console);
    scanner.run(new NullProgressMonitor());

    projects = scanner.getProjects();
    assertEquals(1, projects.size());

    module = projects.get(0);
    assertEquals("/pom.xml", module.getLabel());
    assertEquals(new File(baseDir, "module/pom.xml").getCanonicalFile(), module.getPomFile().getCanonicalFile());
  }

  public void testMNGECLIPSE614_ImportModulesOutsideOfParent() throws Exception {
    File baseDir = new File("projects/localprojectscanner/MNGECLIPSE-614/very-important-parent").getCanonicalFile();

    LocalProjectScanner scanner = new LocalProjectScanner(baseDir, baseDir.getAbsolutePath(), false, modelManager,
        console);
    scanner.run(new NullProgressMonitor());
    List<MavenProjectInfo> projects = scanner.getProjects();

    assertEquals(1, projects.size());

    MavenProjectInfo parent = projects.get(0);
    assertEquals("/pom.xml", parent.getLabel());
    assertEquals(new File(baseDir, "pom.xml"), parent.getPomFile());

    List<MavenProjectInfo> modules = new ArrayList<MavenProjectInfo>(parent.getProjects());
    assertEquals(1, modules.size());

    MavenProjectInfo module = modules.get(0);
    assertEquals("../module/pom.xml", module.getLabel());
    assertEquals(new File(baseDir, "../module/pom.xml").getCanonicalFile(), module.getPomFile());
  }

  public void testCircleRefs() throws Exception {
    File baseDir = new File("projects/localprojectscanner/circlerefs").getCanonicalFile();

    LocalProjectScanner scanner = new LocalProjectScanner(baseDir, baseDir.getAbsolutePath(), false, modelManager,
        console);
    scanner.run(new NullProgressMonitor());

    List<MavenProjectInfo> projects = scanner.getProjects();

    assertEquals(1, projects.size());
  }

  public void testRenameInWorkspace() throws Exception {
    File baseDir = new File("projects/localprojectscanner/rename/mavenNNNNNNNN").getCanonicalFile();

    LocalProjectScanner scanner = new LocalProjectScanner(baseDir.getParentFile().getCanonicalFile(), //
        baseDir.getAbsolutePath(), true, modelManager, console);
    scanner.run(new NullProgressMonitor());

    List<MavenProjectInfo> projects = scanner.getProjects();

    MavenProjectInfo project = projects.get(0);
    assertEquals(MavenProjectInfo.RENAME_REQUIRED, project.getBasedirRename());
  }

  public void testNoMetadata() throws Exception {
    File baseDir = new File("projects/localprojectscanner/nometadata").getCanonicalFile();

    LocalProjectScanner scanner = new LocalProjectScanner(baseDir, baseDir.getAbsolutePath(), false, modelManager,
        console);
    scanner.run(new NullProgressMonitor());
    //.metadata folder shouldn't be scanned, hence the project hidden inside shouldn't be found 
    List<MavenProjectInfo> projects = scanner.getProjects();

    assertEquals(1, projects.size());
  }

}
