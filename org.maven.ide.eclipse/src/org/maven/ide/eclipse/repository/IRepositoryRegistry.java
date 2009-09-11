/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.repository;

/**
 * Registry of repositories accessible by m2e. 
 * 
 * Registry automatically includes repositories configured in global/user settings.xml files 
 * and in workspace Maven projects. Note that mirrors are present as separate 
 * repositories in the registry.
 * 
 * @author igor
 */
public interface IRepositoryRegistry {
  
  

}
