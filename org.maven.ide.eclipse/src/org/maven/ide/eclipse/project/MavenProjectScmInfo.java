/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.project;

import org.apache.maven.model.Model;


/**
 * @author Eugene Kuleshov
 */
public class MavenProjectScmInfo extends MavenProjectInfo {

  private final String folderUrl;
  private final String repositoryUrl;
  private final String revision;

  public MavenProjectScmInfo(String label, Model model, MavenProjectInfo parent, //
      String revision, String folderUrl, String repositoryUrl) {
    super(label, null, model, parent);
    this.revision = revision;
    this.folderUrl = folderUrl;
    this.repositoryUrl = repositoryUrl;
  }
  
  public String getRevision() {
    return this.revision;
  }
  
  public String getFolderUrl() {
    return folderUrl;
  }
  
  public String getRepositoryUrl() {
    return repositoryUrl;
  }

}
