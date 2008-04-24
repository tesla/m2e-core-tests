/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.project.configurator;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IProgressMonitor;

import org.apache.maven.embedder.MavenEmbedder;


/**
 * Used to configure maven projects.
 *
 * Work in progress.
 * 
 * @author Igor Fedorenko
 */
public abstract class AbstractProjectConfigurator implements IExecutableExtension {

  public static final String ATTR_ID = "id";
  
  public static final String ATTR_PRIORITY = "priority";

  public static final String ATTR_NAME = "name";
  
  public static final String ATTR_CLASS = "class";
  
  private int priority;
  private String id;
  private String name;

  public abstract void configure(MavenEmbedder embedder, ProjectConfigurationRequest request, IProgressMonitor monitor);

  public int getPriority() {
    return priority;
  }
  
  public String getId() {
    return id;
  }
  
  public String getName() {
    return name;
  }
  
  // IExecutableExtension  
  
  public void setInitializationData(IConfigurationElement config, String propertyName, Object data) {
    this.id = config.getAttribute(ATTR_ID);
    this.name = config.getAttribute(ATTR_NAME);
    String priorityString = config.getAttribute(ATTR_PRIORITY);
    try {
      priority = Integer.parseInt(priorityString);
    } catch (Exception ex) {
      priority = Integer.MAX_VALUE;
    }
  }
  
}
