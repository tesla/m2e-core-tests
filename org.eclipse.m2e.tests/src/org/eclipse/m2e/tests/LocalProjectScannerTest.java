/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.MavenModelManager;
import org.eclipse.m2e.core.project.LocalProjectScanner;
import org.eclipse.m2e.core.project.MavenProjectInfo;


public class LocalProjectScannerTest {

  private MavenModelManager modelManager;

  @Before
  public void setUp() throws Exception {

    modelManager = MavenPlugin.getMavenModelManager();
  }

  @Test
  public void testDeepNesting() throws Exception {
    File baseDir = new File("projects/localprojectscanner/deepnesting/parent").getCanonicalFile();

    LocalProjectScanner scanner = new LocalProjectScanner(List.of(baseDir.getAbsolutePath()), false, modelManager);
    scanner.run(new NullProgressMonitor());

    List<MavenProjectInfo> projects = scanner.getProjects();
    assertEquals(1, projects.size());

    MavenProjectInfo parent = projects.get(0);
    assertEquals("/pom.xml", parent.getLabel());
    assertEquals(new File(baseDir, "pom.xml"), parent.getPomFile());

    List<MavenProjectInfo> modules = new ArrayList<>(parent.getProjects());
    assertEquals(1, modules.size());

    MavenProjectInfo module = modules.get(0);
    assertEquals("module/pom.xml", module.getLabel());
    assertEquals(new File(baseDir, "module/pom.xml"), module.getPomFile());

    List<MavenProjectInfo> submodules = new ArrayList<>(module.getProjects());
    assertEquals(1, submodules.size());

    MavenProjectInfo submodule = submodules.get(0);
    assertEquals("submodule/pom.xml", submodule.getLabel());
    assertEquals(new File(baseDir, "module/submodule/pom.xml"), submodule.getPomFile());
  }

  /**
   * the modules can be referenced either by <module>path</module> or by <module>path/pom.xml</module>
   * 
   * @throws Exception
   */
  @Test
  public void testModuleReferencedByFile() throws Exception {
    File baseDir = new File("projects/localprojectscanner/module_referenced_by_file/parent").getCanonicalFile();

    LocalProjectScanner scanner = new LocalProjectScanner(List.of(baseDir.getAbsolutePath()), false, modelManager);
    scanner.run(new NullProgressMonitor());

    List<MavenProjectInfo> projects = scanner.getProjects();
    assertEquals(1, projects.size());

    MavenProjectInfo parent = projects.get(0);
    assertEquals("/pom.xml", parent.getLabel());
    assertEquals(new File(baseDir, "pom.xml"), parent.getPomFile());

    List<MavenProjectInfo> modules = new ArrayList<>(parent.getProjects());
    assertEquals(1, modules.size());

    MavenProjectInfo module = modules.get(0);
    assertEquals("module/pom.xml", module.getLabel());
    assertEquals(new File(baseDir, "module/pom.xml"), module.getPomFile());

  }

  /**
   * when scanning a directory without a pom file, iterate the children recursively to find poms, but never include
   * projects inside projects if not referenced via <modules>. Preventive measure against including test poms and
   * traversing the tree very deep.
   * 
   * @throws Exception
   */
  @Test
  public void testSkipNested() throws Exception {
    File baseDir = new File("projects/localprojectscanner/skip_nested").getCanonicalFile();

    LocalProjectScanner scanner = new LocalProjectScanner(List.of(baseDir.getAbsolutePath()), false, modelManager);
    scanner.run(new NullProgressMonitor());

    Map<File, MavenProjectInfo> projects = toPathMap(scanner.getProjects());
    assertEquals(3, projects.size());

    //the point of asserts here is to verify the to_skip/pom.xml ones didn't get in
    assertTrue(projects.containsKey(new File(baseDir, "aparent/pom.xml")));
    assertTrue(projects.containsKey(new File(baseDir, "module/pom.xml")));
    assertTrue(projects.containsKey(new File(baseDir, "module/submodule/pom.xml")));
  }

