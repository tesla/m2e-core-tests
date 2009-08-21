/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.index;

/**
 * EnabledIndex
 *
 * @author dyocum
 */
public class EnabledIndex {
  private String name;
  private String url;
  public EnabledIndex(String indexName, String repositoryUrl){
    this.name = indexName;
    this.url = repositoryUrl;
    
  }
  public void setName(String name){
    this.name = name;
  }
  public String getName(){
    return name;
  }
  public void setUrl(String url){
    this.url = url;
  }
  public String getUrl(){
    return url;
  }
  
}
