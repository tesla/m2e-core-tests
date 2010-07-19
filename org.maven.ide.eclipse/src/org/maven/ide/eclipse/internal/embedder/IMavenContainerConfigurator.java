/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.embedder;

import org.codehaus.plexus.ContainerConfiguration;

/**
 * Allows configuration of maven core plexus container configuration *before* container is created and started. This is
 * the only way to register components to be injected into maven core components.
 * 
 * @author igor
 */
public interface IMavenContainerConfigurator {
  
  /**
   * Id of maven core class realm
   */
  public static final String MAVEN_CORE_REALM_ID = "plexus.core";

  public void configure(ContainerConfiguration configuration);
}
