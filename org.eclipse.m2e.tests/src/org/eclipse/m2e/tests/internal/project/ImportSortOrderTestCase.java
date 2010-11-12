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

package org.eclipse.m2e.tests.internal.project;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.internal.embedder.MavenImpl;
import org.eclipse.m2e.core.internal.project.ProjectConfigurationManager;
import org.eclipse.m2e.core.internal.project.registry.ProjectRegistryManager;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenUpdateRequest;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;

public class ImportSortOrderTestCase extends AbstractMavenProjectTestCase {
  
  protected IProject createProject(String projectName, String projectLocation) throws CoreException {
    IProject project = super.createProject(projectName, projectLocation);
    AbstractProjectConfigurator.addNature(project, IMavenConstants.NATURE_ID, monitor);
    return project;
  }

  private  List<IMavenProjectFacade> createFacades() throws Exception {

    List<IMavenProjectFacade> facades =  new ArrayList<IMavenProjectFacade>();
    
    ProjectRegistryManager manager = new ProjectRegistryManager((MavenImpl) plugin.getMaven(), plugin.getConsole(), null, false,
        plugin.getMavenMarkerManager());
    
    List<IProject> projects = new ArrayList<IProject>();
    
    projects.add(createProject("Pos", "projects/MNGECLIPSE-1028/pom.xml"));
    projects.add(createProject("PosServers", "projects/MNGECLIPSE-1028/PosServers/pom.xml"));
    projects.add(createProject("Order", "projects/MNGECLIPSE-1028/Order/pom.xml"));
    projects.add(createProject("OrderEJB", "projects/MNGECLIPSE-1028/Order/OrderEJB/pom.xml"));
    projects.add(createProject("Warehouse", "projects/MNGECLIPSE-1028/Warehouse/pom.xml"));
    projects.add(createProject("WarehouseEJB", "projects/MNGECLIPSE-1028/Warehouse/WarehouseEJB/pom.xml"));
    projects.add(createProject("WarehouseWeb", "projects/MNGECLIPSE-1028/Warehouse/WarehouseWeb/pom.xml"));
    projects.add(createProject("PosJavaEE5Ear", "projects/MNGECLIPSE-1028/PosJavaEE5Ear/pom.xml"));
    projects.add(createProject("PosGateway", "projects/MNGECLIPSE-1028/PosGateway/pom.xml"));
    projects.add(createProject("PosGatewayJavaEE5EJB", "projects/MNGECLIPSE-1028/PosGateway/PosGatewayJavaEE5EJB/pom.xml"));
    projects.add(createProject("PosGatewayWeb", "projects/MNGECLIPSE-1028/PosGateway/PosGatewayWeb/pom.xml"));
    projects.add(createProject("Client", "projects/MNGECLIPSE-1028/Client/pom.xml"));
    projects.add(createProject("ClientSwing", "projects/MNGECLIPSE-1028/Client/ClientSwing/pom.xml"));
    projects.add(createProject("ClientWeb", "projects/MNGECLIPSE-1028/Client/ClientWeb/pom.xml"));
    projects.add(createProject("PosConfig", "projects/MNGECLIPSE-1028/PosConfig/pom.xml"));
    projects.add(createProject("PosConfigJar", "projects/MNGECLIPSE-1028/PosConfig/PosConfigJar/pom.xml"));
    projects.add(createProject("PosConfigWeb", "projects/MNGECLIPSE-1028/PosConfig/PosConfigWeb/pom.xml"));
    
   
    MavenUpdateRequest updateRequest = new MavenUpdateRequest(false, false);
    for (IProject project : projects) {
      updateRequest.addPomFile(project);
    }

    manager.refresh(updateRequest, monitor);

    for(IProject project:projects) {
      facades.add(manager.create(project, monitor));
    }
    
    return facades;
  }
  
  public void testCollectionSortProject() throws Exception {
    ProjectConfigurationManager manager = (ProjectConfigurationManager) plugin.getProjectConfigurationManager();
    
    List<IMavenProjectFacade> facades = createFacades();
    manager.sortProjects(facades, monitor);

    Map<String, Integer> aMap = new HashMap<String, Integer>();    
    for(int i=0; i<facades.size(); i++) {
      aMap.put(facades.get(i).getArtifactKey().getArtifactId(), new Integer(i));
    }
    assertResultMap(aMap);
  }


