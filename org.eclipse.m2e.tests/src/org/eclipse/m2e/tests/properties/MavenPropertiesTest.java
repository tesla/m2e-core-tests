/*******************************************************************************
 * Copyright (c) 2016 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      RedHat, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.tests.properties;

import static org.junit.Assert.assertEquals;

import java.util.Properties;

import org.junit.Test;

import org.eclipse.core.resources.IProject;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.internal.project.ResolverConfigurationIO;
import org.eclipse.m2e.core.internal.project.registry.ProjectRegistryManager;
import org.eclipse.m2e.core.project.IProjectConfiguration;
import org.eclipse.m2e.core.project.IProjectConfigurationManager;
import org.eclipse.m2e.core.project.MavenUpdateRequest;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.eclipse.m2e.tests.common.WorkspaceHelpers;


/**
 * MavenPropertiesTest
 *
 * @author rawagner
 */
public class MavenPropertiesTest extends AbstractMavenProjectTestCase {

  private IProject project;

  private ProjectRegistryManager manager;

  private IProjectConfigurationManager configurationManager;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    manager = MavenPluginActivator.getDefault().getMavenProjectManagerImpl();
    configurationManager = MavenPlugin.getProjectConfigurationManager();
  }

  @Test
  public void testBuildingProjectWithProperties() throws Exception {
    project = importProject("projects/properties_project/pom.xml");
    importProject("projects/properties_project/dependency/pom.xml");
    waitForJobsToComplete();
    WorkspaceHelpers.assertNoErrors(project);

    Properties newp = new Properties();
    newp.setProperty("my_prop1", "non_existing_version");

    setPropertiesToResolver(newp);

    buildProject();
    WorkspaceHelpers.assertErrorMarker("org.eclipse.m2e.core.maven2Problem.dependency",
        "Missing artifact propertiesGrDep:propertiesArDep:jar:non_existing_version", 16, project);

    setPropertiesToResolver(null);

    buildProject();
    WorkspaceHelpers.assertNoErrors(project);
  }

  @Test
  public void testReadResolverConfigurationProperties() throws Exception {
    project = createExisting("propertiesArDep", "projects/properties_project/dependency/");

    Properties testProperties = new Properties();
    testProperties.put("key1", "value1");
    testProperties.put("key2", "value2|");
    testProperties.put("key3", "");
    testProperties.put("key4", "&#33|");
    testProperties.put("key5", "<![CDATA[value|||]]>");
    testProperties.put("key6", "<![CDATA[value>]]>");
    testProperties.put("key7", "value%7C");

    IProjectConfiguration conf = ResolverConfigurationIO.readResolverConfiguration(project);
    Properties properties = new Properties();
    properties.putAll(conf.getConfigurationProperties());
    assertEquals(testProperties, properties);
  }

  private void buildProject() throws Exception {
    ResolverConfiguration resolverConfiguration = new ResolverConfiguration(
        manager.getProject(project).getConfiguration());
    configurationManager.setResolverConfiguration(project, resolverConfiguration);

    MavenUpdateRequest request = new MavenUpdateRequest(project, false, false);
    projectRefreshJob.refresh(request);
    waitForJobsToComplete();
  }

  private void setPropertiesToResolver(Properties properties) {
    ResolverConfiguration resolverConfiguration = new ResolverConfiguration(
        manager.getProject(project).getConfiguration());
    resolverConfiguration.setProperties(properties);
    configurationManager.updateProjectConfiguration(project, resolverConfiguration, null);
  }

}
