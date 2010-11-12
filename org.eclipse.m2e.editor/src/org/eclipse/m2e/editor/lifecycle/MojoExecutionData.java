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

package org.eclipse.m2e.editor.lifecycle;

public class MojoExecutionData {
  private final String displayName;
  private final String id;
  private final boolean enabled;
  private final boolean runOnIncrementalBuild;
  
  public MojoExecutionData(String displayName, String id, boolean enabled, boolean runOnIncrementalBuild) {
    super();
    this.displayName = displayName;
    this.id = id;
    this.enabled = enabled;
    this.runOnIncrementalBuild = runOnIncrementalBuild;
  }

  /**
   * @return the displayName
   */
  public String getDisplayName() {
    return displayName;
  }

  /**
   * @return the id
   */
  public String getId() {
    return id;
  }

  /**
   * @return the enabled
   */
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * @return the runOnIncrementalBuild
   */
  public boolean isRunOnIncrementalBuild() {
    return runOnIncrementalBuild;
  }
  
  
}
