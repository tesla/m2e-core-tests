/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.pr.internal.data;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.runtime.CoreException;

/**
 * A data source wraps some object or method, and returns an InputStream
 * representing the contents of that object or method.
 */
public interface IDataSource {

	/**
	 * Get an InputStream representing the wrapped object's contents.
	 * @return the InputStream
	 * @throws IOException
	 * @throws CoreException
	 */
	InputStream getInputStream() throws CoreException;

	/**
	 * Some implementations (e.g. a source that wraps a file in the file system)
	 * can return an appropriate name (e.g the file name).
	 * 
	 * @return a name
	 */
	String getName();

}
