/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.project;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.operation.IRunnableWithProgress;


/**
 * Project Scanner
 *
 * @author Eugene Kuleshov
 */
public abstract class AbstractProjectScanner implements IRunnableWithProgress {

  private final List<MavenProjectInfo> projects = new ArrayList<MavenProjectInfo>();
  private final List<Exception> errors = new ArrayList<Exception>();
  
  /**
   * Returns <code>List</code> of {@link MavenProjectInfo}
   */
  public List<MavenProjectInfo> getProjects() {
    return projects;
  }

  /**
   * Returns <code>List</code> of <code>Exception</code>
   */
  public List<Exception> getErrors() {
    return this.errors;
  }

  protected void addProject(MavenProjectInfo mavenProjectInfo) {
    projects.add(mavenProjectInfo);
  }

  protected void addError(Exception exception) {
    errors.add(exception);
  }

  public abstract String getDescription();

}
