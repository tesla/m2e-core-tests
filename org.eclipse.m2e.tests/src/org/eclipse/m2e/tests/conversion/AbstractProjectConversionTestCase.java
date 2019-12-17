/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Red Hat, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.tests.conversion;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import org.junit.Assert;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.codehaus.plexus.util.IOUtil;

import org.apache.maven.model.Model;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.MavenModelManager;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;


/**
 * AbstractProjectConversionTestCase
 * 
 * @author Fred Bricon
 */
public abstract class AbstractProjectConversionTestCase extends AbstractMavenProjectTestCase {

  /**
   * Instanciates a new default Maven Model, using the projectName as groupId and artifactId, having a default version
   * of 0.0.1-SNAPSHOT.
   */
  protected Model initDefaultModel(String projectName) {
    Model model = new Model();
    model.setModelVersion("4.0.0"); //$NON-NLS-1$
    model.setArtifactId(projectName);
    model.setGroupId(projectName);
    model.setVersion("0.0.1-SNAPSHOT");//$NON-NLS-1$
    return model;
  }

  /**
   * Converts an Eclipse project to a Maven project (generates a pom.xm and enables the Maven nature)
   */
  protected void convert(IProject project) throws CoreException, InterruptedException {
    Model model = initDefaultModel(project.getName());
    convert(project, model);
  }

  protected void convert(IProject project, Model model) throws CoreException, InterruptedException {
    IProgressMonitor monitor = new NullProgressMonitor();
    MavenPlugin.getProjectConversionManager().convert(project, model, monitor);
    createPomXml(project, model);
    ResolverConfiguration configuration = new ResolverConfiguration();
    MavenPlugin.getProjectConfigurationManager().enableMavenNature(project, configuration, monitor);
    waitForJobsToComplete(monitor);
  }

  /**
   * Serializes the maven model to &lt;project&gt;/pom.xml
   */
  protected void createPomXml(IProject project, Model model) throws CoreException {
    MavenModelManager mavenModelManager = MavenPlugin.getMavenModelManager();
    mavenModelManager.createMavenModel(project.getFile(IMavenConstants.POM_FILE_NAME), model);
  }

  /**
   * Asserts the contents of the file is identical to the expectedFile
   */
  public void assertFileContentEquals(String message, IFile expectedFile, IFile file) throws Exception {
    Assert.assertEquals(message, getAsString(expectedFile), getAsString(file));
  }

  /**
   * Asserts the generated pom.xml is identical to &lt;project&gt;/expectedPom.xml
   */
  protected void verifyGeneratedPom(IProject project) throws Exception {
    assertFileContentEquals("pom.xml comparison failed", project.getFile("expectedPom.xml"),
        project.getFile(IMavenConstants.POM_FILE_NAME));
  }

  protected String getAsString(IFile file) throws IOException, CoreException {
    StringWriter sw = new StringWriter();
    InputStream is = null;
    try {
      is = file.getContents();
      IOUtil.copy(is, sw, "UTF-8");
    } finally {
      IOUtil.close(is);
    }
    return sw.toString().replaceAll("\r\n", "\n");
  }

}
