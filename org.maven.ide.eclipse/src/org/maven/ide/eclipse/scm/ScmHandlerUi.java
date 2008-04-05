/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.scm;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.swt.widgets.Shell;

/**
 * An SCM handler UI base class
 *
 * @author Eugene Kuleshov
 */
public abstract class ScmHandlerUi implements IExecutableExtension {
  public static final String ATTR_TYPE = "type";
  public static final String ATTR_CLASS = "class";
  
  private String type;
  
  public String getType() {
    return type;
  }

  public String selectRevision(Shell shell, ScmUrl scmUrl, String scmRevision) {
    return null;
  }

  public ScmUrl selectUrl(Shell shell, ScmUrl scmUrl) {
    return null;
  }
  
  public boolean isValidUrl(String scmUrl) {
    return false;
  }
  
  public boolean isValidRevision(ScmUrl scmUrl, String scmRevision) {
    return false;
  }

  public boolean canSelectUrl() {
    return false;
  }

  public boolean canSelectRevision() {
    return false;
  }

  
  // IExecutableExtension  
  
  public void setInitializationData(IConfigurationElement config, String propertyName, Object data) {
    this.type = config.getAttribute(ATTR_TYPE);
  }
  
}

