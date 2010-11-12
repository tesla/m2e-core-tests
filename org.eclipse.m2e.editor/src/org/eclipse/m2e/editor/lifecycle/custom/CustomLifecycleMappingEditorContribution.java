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

package org.eclipse.m2e.editor.lifecycle.custom;

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
import org.eclipse.m2e.core.internal.ExtensionReader;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.AbstractLifecycleMapping;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.ILifecycleMapping;
import org.eclipse.m2e.editor.lifecycle.ILifecycleMappingEditorContribution;
import org.eclipse.m2e.editor.lifecycle.LifecycleEditorUtils;
import org.eclipse.m2e.editor.lifecycle.MojoExecutionData;
import org.eclipse.m2e.editor.pom.MavenPomEditor;
import org.eclipse.m2e.model.edit.pom.Configuration;
import org.eclipse.m2e.model.edit.pom.Model;
import org.eclipse.m2e.model.edit.pom.Plugin;
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
    lifecycleMappingPlugin.getConfiguration().setStringValue("mappingId", "customizable"); //$NON-NLS-1$ //$NON-NLS-2$
    Element configuratorsElement = configNode.getOwnerDocument().createElement("configurators"); //$NON-NLS-1$
    configNode.appendChild(configuratorsElement);
    
    if(templateId != null) {
      ILifecycleMapping mapping = mappings.get(templateId);
      for(AbstractProjectConfigurator configer : mapping.getProjectConfigurators(projectFacade, new NullProgressMonitor())) {
        if(!configer.isGeneric()) {
          Element configuratorElement = configNode.getOwnerDocument().createElement("configurator"); //$NON-NLS-1$
          configuratorElement.setAttribute("id", configer.getId()); //$NON-NLS-1$
          configuratorsElement.appendChild(configuratorElement);
        }
      }
    }
    
    Element mojoExecutionsElement = configNode.getOwnerDocument().createElement("mojoExecutions"); //$NON-NLS-1$
    configNode.appendChild(mojoExecutionsElement);
    if(templateId != null) {
      ILifecycleMapping mapping = mappings.get(templateId);
      List<String> allExecutions = mapping.getPotentialMojoExecutionsForBuildKind(projectFacade, IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
      List<String> incrExecutions = mapping.getPotentialMojoExecutionsForBuildKind(projectFacade, IncrementalProjectBuilder.INCREMENTAL_BUILD, new NullProgressMonitor());
      for(String execution : allExecutions) {
        Element mojoExecutionElement = configNode.getOwnerDocument().createElement("mojoExecution"); //$NON-NLS-1$
        if(incrExecutions.contains(execution)) {
          mojoExecutionElement.setAttribute("runOnIncremental", "true"); //$NON-NLS-1$ //$NON-NLS-2$
        } else {
          mojoExecutionElement.setAttribute("runOnIncremental", "false"); //$NON-NLS-1$ //$NON-NLS-2$
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

    Element configuratorsDom = getChildElement((Element)config.getConfigurationNode(), "configurators"); //$NON-NLS-1$

    List<AbstractProjectConfigurator> configurators = new ArrayList<AbstractProjectConfigurator>();
    
    if (configuratorsDom != null) {
      for(Element configuratorDom : getChildren(configuratorsDom, "configurator")) { //$NON-NLS-1$
        String configuratorId = configuratorDom.getAttribute("id"); //$NON-NLS-1$
        AbstractProjectConfigurator configurator = configuratorsMap.get(configuratorId);
        if(configurator == null) {
          throw new IllegalArgumentException("Unknown configurator id=" + configuratorId); //$NON-NLS-1$
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

    Element configuratorsDom = getChildElement((Element)config.getConfigurationNode(), "configurators"); //$NON-NLS-1$
    if(null != configuratorsDom) {
      Set<String> usedIds = new LinkedHashSet<String>();
      for(Element configuratorDom : getChildren(configuratorsDom, "configurator")) { //$NON-NLS-1$
        usedIds.add(configuratorDom.getAttribute("id")); //$NON-NLS-1$
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
          configuratorsDom = config.getConfigurationNode().getOwnerDocument().createElement("configurators"); //$NON-NLS-1$
          config.getConfigurationNode().appendChild(configuratorsDom);
        }
        
        Element configuratorDom = configuratorsDom.getOwnerDocument().createElement("configurator"); //$NON-NLS-1$
        configuratorDom.setAttribute("id", selection); //$NON-NLS-1$
        configuratorsDom.appendChild(configuratorDom);
      }
    }

  }
  
  public void editProjectConfigurator(AbstractProjectConfigurator configurator) throws CoreException { }
  
  public void removeProjectConfigurator(AbstractProjectConfigurator configurator) throws CoreException {
    Configuration config = lifecycleMappingPlugin.getConfiguration();

    Element configuratorsDom = getChildElement((Element)config.getConfigurationNode(), "configurators"); //$NON-NLS-1$

    if (configuratorsDom != null) {
      for(Element configuratorDom : getChildren(configuratorsDom, "configurator")) { //$NON-NLS-1$
        String configuratorId = configuratorDom.getAttribute("id"); //$NON-NLS-1$
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
    
    Element executionsDom = getChildElement(configNode, "mojoExecutions"); //$NON-NLS-1$

    List<MojoExecutionData> mojos = new LinkedList<MojoExecutionData>();
    
    if (executionsDom != null) {
      for(Element execution : getChildren(executionsDom, "mojoExecution")) { //$NON-NLS-1$
        String strRunOnIncremental = execution.getAttribute("runOnIncremental"); //$NON-NLS-1$
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
    
    Element executionsDom = getChildElement(configNode, "mojoExecutions"); //$NON-NLS-1$

    if(null == executionsDom) {
      executionsDom = configNode.getOwnerDocument().createElement("mojoExecutions"); //$NON-NLS-1$
      configNode.appendChild(executionsDom);
    }
    
    Element executionDom = configNode.getOwnerDocument().createElement("mojoExecution"); //$NON-NLS-1$
    executionDom.setAttribute("runOnIncremental", "" + execution.isRunOnIncrementalBuild()); //$NON-NLS-1$ //$NON-NLS-2$
    executionDom.appendChild(configNode.getOwnerDocument().createTextNode(execution.getId()));
    executionsDom.appendChild(executionDom);
  }
  
  public void disableMojoExecution(MojoExecutionData execution) throws CoreException {
    Configuration config = lifecycleMappingPlugin.getConfiguration();
    Element configNode = (Element) config.getConfigurationNode();
    
    Element executionsDom = getChildElement(configNode, "mojoExecutions"); //$NON-NLS-1$

    if(null == executionsDom) {
      return;
    }
    
    for(Element executionDom : getChildren(executionsDom, "mojoExecution")) { //$NON-NLS-1$
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
    
    Element executionsDom = getChildElement(configNode, "mojoExecutions"); //$NON-NLS-1$

    if(null == executionsDom) {
      return;
    }
    
    for(Element executionDom : getChildren(executionsDom, "mojoExecution")) { //$NON-NLS-1$
      String name = getNodeContents(executionDom);
      if(name.equals(execution.getId())) {
        executionDom.setAttribute("runOnIncremental", "" + incremental); //$NON-NLS-1$ //$NON-NLS-2$
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
    return ""; //$NON-NLS-1$
  }
  
  private boolean toBool(String value, boolean def) {
    if(value == null || value.length() == 0) {
      return def;
    }
    return Boolean.parseBoolean(value);
  }

}