  private void assertResultMap(Map<String, Integer> aMap) {
    //Pos
    assertTrue(aMap.get("Pos").intValue() < aMap.get("Client").intValue()); //Client
    assertTrue(aMap.get("Pos").intValue() < aMap.get("Order").intValue()); //Order
    assertTrue(aMap.get("Pos").intValue() < aMap.get("PosConfig").intValue()); //PosConfig
    assertTrue(aMap.get("Pos").intValue() < aMap.get("PosGateway").intValue()); //PosGateway
    assertTrue(aMap.get("Pos").intValue() < aMap.get("PosJavaEE5Ear").intValue()); //PosJavaEE5Ear
    assertTrue(aMap.get("Pos").intValue() < aMap.get("PosServers").intValue()); //PosServers
    assertTrue(aMap.get("Pos").intValue() < aMap.get("Warehouse").intValue()); //Warehouse
    
    //Client
    assertTrue(aMap.get("Client").intValue() < aMap.get("ClientSwing").intValue()); //ClientSwing
    assertTrue(aMap.get("Client").intValue() < aMap.get("ClientWeb").intValue()); //Clientweb
    
    //ClientSwing
    assertTrue(aMap.get("ClientSwing").intValue() > aMap.get("PosConfigJar").intValue()); //PosConfigJar
    assertTrue(aMap.get("ClientSwing").intValue() > aMap.get("OrderEJB").intValue()); //OrderEJB
    
    //ClientWeb
    assertTrue(aMap.get("ClientWeb").intValue() > aMap.get("PosConfigJar").intValue()); //PosConfigJar
    assertTrue(aMap.get("ClientWeb").intValue() > aMap.get("OrderEJB").intValue()); //OrderEJB
    assertTrue(aMap.get("ClientWeb").intValue() > aMap.get("WarehouseEJB").intValue()); //WarehouseEJB
    
    //Order
    assertTrue(aMap.get("Order").intValue() < aMap.get("OrderEJB").intValue()); //OrderEJB
    
    //OrderEJB
    assertTrue(aMap.get("OrderEJB").intValue() > aMap.get("PosConfigJar").intValue()); //PosConfigJar
    assertTrue(aMap.get("OrderEJB").intValue() > aMap.get("WarehouseEJB").intValue()); //WarehouseEJB
    
    //PosConfig
    assertTrue(aMap.get("PosConfig").intValue() < aMap.get("PosConfigJar").intValue()); //PosConfigJar
    assertTrue(aMap.get("PosConfig").intValue() < aMap.get("PosConfigWeb").intValue()); //PosConfigJar
    
    //PosConfigJar
    
    //PosConfigWeb
    assertTrue(aMap.get("PosConfigWeb").intValue() > aMap.get("PosConfigJar").intValue()); //PosConfigJar
    
    //PosGateway
    assertTrue(aMap.get("PosGateway").intValue() < aMap.get("PosGatewayJavaEE5EJB").intValue()); //PosGatewayJavaEE5EJB
    assertTrue(aMap.get("PosGateway").intValue() < aMap.get("PosGatewayWeb").intValue()); //PosGatewayWeb
    
    //PosGatewayJavaEE5EJB
    assertTrue(aMap.get("PosGatewayJavaEE5EJB").intValue() > aMap.get("PosConfigJar").intValue()); //PosConfigJar
    assertTrue(aMap.get("PosGatewayJavaEE5EJB").intValue() > aMap.get("WarehouseEJB").intValue()); //WarehouseEJB
    
    //PosGatewayWeb
    assertTrue(aMap.get("PosGatewayWeb").intValue() > aMap.get("PosConfigJar").intValue()); //PosConfigJar
    assertTrue(aMap.get("PosGatewayWeb").intValue() > aMap.get("OrderEJB").intValue()); //OrderEJB
    assertTrue(aMap.get("PosGatewayWeb").intValue() > aMap.get("WarehouseEJB").intValue()); //WarehouseEJB

    //PosJavaEE5Ear
    assertTrue(aMap.get("PosJavaEE5Ear").intValue() > aMap.get("PosConfigJar").intValue()); //PosConfigJar
    assertTrue(aMap.get("PosJavaEE5Ear").intValue() > aMap.get("PosConfigWeb").intValue()); //PosConfigWeb
    assertTrue(aMap.get("PosJavaEE5Ear").intValue() > aMap.get("OrderEJB").intValue()); //OrderEJB
    assertTrue(aMap.get("PosJavaEE5Ear").intValue() > aMap.get("WarehouseEJB").intValue()); //WarehouseEJB
    assertTrue(aMap.get("PosJavaEE5Ear").intValue() > aMap.get("Client").intValue()); //ClientWeb
    assertTrue(aMap.get("PosJavaEE5Ear").intValue() > aMap.get("WarehouseWeb").intValue()); //WarehouseWeb
    assertTrue(aMap.get("PosJavaEE5Ear").intValue() > aMap.get("PosGatewayJavaEE5EJB").intValue()); //PosGatewayJavaEE5EJB
    assertTrue(aMap.get("PosJavaEE5Ear").intValue() > aMap.get("PosGatewayWeb").intValue()); //PosGatewayWeb
    
    //PosServers
    
    //Warehouse
    assertTrue(aMap.get("Warehouse").intValue() < aMap.get("WarehouseEJB").intValue()); //WarehouseEJB
    assertTrue(aMap.get("Warehouse").intValue() < aMap.get("WarehouseWeb").intValue()); //WarehouseWeb
    
    //WarehouseEJB
    assertTrue(aMap.get("WarehouseEJB").intValue() > aMap.get("PosConfigJar").intValue()); //PosConfigJar
    
    //WarehouseWeb
    assertTrue(aMap.get("WarehouseWeb").intValue() > aMap.get("PosConfigJar").intValue()); //PosConfigJar
    assertTrue(aMap.get("WarehouseWeb").intValue() > aMap.get("WarehouseEJB").intValue()); //WarehouseEJB
    
  }
 }
