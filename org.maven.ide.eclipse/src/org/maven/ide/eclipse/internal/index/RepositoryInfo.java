/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.index;

import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.proxy.ProxyInfo;


class RepositoryInfo {

  private final ProxyInfo proxyInfo;
  private final AuthenticationInfo authInfo;
  private final String repositoryUrl;

  public RepositoryInfo(String repositoryUrl, AuthenticationInfo authInfo, ProxyInfo proxyInfo) {
    this.repositoryUrl = repositoryUrl;
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
    return this.repositoryUrl;
  }
}