  private Map<File, MavenProjectInfo> toPathMap(List<MavenProjectInfo> projects) throws IOException {
    SortedMap<File, MavenProjectInfo> result = new TreeMap<>();
    addPathMap(result, projects);
    return result;
  }

  void addPathMap(Map<File, MavenProjectInfo> result, Collection<MavenProjectInfo> projects) throws IOException {
    for(MavenProjectInfo project : projects) {
      File canonicalPath = project.getPomFile().getCanonicalFile();
      if(!result.containsKey(canonicalPath)) {
        result.put(canonicalPath, project);
        addPathMap(result, project.getProjects());
      }
    }
  }

  @Test
  public void testDeepNesting002() throws Exception {
    File baseDir = new File("projects/localprojectscanner/deepnesting/parent").getCanonicalFile();

    LocalProjectScanner scanner = new LocalProjectScanner(List.of(baseDir.getParentFile().getAbsolutePath()), false,
        modelManager);
    scanner.run(new NullProgressMonitor());

    List<MavenProjectInfo> projects = scanner.getProjects();
    assertEquals(1, projects.size());

    MavenProjectInfo parent = projects.get(0);
    assertEquals("/parent/pom.xml", parent.getLabel());
    assertEquals(new File(baseDir, "pom.xml"), parent.getPomFile());

    List<MavenProjectInfo> modules = new ArrayList<>(parent.getProjects());
    assertEquals(1, modules.size());

    MavenProjectInfo module = modules.get(0);
    assertEquals("module/pom.xml", module.getLabel());
    assertEquals(new File(baseDir, "module/pom.xml"), module.getPomFile());

    List<MavenProjectInfo> submodules = new ArrayList<>(module.getProjects());
    assertEquals(1, submodules.size());

    MavenProjectInfo submodule = submodules.get(0);
    assertEquals("submodule/pom.xml", submodule.getLabel());
    assertEquals(new File(baseDir, "module/submodule/pom.xml"), submodule.getPomFile());
  }

  @Test
  public void testModuleCorrelationInverse() throws Exception {
    /*
     * Currently, we do NOT correlate modules to "top-level" project.
     * This is not a desired behaviour, but a limitation of the implementation.
     */
    File baseDir = new File("projects/localprojectscanner/modulecorrelation/parent").getCanonicalFile();

    List<String> folders = new ArrayList<>();
    folders.add(new File(baseDir, "submodule").getAbsolutePath());
    folders.add(new File(baseDir, "module").getAbsolutePath());

    LocalProjectScanner scanner = new LocalProjectScanner(folders, false, modelManager);
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
    folders = new ArrayList<>();
    folders.add(new File(baseDir, "module").getAbsolutePath());
    folders.add(new File(baseDir, "submodule").getAbsolutePath());

    scanner = new LocalProjectScanner(folders, false, modelManager);
    scanner.run(new NullProgressMonitor());

    projects = scanner.getProjects();
    assertEquals(1, projects.size());

    module = projects.get(0);
    assertEquals("/pom.xml", module.getLabel());
    assertEquals(new File(baseDir, "module/pom.xml").getCanonicalFile(), module.getPomFile().getCanonicalFile());
  }

  @Test
  public void testMNGECLIPSE614_ImportModulesOutsideOfParent() throws Exception {
    File baseDir = new File("projects/localprojectscanner/MNGECLIPSE-614/very-important-parent").getCanonicalFile();

    LocalProjectScanner scanner = new LocalProjectScanner(List.of(baseDir.getAbsolutePath()), false, modelManager);
    scanner.run(new NullProgressMonitor());
    List<MavenProjectInfo> projects = scanner.getProjects();

    assertEquals(1, projects.size());

    MavenProjectInfo parent = projects.get(0);
    assertEquals("/pom.xml", parent.getLabel());
    assertEquals(new File(baseDir, "pom.xml"), parent.getPomFile());

    List<MavenProjectInfo> modules = new ArrayList<>(parent.getProjects());
    assertEquals(1, modules.size());

    MavenProjectInfo module = modules.get(0);
    assertEquals("../module/pom.xml", module.getLabel());
    assertEquals(new File(baseDir, "../module/pom.xml").getCanonicalFile(), module.getPomFile());
  }

