/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.index;

/**
 * IndexListener
 *
 * @author Eugene Kuleshov
 */
public interface IndexListener {
  
  void indexAdded(IndexInfo info);

  void indexRemoved(IndexInfo info);
  
  void indexChanged(IndexInfo info);

}
