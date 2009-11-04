/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.editor.lifecycle.custom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.maven.ide.components.pom.Configuration;
import org.maven.ide.components.pom.Model;
import org.maven.ide.components.pom.Plugin;
import org.maven.ide.eclipse.editor.lifecycle.ILifecycleMappingEditorContribution;
import org.maven.ide.eclipse.editor.lifecycle.LifecycleEditorUtils;
import org.maven.ide.eclipse.editor.lifecycle.MojoExecutionData;
import org.maven.ide.eclipse.editor.pom.MavenPomEditor;
import org.maven.ide.eclipse.internal.ExtensionReader;
import org.maven.ide.eclipse.project.IMavenProjectFacade;
import org.maven.ide.eclipse.project.configurator.AbstractLifecycleMapping;
import org.maven.ide.eclipse.project.configurator.AbstractProjectConfigurator;
import org.maven.ide.eclipse.project.configurator.ILifecycleMapping;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

public class CustomLifecycleMappingEditorContribution implements ILifecycleMappingEditorContribution {
  private IMavenProjectFacade projectFacade = null;
  private Plugin lifecycleMappingPlugin = null;
  private MavenPomEditor pomEditor = null;
  
  public void setSiteData(MavenPomEditor editor, IMavenProjectFacade project, Model pom) {
    lifecycleMappingPlugin = LifecycleEditorUtils.getOrCreateLifecycleMappingPlugin(pom);
    projectFacade = project;
    pomEditor = editor;
  }
  
