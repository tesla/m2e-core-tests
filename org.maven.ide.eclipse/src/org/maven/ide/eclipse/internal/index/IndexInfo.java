/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.index;


public class IndexInfo {

  public static final String INDEX_NO = "off";

  public static final String INDEX_MIN = "min";

  public static final String INDEX_FULL = "full";

  private final String repositoryUrl;

  private final Type type;

  public IndexInfo(Type type, String repositoryUrl) {
    this.type = type;
    this.repositoryUrl = repositoryUrl;
  }

  public String getRepositoryUrl() {
    return repositoryUrl;
  }
  
  public Type getType() {
    return type;
  }

  /**
   * Repository index type
   */
  public static class Type {
    public static final Type REMOTE = new Type();

    public static final Type LOCAL = new Type();

    public static final Type WORKSPACE = new Type();
  }

}
