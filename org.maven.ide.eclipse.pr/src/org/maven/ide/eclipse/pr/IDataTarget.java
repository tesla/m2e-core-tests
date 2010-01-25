/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.pr;

import org.eclipse.core.runtime.CoreException;


/**
 * A data target knows how to consume a data source, recording the result under
 * the specified name.
 */
public interface IDataTarget {

	/**
	 * Consume the data, storing the data under the specified name.
	 * 
	 * @param folderName a folder name to save data
	 * @param source the source of the data
	 */
	void consume(String folderName, IDataSource source) throws CoreException;

}