  public void initializeConfiguration()  throws CoreException {
    String[] lifecycleNames;
    String[] lifecycleIds;
    Map<String, ILifecycleMapping> mappings = new HashMap<String, ILifecycleMapping>(ExtensionReader.readLifecycleMappingExtensions());
    lifecycleNames = new String[mappings.size()];
    lifecycleIds = new String[mappings.size()];
    int i = 0;
    for(Map.Entry<String, ILifecycleMapping> mapping : mappings.entrySet()) {
      lifecycleIds[i] = mapping.getKey();
      lifecycleNames[i] = mapping.getValue().getName();
      i++;
    }
    CustomLifecycleParamsDialog dialog = new CustomLifecycleParamsDialog(pomEditor.getSite().getShell(), lifecycleIds, lifecycleNames);
    dialog.setBlockOnOpen(true);
    dialog.open();
    String templateId = dialog.getSelectedTemplate();
    
    Element configNode = (Element)lifecycleMappingPlugin.getConfiguration().getConfigurationNode();
    while(configNode.hasChildNodes()) {
      configNode.removeChild(configNode.getFirstChild());
    }
    lifecycleMappingPlugin.getConfiguration().setStringValue("mappingId", "customizable");
    Element configuratorsElement = configNode.getOwnerDocument().createElement("configurators");
    configNode.appendChild(configuratorsElement);
    
    if(templateId != null) {
      ILifecycleMapping mapping = mappings.get(templateId);
      for(AbstractProjectConfigurator configer : mapping.getProjectConfigurators(projectFacade, new NullProgressMonitor())) {
        if(!configer.isGeneric()) {
          Element configuratorElement = configNode.getOwnerDocument().createElement("configurator");
          configuratorElement.setAttribute("id", configer.getId());
          configuratorsElement.appendChild(configuratorElement);
        }
      }
    }
    
    Element mojoExecutionsElement = configNode.getOwnerDocument().createElement("mojoExecutions");
    configNode.appendChild(mojoExecutionsElement);
    if(templateId != null) {
      ILifecycleMapping mapping = mappings.get(templateId);
      List<String> allExecutions = mapping.getPotentialMojoExecutionsForBuildKind(projectFacade, IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
      List<String> incrExecutions = mapping.getPotentialMojoExecutionsForBuildKind(projectFacade, IncrementalProjectBuilder.INCREMENTAL_BUILD, new NullProgressMonitor());
      for(String execution : allExecutions) {
        Element mojoExecutionElement = configNode.getOwnerDocument().createElement("mojoExecution");
        if(incrExecutions.contains(execution)) {
          mojoExecutionElement.setAttribute("runOnIncremental", "true");
        } else {
          mojoExecutionElement.setAttribute("runOnIncremental", "false");
        }
        Text t = configNode.getOwnerDocument().createTextNode(execution);
        mojoExecutionElement.appendChild(t);
        mojoExecutionsElement.appendChild(mojoExecutionElement);
      }
    }
  }
  
  public List<AbstractProjectConfigurator> getProjectConfigurators() throws CoreException {
    Map<String, AbstractProjectConfigurator> configuratorsMap = new LinkedHashMap<String, AbstractProjectConfigurator>();
    for(AbstractProjectConfigurator configurator : AbstractLifecycleMapping.getProjectConfigurators()) {
      configuratorsMap.put(configurator.getId(), configurator);
    }

    Configuration config = lifecycleMappingPlugin.getConfiguration();

    Element configuratorsDom = getChildElement((Element)config.getConfigurationNode(), "configurators");

    List<AbstractProjectConfigurator> configurators = new ArrayList<AbstractProjectConfigurator>();
    
    if (configuratorsDom != null) {
      for(Element configuratorDom : getChildren(configuratorsDom, "configurator")) {
        String configuratorId = configuratorDom.getAttribute("id");
        AbstractProjectConfigurator configurator = configuratorsMap.get(configuratorId);
        if(configurator == null) {
          throw new IllegalArgumentException("Unknown configurator id=" + configuratorId);
        }

        configurators.add(configurator);
      }
    }
    return configurators;
  }
  
  
  
  public boolean canAddProjectConfigurator() throws CoreException {
    return true;
  }
  
  public boolean canEditProjectConfigurator(AbstractProjectConfigurator configurator) {
    return false;
  }
  
  public boolean canRemoveProjectConfigurator(AbstractProjectConfigurator configurator) {
    return true;
  }
  
  public void addProjectConfigurator() {
    List<AbstractProjectConfigurator> allConfigurators = new ArrayList<AbstractProjectConfigurator>(AbstractLifecycleMapping.getProjectConfigurators());
    Configuration config = lifecycleMappingPlugin.getConfiguration();

    Element configuratorsDom = getChildElement((Element)config.getConfigurationNode(), "configurators");
    if(null != configuratorsDom) {
      Set<String> usedIds = new LinkedHashSet<String>();
      for(Element configuratorDom : getChildren(configuratorsDom, "configurator")) {
        usedIds.add(configuratorDom.getAttribute("id"));
      }
      
      Iterator<AbstractProjectConfigurator> confItr = allConfigurators.iterator();
      while(confItr.hasNext()) {
        if(usedIds.contains(confItr.next().getId())) {
          confItr.remove();
        }
      }
    }
    
    if(!allConfigurators.isEmpty()) {
    
      ConfiguratorSelectionDialog dialog = new ConfiguratorSelectionDialog(pomEditor.getSite().getShell(), allConfigurators);
      dialog.setBlockOnOpen(true);
      dialog.open();
      String selection = dialog.getSelectedConfigurator();
      if(selection != null) {
        if(null == configuratorsDom) {
          configuratorsDom = config.getConfigurationNode().getOwnerDocument().createElement("configurators");
          config.getConfigurationNode().appendChild(configuratorsDom);
        }
        
        Element configuratorDom = configuratorsDom.getOwnerDocument().createElement("configurator");
        configuratorDom.setAttribute("id", selection);
        configuratorsDom.appendChild(configuratorDom);
      }
    }

  }
  
  public void editProjectConfigurator(AbstractProjectConfigurator configurator) throws CoreException { }
  
  public void removeProjectConfigurator(AbstractProjectConfigurator configurator) throws CoreException {
    Configuration config = lifecycleMappingPlugin.getConfiguration();

    Element configuratorsDom = getChildElement((Element)config.getConfigurationNode(), "configurators");

    if (configuratorsDom != null) {
      for(Element configuratorDom : getChildren(configuratorsDom, "configurator")) {
        String configuratorId = configuratorDom.getAttribute("id");
        if(configuratorId != null && configuratorId.equals(configurator.getId())) {
          configuratorsDom.removeChild(configuratorDom);
          break;
        }
      }
    }
  }
  
  public List<MojoExecutionData> getMojoExecutions() throws CoreException {
    Configuration config = lifecycleMappingPlugin.getConfiguration();
    Element configNode = (Element) config.getConfigurationNode();
    
    Element executionsDom = getChildElement(configNode, "mojoExecutions");

    List<MojoExecutionData> mojos = new LinkedList<MojoExecutionData>();
    
    if (executionsDom != null) {
      for(Element execution : getChildren(executionsDom, "mojoExecution")) {
        String strRunOnIncremental = execution.getAttribute("runOnIncremental");
        String name = getNodeContents(execution);
        mojos.add(new MojoExecutionData(name, name, true, toBool(strRunOnIncremental, true)));
      }
    }
    
    return mojos;
  }
  
  public boolean canEnableMojoExecution(MojoExecutionData execution) throws CoreException {
    for(MojoExecutionData mojo : getMojoExecutions()) {
      if(mojo.getId().equals(execution.getId())) {
        return false;
      }
    }
    return true;
  }
  
  public boolean canDisableMojoExecution(MojoExecutionData execution) throws CoreException {
    for(MojoExecutionData mojo : getMojoExecutions()) {
      if(mojo.getId().equals(execution.getId())) {
        return true;
      }
    }
    return false;
  }
  
  public void enableMojoExecution(MojoExecutionData execution) throws CoreException {
    Configuration config = lifecycleMappingPlugin.getConfiguration();
    Element configNode = (Element) config.getConfigurationNode();
    
    Element executionsDom = getChildElement(configNode, "mojoExecutions");

    if(null == executionsDom) {
      executionsDom = configNode.getOwnerDocument().createElement("mojoExecutions");
      configNode.appendChild(executionsDom);
    }
    
    Element executionDom = configNode.getOwnerDocument().createElement("mojoExecution");
    executionDom.setAttribute("runOnIncremental", "" + execution.isRunOnIncrementalBuild());
    executionDom.appendChild(configNode.getOwnerDocument().createTextNode(execution.getId()));
    executionsDom.appendChild(executionDom);
  }
  
  public void disableMojoExecution(MojoExecutionData execution) throws CoreException {
    Configuration config = lifecycleMappingPlugin.getConfiguration();
    Element configNode = (Element) config.getConfigurationNode();
    
    Element executionsDom = getChildElement(configNode, "mojoExecutions");

    if(null == executionsDom) {
      return;
    }
    
    for(Element executionDom : getChildren(executionsDom, "mojoExecution")) {
      String name = getNodeContents(executionDom);
      if(name.equals(execution.getId())) {
        executionsDom.removeChild(executionDom);
        break;
      }
    }
  }
  
  public boolean canSetIncremental(MojoExecutionData execution) throws CoreException {
    return true;
  }
  
  public void setIncremental(MojoExecutionData execution, boolean incremental) throws CoreException {
    Configuration config = lifecycleMappingPlugin.getConfiguration();
    Element configNode = (Element) config.getConfigurationNode();
    
    Element executionsDom = getChildElement(configNode, "mojoExecutions");

    if(null == executionsDom) {
      return;
    }
    
    for(Element executionDom : getChildren(executionsDom, "mojoExecution")) {
      String name = getNodeContents(executionDom);
      if(name.equals(execution.getId())) {
        executionDom.setAttribute("runOnIncremental", "" + incremental);
        break;
      }
    }
  }
  
  private Element getChildElement(Element parent, String name) {
    Node n = parent.getFirstChild();
    while(n != null) {
      if(n instanceof Element && n.getNodeName().equals(name)) {
        return (Element)n;
      }
      n = n.getNextSibling();
    }
    return null;
  }
  
  private List<Element> getChildren(Element parent, String name) {
    List<Element> ret = new LinkedList<Element>();
    Node n = parent.getFirstChild();
    while(n != null) {
      if(n instanceof Element && n.getNodeName().equals(name)) {
        ret.add((Element)n);
      }
      n = n.getNextSibling();
    }
    return ret;
  }
  
  private String getNodeContents(Node n) {
    if(n instanceof Text) {
      return ((Text)n).getNodeValue();
    } else if(n instanceof Element) {
      StringBuilder value = new StringBuilder();
      Node child = ((Element)n).getFirstChild();
      while(child != null) {
        value.append(getNodeContents(child));
        child = child.getNextSibling();
      }
      return value.toString();
    }
    return "";
  }
  
  private boolean toBool(String value, boolean def) {
    if(value == null || value.length() == 0) {
      return def;
    }
    return Boolean.parseBoolean(value);
  }

}
