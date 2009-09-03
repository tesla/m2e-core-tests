/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.index;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IPath;

import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.proxy.ProxyInfo;


public class RepositoryInfo {

  /** 
   * Repository index is disabled.
   */
  public static final String DETAILS_DISABLED = "off";

  /**
   * Only artifact index information is used. Classname index is disabled. 
   */
  public static final String DETAILS_MIN = "min";

  /**
   * Both artifact and classname indexes are used.
   */
  public static final String DETAILS_FULL = "full";

  private final String id;
  private final String repositoryUrl;
  private final boolean global;
  private final ProxyInfo proxyInfo;
  private final AuthenticationInfo authInfo;
  private String mirrorId;
  private String mirrorOf;
  private Set<IPath> projects = new HashSet<IPath>();

  RepositoryInfo(String id, String repositoryUrl, boolean global, AuthenticationInfo authInfo, ProxyInfo proxyInfo) {
    this.id = id;
    this.repositoryUrl = repositoryUrl;
    this.global = global;
    this.authInfo = authInfo;
    this.proxyInfo = proxyInfo;
  }

  public ProxyInfo getProxyInfo() {
    return proxyInfo;
  }

  public AuthenticationInfo getAuthenticationInfo() {
    return authInfo;
  }

  public String getRepositoryUrl() {
    return repositoryUrl;
  }

  public boolean isGlobal() {
    return global;
  }

  public String getId() {
    return id;
  }

  public String getMirrorId() {
    return mirrorId;
  }

  public String getMirrorOf() {
    return mirrorOf;
  }

  public void setMirrorOf(String mirrorOf) {
    this.mirrorOf = mirrorOf;
  }

  public void setMirrorId(String mirrorId) {
    this.mirrorId = mirrorId;
  }

  public Set<IPath> getProjects() {
    return projects;
  }

  public void addProject(IPath project) {
    if (!global) {
      projects.add(project);
    }
  }

  public void removeProject(IPath project) {
    projects.remove(project);
  }
}
