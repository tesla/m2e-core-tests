/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.scm;

import org.eclipse.core.runtime.CoreException;

/**
 * An SCM URL wrapper used to adapt 3rd party resources:
 * 
 * <pre>
 * scm:{scm_provider}:{scm_provider_specific_part}
 * </pre>
 * 
 * @see http://maven.apache.org/scm/scm-url-format.html
 * @see org.eclipse.core.runtime.IAdapterManager
 *
 * @author Eugene Kuleshov
 */
public class ScmUrl {
  private final String scmUrl;
  private final String scmParentUrl;

  public ScmUrl(String scmUrl) {
    this(scmUrl, null);
  }
  
  public ScmUrl(String scmUrl, String scmParentUrl) {
    this.scmUrl = scmUrl;
    this.scmParentUrl = scmParentUrl;
  }

  /**
   * Return SCM url
   */
  public String getUrl() {
    return scmUrl;
  }
  
  /**
   * Return SCM url for the parent folder
   */
  public String getParentUrl() {
    return scmParentUrl;
  }
  
  /**
   * Return provider-specific part of the SCM url
   * 
   * @return
   */
  public String getProviderUrl() {
    try {
      String type = ScmHandlerFactory.getType(scmUrl);
      return scmUrl.substring(type.length() + 5);
      
    } catch(CoreException ex) {
      return null;
    }
  }

}
