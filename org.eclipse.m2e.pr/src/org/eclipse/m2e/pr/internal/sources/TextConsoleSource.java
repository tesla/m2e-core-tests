/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.pr.internal.sources;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.m2e.core.core.MavenConsole;
import org.eclipse.m2e.pr.IDataSource;
import org.eclipse.ui.console.TextConsole;

/**
 * Returns the contents of a TextConsole window.
 */
public class TextConsoleSource implements IDataSource {

	private final MavenConsole console;
  private final String name;

	public TextConsoleSource(MavenConsole console, String name) {
		this.console = console;
    this.name = name;
	}

	public InputStream getInputStream() throws CoreException {
    if(console instanceof TextConsole) {
      IDocument document = ((TextConsole) console).getDocument();
      String consoleText = document.get();
      return new ByteArrayInputStream(consoleText.getBytes());
    }
	  return null;
	}

	public String getName() {
		return name;
	}

}
