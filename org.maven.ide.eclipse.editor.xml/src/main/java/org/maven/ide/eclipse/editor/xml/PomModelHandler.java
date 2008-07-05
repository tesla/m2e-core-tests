/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.editor.xml;

import org.eclipse.wst.xml.core.internal.modelhandler.ModelHandlerForXML;

@SuppressWarnings("restriction")
public class PomModelHandler extends ModelHandlerForXML {

  private static final String ASSOCIATED_CONTENT_TYPE_ID = "org.maven.ide.eclipse.pomFile";
  
  public PomModelHandler() {
    super();
    setAssociatedContentTypeId(ASSOCIATED_CONTENT_TYPE_ID);
  }
  
}