  @Test
  public void testCircleRefs() throws Exception {
    File baseDir = new File("projects/localprojectscanner/circlerefs").getCanonicalFile();

    LocalProjectScanner scanner = new LocalProjectScanner(List.of(baseDir.getAbsolutePath()), false, modelManager);
    scanner.run(new NullProgressMonitor());

    List<MavenProjectInfo> projects = scanner.getProjects();

    assertEquals(1, projects.size());
  }

  @Test
  public void testRenameInWorkspace() throws Exception {
    File baseDir = new File("projects/localprojectscanner/rename/mavenNNNNNNNN").getCanonicalFile();

    LocalProjectScanner scanner = new LocalProjectScanner(List.of(baseDir.getAbsolutePath()), true, modelManager);
    scanner.run(new NullProgressMonitor());

    List<MavenProjectInfo> projects = scanner.getProjects();

    MavenProjectInfo project = projects.get(0);
    assertEquals(MavenProjectInfo.RENAME_REQUIRED, project.getBasedirRename());

    //Non of the sub modules should be renamed
    for(MavenProjectInfo subProject : project.getProjects()) {
      assertEquals(MavenProjectInfo.RENAME_NO, subProject.getBasedirRename());
    }

  }

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  @Test
  public void testStackOverflow() throws Exception {
    File tempDirectory = folder.newFolder("testlink");
    File d1 = new File(tempDirectory, "d1");
    File d2 = new File(tempDirectory, "d2");
    d1.mkdirs();
    d2.mkdirs();
    File d1link = new File(d1, "d2");
    File d2link = new File(d2, "d1");
    createSymbolicLink(d1link, d2);
    createSymbolicLink(d2link, d1);

    LocalProjectScanner scanner = new LocalProjectScanner(List.of(tempDirectory.toString()),
        false, modelManager);
    scanner.run(new NullProgressMonitor());
    List<MavenProjectInfo> projects = scanner.getProjects();
    assertEquals(0, projects.size());
  }

  private void createSymbolicLink(File link, File target) throws IOException, AssertionError {
    try {
      Files.createSymbolicLink(link.toPath(), target.toPath());
    } catch(FileSystemException e) {
      throw new AssertionError(
          "Creation of symbolic-link failed. On Windows administrator privileges are required to create a symbolic-link.",
          e);
    }
  }

  @Test
  public void testNoMetadata() throws Exception {
    File baseDir = new File("projects/localprojectscanner/nometadata").getCanonicalFile();

    LocalProjectScanner scanner = new LocalProjectScanner(List.of(baseDir.getAbsolutePath()), false, modelManager);
    scanner.run(new NullProgressMonitor());
    //.metadata folder shouldn't be scanned, hence the project hidden inside shouldn't be found 
    List<MavenProjectInfo> projects = scanner.getProjects();

    assertEquals(1, projects.size());
  }

  @Test
  public void testOutsideWorkspace() throws Exception {

    File baseDir = new File("projects/localprojectscanner/341038_projectsOutsideWorkspaceNotRenamed")
        .getCanonicalFile();

    LocalProjectScanner scanner = new LocalProjectScanner(List.of(baseDir.getAbsolutePath()), true,
        modelManager);
    scanner.run(new NullProgressMonitor());

    List<MavenProjectInfo> projects = scanner.getProjects();
    MavenProjectInfo project = projects.get(0);

    //The base directory should be renamed
    assertEquals(MavenProjectInfo.RENAME_REQUIRED, project.getBasedirRename());

    //Non of the sub modules should be renamed
    for(MavenProjectInfo subProject : project.getProjects()) {
      assertEquals(MavenProjectInfo.RENAME_NO, subProject.getBasedirRename());
    }

  }

}
